package com.intellitrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.config.GeminiConfig;
import com.intellitrip.dto.TripPlanRequest;
import com.intellitrip.dto.TripPlanResponse;
import com.intellitrip.exception.GeminiQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    /**
     * Thrown when the Gemini API returns a 429 (RESOURCE_EXHAUSTED) or 420 rate-limit error,
     * indicating the request should be retried with exponential backoff.
     */
    private static class RateLimitException extends RuntimeException {
        RateLimitException(String message) {
            super(message);
        }
    }

    private static final String RESPONSE_SCHEMA_JSON = """
        {
          "type": "object",
          "properties": {
            "tripOverview": {
              "type": "object",
              "properties": {
                "destination": {"type": "string"},
                "source": {"type": "string"},
                "startDate": {"type": "string"},
                "totalDays": {"type": "integer"},
                "seasonalHighlights": {"type": "string"}
              },
              "required": ["destination", "source", "startDate", "totalDays", "seasonalHighlights"]
            },
            "hotels": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": {"type": "string"},
                  "area": {"type": "string"},
                  "pricePerNight": {"type": "number"},
                  "bestFor": {"type": "string"}
                }
              }
            },
            "transport": {
              "type": "object",
              "properties": {
                "roundTrip": {
                  "type": "object",
                  "properties": {
                    "mode": {"type": "string"},
                    "route": {"type": "string"},
                    "duration": {"type": "string"},
                    "estimatedCost": {"type": "number"},
                    "recommendedBookingApp": {"type": "string"}
                  },
                  "required": ["mode", "route", "duration", "estimatedCost", "recommendedBookingApp"]
                },
                "localTransportApps": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string"},
                      "purpose": {"type": "string"}
                    }
                  }
                }
              },
              "required": ["roundTrip", "localTransportApps"]
            },
            "budget": {
              "type": "object",
              "properties": {
                "currency": {"type": "string"},
                "estimatedAccommodationTotal": {"type": "number"},
                "estimatedFoodTotal": {"type": "number"},
                "estimatedTransportTotal": {"type": "number"},
                "estimatedActivitiesTotal": {"type": "number"},
                "estimatedOtherTotal": {"type": "number"},
                "grandTotal": {"type": "number"}
              },
              "required": ["currency", "estimatedAccommodationTotal", "estimatedFoodTotal", "estimatedTransportTotal", "estimatedActivitiesTotal", "estimatedOtherTotal", "grandTotal"]
            },
            "itinerary": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "day": {"type": "integer"},
                  "date": {"type": "string"},
                  "theme": {"type": "string"},
                  "activities": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {
                        "timeSlot": {"type": "string"},
                        "place": {"type": "string"},
                        "transitToPlace": {"type": "string"},
                        "description": {"type": "string"}
                      },
                      "required": ["timeSlot", "place", "transitToPlace", "description"]
                    }
                  },
                  "localFoodSpot": {"type": "string"}
                },
                "required": ["day", "date", "theme", "activities", "localFoodSpot"]
              }
            },
            "estimatedDailyCosts": {
              "type": "object",
              "properties": {
                "accommodationPerNight": {"type": "number"},
                "foodPerDay": {"type": "number"},
                "interestsPerDay": {"type": "number"}
              }
            }
          },
          "required": ["tripOverview", "hotels", "transport", "budget", "itinerary", "estimatedDailyCosts"]
        }
        """;

    private final RestClient restClient;
    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;
    private Map<String, Object> cachedResponseSchema;

    public GeminiAiService(RestClient geminiRestClient, GeminiConfig geminiConfig, ObjectMapper objectMapper) {
        this.restClient = geminiRestClient;
        this.geminiConfig = geminiConfig;
        this.objectMapper = objectMapper;
    }

    private Map<String, Object> buildResponseSchema() {
        if (cachedResponseSchema == null) {
            try {
                cachedResponseSchema = objectMapper.readValue(RESPONSE_SCHEMA_JSON, Map.class);
            } catch (Exception e) {
                log.error("Failed to parse response schema JSON", e);
                cachedResponseSchema = new HashMap<>();
            }
        }
        return cachedResponseSchema;
    }

    public TripPlanResponse generateTripPlan(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "contents", new Object[] {
                Map.of("parts", new Object[] {
                    Map.of("text", prompt)
                })
            },
            "generationConfig", Map.of(
                "temperature", 0.1,
                "responseMimeType", "application/json",
                "responseSchema", buildResponseSchema()
            )
        );

        int maxRetries = geminiConfig.getMaxRetries();
        long delayMs = geminiConfig.getInitialDelayMs();

        for (int attempt = 1; attempt <= (maxRetries + 1); attempt++) {
            try {
                String response = callGeminiWithFallback(requestBody);
                return parseResponse(response);
            } catch (RateLimitException e) {
                if (attempt <= maxRetries) {
                    log.warn("⚠️ Gemini rate limit (429/420) hit. Retrying in {}s... (Attempt {}/{})",
                            delayMs / 1000.0, attempt, maxRetries + 1);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    delayMs *= 2;
                } else {
                    log.error("Gemini API rate limit exceeded after {} retries.", maxRetries);
                    throw new GeminiQuotaExceededException("Daily limit reached, please try again shortly", e);
                }
            }
        }
        throw new GeminiQuotaExceededException("Daily limit reached, please try again shortly");
    }

    private String callGeminiWithFallback(Map<String, Object> requestBody) {
        String primaryKey = geminiConfig.getApiKey();
        String fallbackKey = geminiConfig.getFallbackApiKey();

        boolean primaryAvailable = primaryKey != null && !primaryKey.isBlank();
        boolean fallbackAvailable = fallbackKey != null && !fallbackKey.isBlank();

        if (!primaryAvailable) {
            if (fallbackAvailable) {
                log.info("Primary key not set, using fallback key");
                return callGemini(requestBody, fallbackKey);
            }
            throw new RuntimeException("GEMINI_API_KEY is not set");
        }

        try {
            log.debug("Trying primary API key");
            return callGemini(requestBody, primaryKey);
        } catch (RateLimitException e) {
            log.warn("Primary API key rate-limited, trying fallback key...");
            if (fallbackAvailable) {
                return callGemini(requestBody, fallbackKey);
            }
            throw e;
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            boolean rateLimited = msg != null && (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("403") || msg.contains("PERMISSION_DENIED"));
            boolean networkError = msg != null && (msg.contains("timeout") || msg.contains("Timeout") || msg.contains("Connection") || msg.contains("UnknownHost") || msg.contains("SSL"));
            if ((rateLimited || networkError) && fallbackAvailable) {
                log.warn("Primary API key failed ({}), trying fallback key...", msg != null ? msg : "error");
                try {
                    return callGemini(requestBody, fallbackKey);
                } catch (Exception fallbackEx) {
                    log.error("Fallback API key also failed: {}", fallbackEx.getMessage());
                    throw new RuntimeException("Both primary and fallback Gemini API keys failed: " + fallbackEx.getMessage(), fallbackEx);
                }
            }
            throw e;
        } catch (Exception e) {
            if (fallbackAvailable) {
                log.warn("Primary API key threw unexpected exception, trying fallback key: {}", e.getMessage());
                try {
                    return callGemini(requestBody, fallbackKey);
                } catch (Exception fallbackEx) {
                    log.error("Fallback API key also failed: {}", fallbackEx.getMessage());
                    throw new RuntimeException("Both primary and fallback Gemini API keys failed: " + fallbackEx.getMessage(), fallbackEx);
                }
            }
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private String callGemini(Map<String, Object> requestBody, String apiKey) {
        String model = geminiConfig.getModel();
        log.debug("Calling Gemini API with model: {} (key ending in ...{})", model, apiKey.substring(Math.max(0, apiKey.length() - 4)));

        try {
            return restClient.post()
                    .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (req, resp) -> {
                                String respBody = "";
                                try {
                                    respBody = resp.getBody() != null ? new String(resp.getBody().readAllBytes()) : "";
                                } catch (Exception ignored) {}
                                log.error("Gemini API returned HTTP {}: {}", resp.getStatusCode(), respBody);
                                if (resp.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS || resp.getStatusCode().value() == 420) {
                                     throw new RateLimitException("Gemini API rate limit (429/420): " + respBody);
                                }
                                if (resp.getStatusCode() == HttpStatus.FORBIDDEN || resp.getStatusCode().value() == 403) {
                                     throw new RuntimeException("Gemini API forbidden (403): " + respBody);
                                }
                                throw new RuntimeException("Gemini API error: HTTP " + resp.getStatusCode() + " - " + respBody);
                            })
                    .body(String.class);
        } catch (RateLimitException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private TripPlanResponse parseResponse(String response) {
        if (response == null || response.isBlank()) {
            log.error("Gemini API returned empty response body");
            throw new RuntimeException("Gemini API returned an empty response");
        }

        log.debug("Received Gemini API response ({} chars): {}", response.length(), response);

        // Parse the response to extract text
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            var candidates = (java.util.List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                var content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    String text = (String) parts.get(0).get("text");
                    log.debug("Raw Gemini response text: {}", text);
                    // Clean potential markdown
                    text = text.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
                    text = extractJsonObject(text);
                    TripPlanResponse parsed = objectMapper.readValue(text, TripPlanResponse.class);
                    log.debug("Parsed TripPlanResponse: destination={}, days={}",
                            parsed.getDestination(),
                            parsed.getItinerary() != null ? parsed.getItinerary().size() : 0);
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", response, e);
        }

        throw new RuntimeException("Failed to parse AI response");
    }

    private String extractJsonObject(String text) {
        if (text == null || text.isBlank()) return text;
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return text.substring(first, last + 1);
        }
        return text;
    }
}
