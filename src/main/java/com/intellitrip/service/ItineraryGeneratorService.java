package com.intellitrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.dto.ItineraryData;
import com.intellitrip.dto.TripPlanRequest;
import com.intellitrip.dto.TripPlanResponse;
import com.intellitrip.exception.GeminiQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ItineraryGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGeneratorService.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert travel planner. Create a realistic, season-aware travel plan based on the following trip details.

            CRITICAL INSTRUCTIONS FOR SPEED & ACCURACY:
            1. Seasonality: Use the calendar month from the Start Date to recommend authentic, native, and weather-appropriate places, local seasonal foods, and avoid attractions closed for the season.
            2. Transit:
               - Provide strictly ONE best round-trip transportation route (Source -> Destination -> Source).
               - In each day's plan, specify how to travel locally between sights (e.g., "Metro Line 1", "Walk 10m", "Local Rickshaw/Taxi").
               - List 2 to 3 essential local transport/ride-hailing apps to download.
            3. Conciseness: Keep each activity and description strictly under 15 words to maximize response generation speed.
            4. Output Format: Return strictly valid JSON adhering to the exact schema below. No markdown backticks, no markdown formatting, and no introductory text.

            STRICT JSON OUTPUT SCHEMA REQUIRED (match the field names exactly):
            {
              "tripOverview": {
                "destination": "String (the destination city/country)",
                "source": "String (the source city/country from the user's request)",
                "startDate": "YYYY-MM-DD (the exact start date from user's request)",
                "totalDays": Number (number of days from user's request)",
                "seasonalHighlights": "Brief note on local weather and vibe for this month"
              },
              "hotels": [
                {
                  "name": "Hotel Name",
                  "area": "Neighborhood or Landmark area",
                  "pricePerNight": Number,
                  "bestFor": "Budget / Location / Families"
                }
              ],
              "transport": {
                "roundTrip": {
                  "mode": "Flight / Train / Express Bus",
                  "route": "Source to Destination and return",
                  "duration": "e.g., 2h 15m each way",
                  "estimatedCost": Number,
                  "recommendedBookingApp": "e.g., MakeMyTrip / Skyscanner / IRCTC"
                },
                "localTransportApps": [
                  { "name": "App Name", "purpose": "Ride-hailing / Metro tickets / Transit maps" }
                ]
              },
              "budget": {
                "currency": "INR / USD / EUR",
                "estimatedAccommodationTotal": Number,
                "estimatedFoodTotal": Number,
                "estimatedTransportTotal": Number,
                "estimatedActivitiesTotal": Number,
                "estimatedOtherTotal": Number,
                "grandTotal": Number
              },
              "itinerary": [
                {
                  "day": Number,
                  "date": "YYYY-MM-DD (calculate from startDate: day 1 = startDate, day 2 = startDate+1, etc.)",
                  "theme": "Arrival & City Center",
                  "activities": [
                    {
                      "timeSlot": "Morning / Afternoon / Evening",
                      "place": "Native Attraction Name",
                      "transitToPlace": "Metro Line 2 / 10 min walk",
                      "description": "Short 10-word activity description"
                    }
                  ],
                  "localFoodSpot": "Famous local restaurant or street food market"
                }
              ]
            }

            RULES FOR BUDGET ESTIMATION:
            1. All amounts MUST be in the destination currency specified in the prompt and realistic for travel to that destination.
            2. Calculate costs using this formula: Total = (Daily Stay + Daily Food + Daily Activities + Daily Transport + Daily Other) * Number of Days.
            3. A longer stay MUST ALWAYS cost more than a shorter stay for the same destination and budget tier.
            4. Output strict numeric values without currency symbols inside numeric fields.
            5. You MUST include "estimatedDailyCosts" in the JSON response with these exact numeric fields: "accommodationPerNight", "foodPerDay", "interestsPerDay".

            RULES:
            - **CRITICAL RULE 1: RESEARCH REAL PLACES**: Before generating the itinerary, you MUST research and identify 15-25 famous, iconic, must-see places for the exact destination city in the user's request. These must be real, named landmarks, museums, temples, neighborhoods, markets, parks, viewpoints, beaches, monuments, and notable restaurants that are genuinely located in that specific destination.
            - **CRITICAL RULE 2: USE ONLY REAL, DESTINATION-SPECIFIC PLACES**: Every single `place` in the entire itinerary MUST be a real, named place from the destination city you researched. DO NOT use generic, made-up, or template place names like "Local Market", "Historic District", "City Park", "Art Gallery", "Waterfront", "Viewpoint", "Cultural Center", "Botanical Garden", "Local Cafe", "Quick Bite", or "Sit-down Restaurant".
            - **CRITICAL RULE 3: UNIQUE PLACES PER DAY**: Every day of the itinerary MUST feature completely different and unique locations and attractions. It is strictly forbidden to repeat ANY `place` across different days. For a 5-day trip, you must provide 5 completely distinct sets of places.
            - **CRITICAL RULE 4: SEASONAL ACCURACY**: Match activities and food recommendations to the season/month. Suggest indoor attractions during monsoon/hot months. Recommend seasonal festivals or events if applicable.
            - **CRITICAL RULE 5: DATE CALCULATION**: Calculate each day's date from the startDate. Day 1 = startDate, Day 2 = startDate + 1 day, etc. Use proper date formatting (YYYY-MM-DD).
            - The `itinerary` array MUST contain exactly the number of days requested by the user. If the user asks for 5 days, you MUST return exactly 5 day objects.
            - Every day MUST have at least 3 activity entries (Morning, Afternoon, Evening) with real, non-generic `place` values.
            - Respond ONLY with a valid, raw JSON string. Do not include markdown formatting like ```json. Do not include any conversational text before or after the JSON.
            - If you cannot generate a complete itinerary for the requested duration, DO NOT return a partial response. You must return the full schema with all days filled out.
            """;

    private static final double TIER_BUDGET_DAILY_INR = 3250;
    private static final double TIER_MODERATE_DAILY_INR = 7000;
    private static final double TIER_LUXURY_DAILY_INR = 18000;
    private static final String BASE_CURRENCY_INR = "INR";

    private final GeminiAiService geminiAiService;
    private final ItineraryCacheService itineraryCacheService;
    private final ObjectMapper objectMapper;
    private final CurrencyService currencyService;

    public ItineraryGeneratorService(GeminiAiService geminiAiService, ItineraryCacheService itineraryCacheService, ObjectMapper objectMapper, CurrencyService currencyService) {
        this.geminiAiService = geminiAiService;
        this.itineraryCacheService = itineraryCacheService;
        this.objectMapper = objectMapper;
        this.currencyService = currencyService;
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

        double dailyRateInr = dailyRateInrForTier(data.getBudget());
        double targetDailyBudget = convertInrToDestination(dailyRateInr, destinationCurrencyCode);
        String formattedDailyBudget = String.format(java.util.Locale.US, "%.2f", targetDailyBudget);

        return """
                TRIP REQUEST:
                - Source: %s
                - Destination: %s
                - Start Date: %s
                - Duration: %d days
                - Travelers: %s
                - Budget Tier: %s
                - Interests: %s
                - Destination Currency: %s (%s)
                - Target Daily Budget: %s %s (STRICT: do not exceed this per-person daily rate)

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
                        data.getSource() != null ? data.getSource() : "Not specified", //  1. %s Source
                        data.getCity(),              //  2. %s Destination
                        data.getStartDate() != null ? data.getStartDate() : "Not specified", //  3. %s Start Date
                        data.getNumberOfDays(),      //  4. %d Duration
                        data.getTravelers(),         //  5. %s Travelers
                        data.getBudget(),            //  6. %s Budget Tier
                        interestsStr,                //  7. %s Interests
                        currencyName,                //  8. %s Destination Currency Name
                        currencyCode,                //  9. %s Destination Currency Code
                        formattedDailyBudget,        // 10. %s Target Daily Budget
                        currencyCode,                // 11. %s Currency Code for budget
                        SYSTEM_PROMPT,               // 12. %s System prompt
                        data.getCity(),              // 13. %s RESEARCH: city of %s
                        data.getCity(),              // 14. %s RESEARCH: located in %s
                        data.getCity(),              // 15. %s RESEARCH: unique to %s
                        data.getCity(),              // 16. %s RESEARCH: specifically for %s
                        data.getCity(),              // 17. %s NO GENERIC NAMES: from %s
                        data.getNumberOfDays(),      // 18. %d UNIQUE DAYS: %d-day trip
                        data.getNumberOfDays(),      // 19. %d UNIQUE DAYS: provide %d plans
                        data.getNumberOfDays(),      // 20. %d ALLOCATE: across %d days
                        currencyName,                // 21. %s CURRENCY: in %s
                        currencyCode,                // 22. %s CURRENCY: (%s)
                        data.getCity(),              // 23. %s DESTINATION-SPECIFIC: famous places of %s
                        data.getCity(),              // 24. %s DESTINATION-SPECIFIC: restaurants of %s
                        data.getNumberOfDays(),      // 25. %d UNIQUENESS: spans %d days
                        data.getNumberOfDays(),      // 26. %d UNIQUENESS: provide %d
                        data.getCity(),              // 27. %s DAY ALLOCATION: famous places of %s
                        data.getNumberOfDays(),      // 28. %d DAY ALLOCATION: into %d
                        currencyName,                // 29. %s CURRENCY RULE: expressed in %s
                        currencyCode,                // 30. %s CURRENCY RULE: (%s)
                        currencyName);               // 31. %s CURRENCY RULE: code for %s
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

    private double dailyRateInrForTier(String budgetTier) {
        return switch (budgetTier) {
            case "Budget Friendly" -> TIER_BUDGET_DAILY_INR;
            case "Luxury" -> TIER_LUXURY_DAILY_INR;
            default -> TIER_MODERATE_DAILY_INR;
        };
    }

    private double convertInrToDestination(double inrAmount, String destCurrencyCode) {
        if (destCurrencyCode == null || destCurrencyCode.isBlank() || BASE_CURRENCY_INR.equalsIgnoreCase(destCurrencyCode)) {
            return inrAmount;
        }
        double inrRate = currencyService.usdRateForCurrency(BASE_CURRENCY_INR);
        double destRate = currencyService.usdRateForCurrency(destCurrencyCode);
        if (inrRate <= 0) return inrAmount;
        return inrAmount * (destRate / inrRate);
    }

    public ItineraryData generateItinerary(TripPlanRequest request) {
        return generateItineraryWithRawResponse(request).itinerary();
    }

    public ItineraryGenerationResult generateItineraryWithRawResponse(TripPlanRequest request) {
        Optional<ItineraryData> cached = itineraryCacheService.get(
                request.getCity(),
                request.getSource(),
                request.getStartDate(),
                request.getNumberOfDays(),
                request.getBudget(),
                request.getTravelers()
        );
        if (cached.isPresent()) {
            return new ItineraryGenerationResult(cached.get(), null);
        }

        String prompt = generatePrompt(request);

        try {
            TripPlanResponse aiResponse = geminiAiService.generateTripPlan(prompt);
            ItineraryData result = transformToItinerary(request, aiResponse);
            if (result.getDays() != null && !result.getDays().isEmpty()) {
                itineraryCacheService.put(
                        request.getCity(),
                        request.getSource(),
                        request.getStartDate(),
                        request.getNumberOfDays(),
                        request.getBudget(),
                        request.getTravelers(),
                        result
                );
                return new ItineraryGenerationResult(result, aiResponse);
            }
            log.warn("First AI attempt returned empty itinerary, retrying with explicit warning...");
        } catch (GeminiQuotaExceededException e) {
            log.error("Gemini API quota exhausted on first attempt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("First AI attempt failed: {}", e.getMessage(), e);
        }

        try {
            String retryPrompt = prompt + "\n\nIMPORTANT: Your previous response was rejected because it contained no itinerary days. You MUST return the complete JSON with the 'itinerary' array containing exactly " + request.getNumberOfDays() + " day objects. Each day must have schedule entries. Failure to include the full itinerary will result in the response being discarded.\n";
            TripPlanResponse aiResponse = geminiAiService.generateTripPlan(retryPrompt);
            ItineraryData result = transformToItinerary(request, aiResponse);
            if (result.getDays() != null && !result.getDays().isEmpty()) {
                itineraryCacheService.put(
                        request.getCity(),
                        request.getSource(),
                        request.getStartDate(),
                        request.getNumberOfDays(),
                        request.getBudget(),
                        request.getTravelers(),
                        result
                );
                return new ItineraryGenerationResult(result, aiResponse);
            }
            log.warn("Retry AI attempt returned empty itinerary, using fallback.");
        } catch (GeminiQuotaExceededException e) {
            log.error("Gemini API quota exhausted on retry attempt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Retry AI attempt failed: {}", e.getMessage(), e);
        }

        log.error("All AI attempts failed — falling back to generated itinerary. Check GEMINI_API_KEY and API connectivity.");
        return new ItineraryGenerationResult(generateFallbackItinerary(request), null);
    }

    public record ItineraryGenerationResult(ItineraryData itinerary, TripPlanResponse rawResponse) {}

    public ItineraryData transformToItinerary(TripPlanRequest request, TripPlanResponse response) {
        ItineraryData data = new ItineraryData();

        if (response.getTripOverview() != null) {
            data.setTitle("%d-Day %s Trip".formatted(
                    response.getTripOverview().getTotalDays() > 0 ? response.getTripOverview().getTotalDays() : request.getNumberOfDays(),
                    response.getTripOverview().getDestination() != null ? response.getTripOverview().getDestination() : request.getCity()));
            data.setDestination(response.getTripOverview().getDestination());
            data.setDuration(response.getTripOverview().getTotalDays() + " days");
        } else {
            data.setTitle("%d-Day %s Trip".formatted(request.getNumberOfDays(), request.getCity()));
            data.setDestination(request.getCity());
            data.setDuration(request.getNumberOfDays() + " days");
        }

        data.setSource(request.getSource() != null ? request.getSource() : (response.getTripOverview() != null ? response.getTripOverview().getSource() : null));
        data.setStartDate(request.getStartDate() != null ? request.getStartDate() : (response.getTripOverview() != null ? response.getTripOverview().getStartDate() : null));
        data.setTravelers(request.getTravelers());
        data.setBudgetTier(request.getBudget());
        data.setDestinationCurrencyCode(resolveCurrencyCode(request.getCountryCode(), request.getCity()));
        data.setHotels(response.getHotels());
        data.setTransport(response.getTransport());
        data.setBudgetBreakdown(response.getBudget());
        data.setTripOverview(response.getTripOverview());

        List<ItineraryData.DayData> days = new ArrayList<>();
        List<TripPlanResponse.DayItinerary> dayItinerary = response.getItinerary() != null ? response.getItinerary() : response.getDays();
        if (dayItinerary != null) {
            for (TripPlanResponse.DayItinerary dayIt : dayItinerary) {
                ItineraryData.DayData dayData = new ItineraryData.DayData();
                dayData.setDay(dayIt.getDay());
                String theme = dayIt.getTheme() != null ? dayIt.getTheme() : (dayIt.getDate() != null ? "Day " + dayIt.getDay() : "Day " + dayIt.getDay());
                dayData.setTitle(theme);
                dayData.setTheme(theme);
                dayData.setDayTitle(theme);

                String dayDate = dayIt.getDate();
                if ((dayDate == null || dayDate.isBlank()) && data.getStartDate() != null && !data.getStartDate().isBlank()) {
                    try {
                        java.time.LocalDate start = java.time.LocalDate.parse(data.getStartDate().trim());
                        dayDate = start.plusDays(dayIt.getDay() - 1).toString();
                    } catch (Exception ignored) {}
                }
                dayData.setDate(dayDate);

                List<ItineraryData.ActivityData> activities = new ArrayList<>();
                if (dayIt.getActivities() != null) {
                    for (TripPlanResponse.Activity act : dayIt.getActivities()) {
                        ItineraryData.ActivityData actData = new ItineraryData.ActivityData();
                        actData.setTime(act.getTimeSlot());
                        actData.setActivity(act.getPlace());
                        actData.setDescription(act.getDescription());
                        actData.setTimeOfDay(act.getTimeSlot());
                        actData.setTimeSlot(act.getTimeSlot());
                        actData.setLocationName(act.getPlace());
                        actData.setTransitInfo(act.getTransitToPlace());
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

        double dailyRateInr = dailyRateInrForTier(request.getBudget());
        double destDailyRate = convertInrToDestination(dailyRateInr, data.getDestinationCurrencyCode());
        double totalBudget = destDailyRate * request.getNumberOfDays();
        double inrRate = currencyService.usdRateForCurrency(BASE_CURRENCY_INR);
        double totalBudgetUsd = inrRate > 0 ? (dailyRateInr / inrRate) * request.getNumberOfDays() : totalBudget;

        data.setTotalBudget(totalBudget);
        data.setTotalBudgetUsd(totalBudgetUsd);
        data.setDailyBudget(destDailyRate);

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
        data.setSource(request.getSource());
        data.setStartDate(request.getStartDate());
        data.setDuration(days + " days");
        data.setTravelers(request.getTravelers());
        data.setBudgetTier(budget);
        data.setDestinationCurrencyCode(resolveCurrencyCode(request.getCountryCode(), request.getCity()));

        double dailyRateInr = dailyRateInrForTier(budget);
        double destDailyRate = convertInrToDestination(dailyRateInr, data.getDestinationCurrencyCode());
        double totalBudget = destDailyRate * days;
        double inrRate = currencyService.usdRateForCurrency(BASE_CURRENCY_INR);
        double totalBudgetUsd = inrRate > 0 ? (dailyRateInr / inrRate) * days : totalBudget;

        data.setTotalBudget(totalBudget);
        data.setTotalBudgetUsd(totalBudgetUsd);
        data.setDailyBudget(destDailyRate);

        String[] timeSlots = {"09:00 AM", "11:30 AM", "02:00 PM", "04:30 PM", "07:00 PM", "08:30 PM"};
        String[] themePool = {"Local Exploration", "Cultural Immersion", "Nature & Adventure", "Food & Dining", "Relaxation", "Sightseeing"};

        List<ItineraryData.DayData> dayList = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            ItineraryData.DayData dayData = new ItineraryData.DayData();
            dayData.setDay(d + 1);
            dayData.setTheme(themePool[d % themePool.length]);
            dayData.setTitle("Day %d: %s".formatted(d + 1, dayData.getTheme()));
            dayData.setDayTitle(dayData.getTitle());

            if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
                try {
                    java.time.LocalDate start = java.time.LocalDate.parse(request.getStartDate().trim());
                    dayData.setDate(start.plusDays(d).toString());
                } catch (Exception ignored) {}
            }

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
