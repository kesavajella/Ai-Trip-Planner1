package com.intellitrip.service;

import com.intellitrip.dto.AccommodationResponse;
import com.intellitrip.dto.ActivityResponse;
import com.intellitrip.dto.CostBreakdownResponse;
import com.intellitrip.dto.DayResponse;
import com.intellitrip.dto.ItineraryResponse;
import com.intellitrip.dto.TransportResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ItineraryService {
    @Value("${google.ai.api-key:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ItineraryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public ItineraryResponse generateItinerary(String destination, String budget, Integer days, List<String> interests) {
        String prompt = buildPrompt(destination, budget, days, interests);

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return generateFallbackItinerary(destination, budget, days, interests);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(part));
            requestBody.put("contents", Collections.singletonList(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                text = text.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
                JsonNode json = objectMapper.readTree(text);
                return parseJsonResponse(json, destination, budget, days);
            }
        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
        }

        return generateFallbackItinerary(destination, budget, days, interests);
    }

    private String buildPrompt(String destination, String budget, Integer days, List<String> interests) {
        return String.format("""
            Generate a highly detailed and realistic %d-day trip itinerary for %s.

            **Trip Details:**
            - Budget level: %s
            - Number of travelers: 1
            - Travel companion type: solo
            - Interests: %s

            **Important Requirements:**
            1. Each day must have UNIQUE activities - no repeating activities
            2. Include SPECIFIC, REAL locations and attractions
            3. Times must be realistic and varied
            4. Activities must match the interests provided
            5. Suggest SPECIFIC restaurant names, neighborhoods
            6. Each activity should have a realistic estimated cost
            7. Include practical tips like opening hours, booking requirements
            8. Research real attractions in %s - use actual place names

            **Response Format (VALID JSON ONLY - no markdown):**
            {
              "title": "X-Day [Destination] Trip",
              "overview": "2-3 sentence description",
              "totalBudget": 2000,
              "dailyBudget": 200,
              "days": [
                {
                  "day": 1,
                  "title": "Day 1 Theme",
                  "theme": "Specific theme",
                  "activities": [
                    {
                      "time": "09:00 AM",
                      "activity": "Specific real attraction",
                      "description": "Detailed description",
                      "estimatedCost": 45
                    }
                  ]
                }
              ],
              "accommodation": [
                {
                  "name": "Specific hotel name",
                  "type": "Hotel",
                  "price": 85,
                  "rating": "4.5/5",
                  "description": "Why it is good"
                }
              ],
              "transportation": [
                {
                  "type": "Transport type",
                  "cost": 15,
                  "description": "Description"
                }
              ],
              "costBreakdown": {
                "accommodation": 800,
                "food": 500,
                "activities": 400,
                "transportation": 200,
                "other": 100
              },
              "tips": [
                "Tip 1 for %s",
                "Tip 2",
                "Tip 3",
                "Tip 4",
                "Tip 5"
              ]
            }
            """, days, destination, budget, String.join(", ", interests), destination, destination);
    }

    private ItineraryResponse parseJsonResponse(JsonNode json, String destination, String budget, Integer days) throws Exception {
        String title = json.path("title").asText(destination + " Trip");
        String overview = json.path("overview").asText("");
        BigDecimal totalBudget = new BigDecimal(json.path("totalBudget").asInt(2000));
        BigDecimal dailyBudget = new BigDecimal(json.path("dailyBudget").asInt(200));

        List<DayResponse> dayResponses = new ArrayList<>();
        JsonNode daysNode = json.path("days");
        for (JsonNode dayNode : daysNode) {
            List<ActivityResponse> activities = new ArrayList<>();
            JsonNode actNodes = dayNode.path("activities");
            for (JsonNode act : actNodes) {
                activities.add(ActivityResponse.builder()
                        .time(act.path("time").asText("09:00 AM"))
                        .activity(act.path("activity").asText("Activity"))
                        .description(act.path("description").asText(""))
                        .estimatedCost(new BigDecimal(act.path("estimatedCost").asInt(30)))
                        .build());
            }
            dayResponses.add(DayResponse.builder()
                    .day(dayNode.path("day").asInt(1))
                    .title(dayNode.path("title").asText("Day " + dayNode.path("day").asInt(1)))
                    .theme(dayNode.path("theme").asText(""))
                    .activities(activities)
                    .build());
        }

        List<AccommodationResponse> accommodations = new ArrayList<>();
        JsonNode accNodes = json.path("accommodation");
        for (JsonNode acc : accNodes) {
            accommodations.add(AccommodationResponse.builder()
                    .name(acc.path("name").asText())
                    .type(acc.path("type").asText())
                    .price(new BigDecimal(acc.path("price").asInt(80)))
                    .rating(acc.path("rating").asText("4.0/5"))
                    .description(acc.path("description").asText())
                    .build());
        }

        List<TransportResponse> transports = new ArrayList<>();
        JsonNode transNodes = json.path("transportation");
        for (JsonNode trans : transNodes) {
            transports.add(TransportResponse.builder()
                    .type(trans.path("type").asText())
                    .cost(new BigDecimal(trans.path("cost").asInt(15)))
                    .description(trans.path("description").asText())
                    .build());
        }

        JsonNode cb = json.path("costBreakdown");
        CostBreakdownResponse costBreakdown = CostBreakdownResponse.builder()
                .accommodation(new BigDecimal(cb.path("accommodation").asInt(800)))
                .food(new BigDecimal(cb.path("food").asInt(500)))
                .activities(new BigDecimal(cb.path("activities").asInt(400)))
                .transportation(new BigDecimal(cb.path("transportation").asInt(200)))
                .other(new BigDecimal(cb.path("other").asInt(100)))
                .build();

        List<String> tips = new ArrayList<>();
        JsonNode tipsNode = json.path("tips");
        for (JsonNode tip : tipsNode) {
            tips.add(tip.asText());
        }

        return ItineraryResponse.builder()
                .title(title)
                .overview(overview)
                .totalBudget(totalBudget)
                .dailyBudget(dailyBudget)
                .days(dayResponses)
                .accommodation(accommodations)
                .transportation(transports)
                .costBreakdown(costBreakdown)
                .tips(tips)
                .build();
    }

    public ItineraryResponse generateFallbackItinerary(String destination, String budget, Integer days, List<String> interests) {
        String interestsStr = interests != null && !interests.isEmpty() ? String.join(", ", interests) : "exploration";
        BigDecimal totalBudget = calculateBudget(budget, days, 1);
        BigDecimal dailyBudget = totalBudget.divide(BigDecimal.valueOf(days), BigDecimal.ROUND_HALF_UP);

        List<DayResponse> dayResponses = new ArrayList<>();
        String[] timePatterns = {"09:00 AM", "11:30 AM", "03:00 PM", "07:00 PM"};
        String[] themes = {"Morning exploration", "Cultural immersion", "Afternoon adventure", "Evening dining"};
        String[] activityTemplates = {
                destination + " historic district exploration",
                "Local market visit in " + destination,
                "Museum or cultural site in " + destination,
                "Nature or park activity near " + destination,
                "Neighborhood walking tour of " + destination,
                "Local cuisine experience in " + destination
        };

        for (int d = 1; d <= Math.max(1, days); d++) {
            List<ActivityResponse> activities = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int idx = (d - 1) * 4 + i;
                activities.add(ActivityResponse.builder()
                        .time(timePatterns[i % timePatterns.length])
                        .activity(activityTemplates[idx % activityTemplates.length])
                        .description(String.format("Experience %s in %s. %s.", activityTemplates[idx % activityTemplates.length].toLowerCase(), destination, themes[i % themes.length]))
                        .estimatedCost(new BigDecimal(25 + (idx * 10)))
                        .build());
            }
            dayResponses.add(DayResponse.builder()
                    .day(d)
                    .title("Day " + d + ": " + (interests.isEmpty() ? "Exploration" : interests.get((d - 1) % interests.size())))
                    .theme(interestsStr)
                    .activities(activities)
                    .build());
        }

        return ItineraryResponse.builder()
                .title(days + "-Day " + destination + " Trip")
                .overview("Discover the best of " + destination + " in " + days + " days with a focus on " + interestsStr + ". This itinerary offers a mix of iconic experiences, local culture, and authentic dining opportunities tailored to your " + budget + " budget.")
                .totalBudget(totalBudget)
                .dailyBudget(dailyBudget)
                .days(dayResponses)
                .accommodation(List.of(
                        AccommodationResponse.builder()
                                .name("Central Location Hotel")
                                .type("Hotel")
                                .price(new BigDecimal(80))
                                .rating("4.0/5")
                                .description("Centrally located for easy access to attractions in " + destination)
                                .build()
                ))
                .transportation(List.of(
                        TransportResponse.builder()
                                .type("Public Transportation (daily pass)")
                                .cost(new BigDecimal(8))
                                .description("Public transportation in " + destination)
                                .build()
                ))
                .costBreakdown(CostBreakdownResponse.builder()
                        .accommodation(totalBudget.multiply(BigDecimal.valueOf(0.4)))
                        .food(totalBudget.multiply(BigDecimal.valueOf(0.25)))
                        .activities(totalBudget.multiply(BigDecimal.valueOf(0.2)))
                        .transportation(totalBudget.multiply(BigDecimal.valueOf(0.1)))
                        .other(totalBudget.multiply(BigDecimal.valueOf(0.05)))
                        .build())
                .tips(List.of(
                        "Book accommodations in advance to save 20-30%",
                        "Visit main attractions early in the morning to avoid crowds",
                        "Eat where locals eat for authentic experiences",
                        "Use public transportation to save money",
                        "Learn basic local phrases before your trip"
                ))
                .build();
    }

    private BigDecimal calculateBudget(String budget, Integer days, Integer travelers) {
        BigDecimal base = switch (budget.toLowerCase()) {
            case "luxury" -> BigDecimal.valueOf(300);
            case "budget friendly", "low" -> BigDecimal.valueOf(80);
            default -> BigDecimal.valueOf(150);
        };
        return base.multiply(BigDecimal.valueOf(days)).multiply(BigDecimal.valueOf(travelers));
    }
}
