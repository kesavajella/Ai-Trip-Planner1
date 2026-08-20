package com.intellitrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.config.GeminiConfig;
import com.intellitrip.dto.TripPlanRequest;
import com.intellitrip.dto.TripPlanResponse;
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
            "destination": {"type": "string"},
            "duration": {"type": "string"},
            "travelers": {"type": "string"},
            "budgetTier": {"type": "string"},
            "estimatedTotalCost": {"type": "string"},
            "tripSummary": {"type": "string"},
            "itinerary": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "day": {"type": "integer"},
                  "dayTitle": {"type": "string"},
                  "schedule": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {
                        "timeOfDay": {"type": "string"},
                        "timeSlot": {"type": "string"},
                        "activityTitle": {"type": "string"},
                        "description": {"type": "string"},
                        "locationName": {"type": "string"},
                        "estimatedCost": {"type": "string"},
                        "transitInfo": {"type": "string"}
                      },
                      "required": ["timeOfDay", "timeSlot", "activityTitle", "description", "locationName", "estimatedCost", "transitInfo"]
                    }
                  }
                },
                "required": ["day", "dayTitle", "schedule"]
              }
            }
          },
          "required": ["destination", "duration", "travelers", "budgetTier", "estimatedTotalCost", "tripSummary", "itinerary"]
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
                "temperature", 0.2,
                "responseMimeType", "application/json",
                "responseSchema", buildResponseSchema()
            )
        );

        int maxRetries = geminiConfig.getMaxRetries();
        long delayMs = geminiConfig.getInitialDelayMs();

        for (int attempt = 1; attempt <= (maxRetries + 1); attempt++) {
            try {
                String response = callGemini(requestBody);
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
                    delayMs *= 2; // Exponential backoff: double wait time on next attempt
                } else {
                    log.error("Gemini API rate limit exceeded after {} retries.", maxRetries);
                    throw new RuntimeException("Gemini API rate limit exceeded after multiple retries. Please try again shortly.", e);
                }
            }
        }
        throw new RuntimeException("Gemini API rate limit exceeded after multiple retries.");
    }

    private String callGemini(Map<String, Object> requestBody) {
        String apiKey = geminiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not set");
        }

        String model = geminiConfig.getModel();
        log.debug("Calling Gemini API with model: {}", model);

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
                                     // 429 (RESOURCE_EXHAUSTED) or 420 - retryable with backoff
                                     throw new RateLimitException("Gemini API rate limit (429/420): " + respBody);
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
