package com.intellitrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.dto.ItineraryData;
import com.intellitrip.dto.TripPlanRequest;
import com.intellitrip.dto.TripPlanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ItineraryGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGeneratorService.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert AI travel planner and itinerary engine.
            Your primary task is to generate a comprehensive, realistic, day-by-day travel itinerary based on the user's input.

            FAILURE CONDITION: If you output only a budget summary, a high-level overview, or any text without the full day-by-day itinerary array, the response will be rejected and discarded. You MUST return the complete JSON object with ALL fields populated.

            STRICT JSON OUTPUT SCHEMA REQUIRED (match the field names exactly):
            {
              "destination": "String",
              "duration": "String",
              "travelers": "String",
              "budgetTier": "String",
              "estimatedTotalCost": "String (e.g. '€2,400 EUR')",
              "tripSummary": "String (a 2-3 sentence summary of the trip plan)",
              "itinerary": [
                {
                  "day": Number (e.g. 1),
                  "dayTitle": "String (e.g. 'Arrival, Local Markets & Sunset Cruise')",
                  "schedule": [
                    {
                      "timeOfDay": "String ('Morning' | 'Afternoon' | 'Evening' | 'Night')",
                      "timeSlot": "String (e.g. '09:00 AM - 11:30 AM')",
                      "activityTitle": "String (e.g. 'Visit the Eiffel Tower')",
                      "description": "String (detailed explanation of what to do)",
                      "locationName": "String (real, named place/attraction, e.g. 'Eiffel Tower')",
                      "estimatedCost": "String (e.g. '€25')",
                      "transitInfo": "String (travel time & recommended mode of transport)"
                    }
                  ]
                }
              ]
            }

            RULES:
            - **CRITICAL RULE 1: RESEARCH REAL PLACES**: Before generating the itinerary, you MUST research and identify 15-25 famous, iconic, must-see places for the exact destination city in the user's request. These must be real, named landmarks, museums, temples, neighborhoods, markets, parks, viewpoints, beaches, monuments, and notable restaurants that are genuinely located in that specific destination. For example, for Paris use the Eiffel Tower, Louvre Museum, Notre-Dame, Montmartre, Arc de Triomphe, Musee d'Orsay, etc. For Tokyo use Senso-ji Temple, Shibuya Crossing, Tokyo Skytree, Meiji Shrine, Tsukiji Outer Market, etc. This list MUST be unique for each destination—never reuse the same list for different destinations.
            - **CRITICAL RULE 2: USE ONLY REAL, DESTINATION-SPECIFIC PLACES**: Every single `locationName` in the entire itinerary MUST be a real, named place from the destination city you researched. DO NOT use generic, made-up, or template place names like "Local Market", "Historic District", "City Park", "Art Gallery", "Waterfront", "Viewpoint", "Cultural Center", "Botanical Garden", "Local Cafe", "Quick Bite", or "Sit-down Restaurant". Using generic names will cause the response to be rejected.
            - **CRITICAL RULE 3: UNIQUE PLACES PER DAY**: Every day of the itinerary MUST feature completely different and unique locations and attractions. It is strictly forbidden to repeat ANY `locationName` across different days. For a 5-day trip, you must provide 5 completely distinct sets of places.
            - **CRITICAL RULE 4: ALLOCATE PLACES ACROSS DAYS**: Distribute the famous places you researched across all days of the trip. Each day must get its own unique group of places. No place should appear on more than one day.
            - The `itinerary` array MUST contain exactly the number of days requested by the user. If the user asks for 5 days, you MUST return exactly 5 day objects.
            - Every day MUST have at least 3 schedule entries (e.g., Morning, Afternoon, Evening) with real, non-generic `locationName` values.
            - Respond ONLY with a valid, raw JSON string. Do not include markdown formatting like ```json. Do not include any conversational text before or after the JSON.
            - If you cannot generate a complete itinerary for the requested duration, DO NOT return a partial response. You must return the full schema with all days filled out.
            """;

    private final GeminiAiService geminiAiService;
    private final ObjectMapper objectMapper;

    public ItineraryGeneratorService(GeminiAiService geminiAiService, ObjectMapper objectMapper) {
        this.geminiAiService = geminiAiService;
        this.objectMapper = objectMapper;
    }

    public String generatePrompt(TripPlanRequest data) {
        String interestsStr = data.getInterests() != null && data.getInterests().length > 0
                ? String.join(", ", data.getInterests())
                : "general sightseeing";
        String accommodationPref = data.getAccommodationPreference() != null && !data.getAccommodationPreference().isBlank()
                ? data.getAccommodationPreference() : "any reasonable option";

        String destinationCurrencyCode = resolveCurrencyCode(data.getCountryCode(), data.getCity());
        String destinationCurrencyName = resolveCurrencyName(destinationCurrencyCode);

        String currencyName = destinationCurrencyName != null ? destinationCurrencyName : "the local currency";
        String currencyCode = destinationCurrencyCode != null ? destinationCurrencyCode : "the local currency code";

        return """
                TRIP REQUEST:
                - Destination: %s
                - Duration: %d days
                - Travelers: %s
                - Budget Tier: %s
                - Interests: %s
                - Destination Currency: %s (%s)

                %s

                CRITICAL DESTINATION RESEARCH STEP: Before writing the itinerary, STOP and research approximately 15-25 of the most famous, iconic, must-see places for the city of %s. Make a list of real, named landmarks, museums, temples, neighborhoods, markets, parks, viewpoints, beaches, monuments that are genuinely located in %s. This list must be completely unique to %s—different destinations must never share the same famous places.

                Now, generate the itinerary following these critical instructions:
                1. **RESEARCH**: First, identify 15-25 real, famous, and unique places specifically for %s.
                2. **NO GENERIC NAMES**: You are forbidden from using generic placeholders like "Local Restaurant" or "City Park". Every location name MUST be a real, specific name from %s.
                3. **UNIQUE DAYS**: For this %d-day trip, ensure that every single day has completely different attractions. Do NOT repeat any place across multiple days. You must provide %d unique daily plans.
                4. **ALLOCATE PLACES**: Distribute the real places you researched across the %d days, giving each day its own unique set.
                5. **CURRENCY**: All costs MUST be in %s (%s). Do not use any other currency.

                CRITICAL DESTINATION-SPECIFIC RULE: All attractions and places in this itinerary MUST be real, named places drawn from the famous places of %s that you identified in the research step. Do NOT use generic placeholders like "Local Market", "Historic District", "City Park", or "Local Cafe". Use the actual famous landmarks, neighborhoods, museums, and places of %s.

                CRITICAL UNIQUENESS PER DAY RULE: This trip spans %d days. Each day MUST use completely different locations and attractions. It is strictly forbidden to repeat any location, activity, or place across different days. Provide %d fully distinct sets of activities, so the places naturally vary from day to day.

                CRITICAL DAY ALLOCATION RULE: Divide the famous places of %s into %d distinct day-groups—one unique group per day. Day 1 uses only its own group of famous places, Day 2 uses a completely different group, and so on. No place may ever be repeated across days. Every day's schedule entries must be real, named, destination-specific places that appear only on that single day.

                CRITICAL CURRENCY RULE: All costs in this itinerary MUST be expressed in %s (%s). Do NOT use USD, EUR, or any other currency symbol. Every monetary value (`estimatedTotalCost`, activity `estimatedCost`) must use the correct currency symbol or code for %s.
                """.formatted(
                        data.getCity(),              //  1. %s Destination
                        data.getNumberOfDays(),      //  2. %d Duration
                        data.getTravelers(),         //  3. %s Travelers
                        data.getBudget(),            //  4. %s Budget Tier
                        interestsStr,                //  5. %s Interests
                        currencyName,                //  6. %s Destination Currency Name
                        currencyCode,                //  7. %s Destination Currency Code
                        SYSTEM_PROMPT,               //  8. %s System prompt
                        data.getCity(),              //  9. %s RESEARCH: city of %s
                        data.getCity(),              // 10. %s RESEARCH: located in %s
                        data.getCity(),              // 11. %s RESEARCH: unique to %s
                        data.getCity(),              // 12. %s RESEARCH: specifically for %s
                        data.getCity(),              // 13. %s NO GENERIC NAMES: from %s
                        data.getNumberOfDays(),      // 14. %d UNIQUE DAYS: %d-day trip
                        data.getNumberOfDays(),      // 15. %d UNIQUE DAYS: provide %d plans
                        data.getNumberOfDays(),      // 16. %d ALLOCATE: across %d days
                        currencyName,                // 17. %s CURRENCY: in %s
                        currencyCode,                // 18. %s CURRENCY: (%s)
                        data.getCity(),              // 19. %s DESTINATION-SPECIFIC: famous places of %s
                        data.getCity(),              // 20. %s DESTINATION-SPECIFIC: restaurants of %s
                        data.getNumberOfDays(),      // 21. %d UNIQUENESS: spans %d days
                        data.getNumberOfDays(),      // 22. %d UNIQUENESS: provide %d
                        data.getCity(),              // 23. %s DAY ALLOCATION: famous places of %s
                        data.getNumberOfDays(),      // 24. %d DAY ALLOCATION: into %d
                        currencyName,                // 25. %s CURRENCY RULE: expressed in %s
                        currencyCode,                // 26. %s CURRENCY RULE: (%s)
                        currencyName);               // 27. %s CURRENCY RULE: code for %s
    }

    private String resolveCurrencyCode(String countryCode, String city) {
        if (countryCode != null && !countryCode.isBlank()) {
            String code = lookupCurrencyByAlpha2(countryCode);
            if (code != null && !code.isBlank()) return code;
        }
        if (city != null && city.contains(",")) {
            String countryPart = city.substring(city.lastIndexOf(",") + 1).trim();
            String code = lookupCurrencyByCountryName(countryPart);
            if (code != null && !code.isBlank()) return code;
        }
        return null;
    }

    private String resolveCurrencyName(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) return null;
        try {
            java.util.Currency currency = java.util.Currency.getInstance(currencyCode);
            return currency.getDisplayName();
        } catch (Exception e) {
            return null;
        }
    }

    private String lookupCurrencyByAlpha2(String alpha2) {
        try (InputStream in = new ClassPathResource("static/data/countries.json").getInputStream()) {
            List<Map<String, Object>> countries = objectMapper.readValue(in, List.class);
            for (Map<String, Object> country : countries) {
                String code = (String) country.getOrDefault("alpha2", "");
                if (alpha2.equalsIgnoreCase(code)) {
                    Object currency = country.get("currency");
                    if (currency instanceof Map) {
                        return (String) ((Map<?, ?>) currency).get("code");
                    }
                    return (String) currency;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to lookup currency for alpha2: {}", alpha2, e);
        }
        return null;
    }

    private String lookupCurrencyByCountryName(String countryName) {
        try (InputStream in = new ClassPathResource("static/data/countries.json").getInputStream()) {
            List<Map<String, Object>> countries = objectMapper.readValue(in, List.class);
            for (Map<String, Object> country : countries) {
                String name = (String) country.getOrDefault("name", "");
                if (countryName.equalsIgnoreCase(name)) {
                    Object currency = country.get("currency");
                    if (currency instanceof Map) {
                        return (String) ((Map<?, ?>) currency).get("code");
                    }
                    return (String) currency;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to lookup currency for country: {}", countryName, e);
        }
        return null;
    }

    public ItineraryData generateItinerary(TripPlanRequest request) {
        String prompt = generatePrompt(request);

        try {
            TripPlanResponse aiResponse = geminiAiService.generateTripPlan(prompt);
            ItineraryData result = transformToItinerary(request, aiResponse);
            if (result.getDays() != null && !result.getDays().isEmpty()) {
                return result;
            }
            log.warn("First AI attempt returned empty itinerary, retrying with explicit warning...");
        } catch (Exception e) {
            log.error("First AI attempt failed: {}", e.getMessage(), e);
        }

        try {
            String retryPrompt = prompt + "\n\nIMPORTANT: Your previous response was rejected because it contained no itinerary days. You MUST return the complete JSON with the 'itinerary' array containing exactly " + request.getNumberOfDays() + " day objects. Each day must have schedule entries. Failure to include the full itinerary will result in the response being discarded.\n";
            TripPlanResponse aiResponse = geminiAiService.generateTripPlan(retryPrompt);
            ItineraryData result = transformToItinerary(request, aiResponse);
            if (result.getDays() != null && !result.getDays().isEmpty()) {
                return result;
            }
            log.warn("Retry AI attempt returned empty itinerary, using fallback.");
        } catch (Exception e) {
            log.error("Retry AI attempt failed: {}", e.getMessage(), e);
        }

        log.error("All AI attempts failed — falling back to generated itinerary. Check GEMINI_API_KEY and API connectivity.");
        return generateFallbackItinerary(request);
    }

    public ItineraryData transformToItinerary(TripPlanRequest request, TripPlanResponse response) {
        ItineraryData data = new ItineraryData();
        data.setTitle("%d-Day %s Trip".formatted(request.getNumberOfDays(), request.getCity()));
        data.setOverview(response.getTripSummary());
        data.setTotalBudget(parseEstimatedCost(response.getEstimatedTotalCost()));
        data.setDailyBudget(data.getTotalBudget() / request.getNumberOfDays());

        data.setDestination(response.getDestination());
        data.setDuration(response.getDuration());
        data.setTravelers(response.getTravelers());
        data.setBudgetTier(response.getBudgetTier());
        data.setDestinationCurrencyCode(resolveCurrencyCode(request.getCountryCode(), request.getCity()));

        List<ItineraryData.DayData> days = new ArrayList<>();
        List<TripPlanResponse.DayItinerary> dayItinerary = response.getItinerary() != null ? response.getItinerary() : response.getDays();
        if (dayItinerary != null) {
            for (TripPlanResponse.DayItinerary dayIt : dayItinerary) {
                ItineraryData.DayData dayData = new ItineraryData.DayData();
                dayData.setDay(dayIt.getDay());
                dayData.setTitle("Day %d: %s".formatted(dayIt.getDay(), dayIt.getDayTitle()));
                dayData.setTheme(dayIt.getDayTitle());
                dayData.setDayTitle(dayIt.getDayTitle());

                List<ItineraryData.ActivityData> activities = new ArrayList<>();
                if (dayIt.getSchedule() != null) {
                    for (TripPlanResponse.ScheduleItem sched : dayIt.getSchedule()) {
                        ItineraryData.ActivityData actData = new ItineraryData.ActivityData();
                        actData.setTime(sched.getTimeSlot());
                        actData.setActivity(sched.getActivityTitle());
                        actData.setDescription(sched.getDescription());
                        actData.setEstimatedCost(parseCostEstimate(sched.getEstimatedCost()));
                        actData.setTimeOfDay(sched.getTimeOfDay());
                        actData.setTimeSlot(sched.getTimeSlot());
                        actData.setLocationName(sched.getLocationName());
                        actData.setTransitInfo(sched.getTransitInfo());
                        activities.add(actData);
                    }
                }
                dayData.setActivities(activities);

                days.add(dayData);
            }
        }
        data.setDays(days);

        if (days.isEmpty()) {
            log.warn("AI response contained no itinerary days — this usually means the model ignored the schema. AI Response: {}. Falling back to generated itinerary.", response);
            throw new IllegalArgumentException("AI returned empty itinerary");
        }

        boolean hasActivities = days.stream().anyMatch(d -> d.getActivities() != null && !d.getActivities().isEmpty());
        if (!hasActivities) {
            log.warn("AI response contained days but no schedule entries. AI Response: {}. Falling back to generated itinerary.", response);
            throw new IllegalArgumentException("AI returned itinerary with no activities");
        }

        return data;
    }

    /** Matches a number with (optional) thousand/decimal separators, e.g. 2400, 2,400, 1.234,56. */
    private static final Pattern NUMBER_WITH_SEPARATORS =
            Pattern.compile("\\d{1,3}(?:[.,]\\d{3})+|\\d+(?:[.,]\\d+)?|\\d+");

    /** Currency symbols to strip before parsing a numeric cost. */
    private static final String CURRENCY_SYMBOLS = "€$£₹¥₡฿₫₦₩₪₺₾₸₼₽₴₵₶₷₸₹₺₻₼₽₾₿";

    public ItineraryData generateFallbackItinerary(TripPlanRequest request) {
        String destination = request.getCity();
        int days = request.getNumberOfDays();
        String budget = request.getBudget();
        Random rand = new Random();

        ItineraryData data = new ItineraryData();
        data.setTitle("%d-Day %s Trip".formatted(days, destination));
        data.setOverview("Discover the best of %s in %d days with a focus on local culture and attractions tailored to your %s budget."
                .formatted(destination, days, budget));
        data.setDestination(destination);
        data.setDuration(days + " days");
        data.setTravelers(request.getTravelers());
        data.setBudgetTier(budget);
        data.setDestinationCurrencyCode(resolveCurrencyCode(request.getCountryCode(), request.getCity()));

        double totalBudget = switch (budget) {
            case "Budget Friendly" -> 800 + rand.nextInt(400);
            case "Luxury" -> 3000 + rand.nextInt(2000);
            default -> 1200 + rand.nextInt(800);
        };
        data.setTotalBudget(totalBudget);
        data.setDailyBudget(totalBudget / days);

        String[] timeSlots = {"09:00 AM", "11:30 AM", "02:00 PM", "04:30 PM", "07:00 PM", "08:30 PM"};
        String[] themePool = {"Local Exploration", "Cultural Immersion", "Nature & Adventure", "Food & Dining", "Relaxation", "Sightseeing"};

        List<ItineraryData.DayData> dayList = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            ItineraryData.DayData dayData = new ItineraryData.DayData();
            dayData.setDay(d + 1);
            dayData.setTheme(themePool[d % themePool.length]);
            dayData.setTitle("Day %d: %s".formatted(d + 1, dayData.getTheme()));
            dayData.setDayTitle(dayData.getTitle());

            List<ItineraryData.ActivityData> activities = new ArrayList<>();
            int actsPerDay = 3 + rand.nextInt(2);
            for (int a = 0; a < actsPerDay; a++) {
                ItineraryData.ActivityData act = new ItineraryData.ActivityData();
                act.setTime(timeSlots[a % timeSlots.length]);
                String[] places = {"Local Market", "Historic District", "City Park", "Art Gallery",
                        "Waterfront", "Viewpoint", "Cultural Center", "Botanical Garden"};
                act.setActivity("%s %s".formatted(destination, places[a % places.length]));
                act.setDescription("Explore this wonderful %s in %s during your visit."
                        .formatted(places[a % places.length].toLowerCase(), destination));
                act.setEstimatedCost(10 + rand.nextInt(60));
                activities.add(act);
            }
            dayData.setActivities(activities);

            dayList.add(dayData);
        }
        data.setDays(dayList);

        return data;
    }

    private double parseEstimatedCost(String range) {
        if (range == null || range.isBlank()) return 1000;
        String sanitized = stripCurrencySymbols(range);
        Matcher m = NUMBER_WITH_SEPARATORS.matcher(sanitized);
        if (m.find()) {
            String number = m.group();
            number = removeThousandSeparators(number);
            try {
                return Double.parseDouble(number);
            } catch (NumberFormatException e) {
                return 1000;
            }
        }
        return 1000;
    }

    private double parseCostEstimate(String estimate) {
        if (estimate == null) return 30;
        String trimmed = estimate.trim().toLowerCase();
        if (trimmed.equals("free")) return 0;
        if (trimmed.equals("$")) return 25;
        if (trimmed.equals("$$")) return 50;
        if (trimmed.equals("$$$")) return 150;
        String sanitized = stripCurrencySymbols(estimate);
        Matcher m = NUMBER_WITH_SEPARATORS.matcher(sanitized);
        if (m.find()) {
            String number = removeThousandSeparators(m.group());
            try {
                return Double.parseDouble(number);
            } catch (NumberFormatException e) {
                return 30;
            }
        }
        return 30;
    }

    /** Remove common currency symbols so only digits/separators remain before parsing. */
    private String stripCurrencySymbols(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (!CURRENCY_SYMBOLS.contains(String.valueOf(c))) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Remove thousand separators (commas or dots separating groups of 3 digits)
     * while preserving a decimal point. E.g. "2,400" -> "2400", "1,234.56" -> "1234.56".
     */
    private String removeThousandSeparators(String number) {
        return number.replaceAll("(?<=\\d)[.,](?=\\d{3}(?:[.,]\\d{3})*$)", "");
    }
}
