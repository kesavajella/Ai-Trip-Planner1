package com.intellitrip.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.dto.ItineraryData;
import com.intellitrip.dto.TripPlanRequest;
import com.intellitrip.model.Trip;
import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import com.intellitrip.service.CurrencyService;
import com.intellitrip.service.ItineraryGeneratorService;
import com.intellitrip.service.NotificationService;
import com.intellitrip.service.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api")
public class TripApiController {

    private static final Logger log = LoggerFactory.getLogger(TripApiController.class);

    private final ItineraryGeneratorService itineraryGenerator;
    private final TripService tripService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;
    private final Random random = new Random();

    public TripApiController(ItineraryGeneratorService itineraryGenerator, TripService tripService, NotificationService notificationService, UserRepository userRepository, CurrencyService currencyService) {
        this.itineraryGenerator = itineraryGenerator;
        this.tripService = tripService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
    }

@GetMapping("/trips")
    public ResponseEntity<?> getUserTrips(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Trip> trips = tripService.getUserTrips(userId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/trips/saved")
    public ResponseEntity<?> getSavedTrips(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Trip> trips = tripService.getSavedTrips(userId);
        return ResponseEntity.ok(trips);
    }

    @PostMapping("/trips/{id}/save")
    public ResponseEntity<?> saveTrip(@PathVariable String id, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        }
        Optional<Trip> tripOpt = tripService.getTripById(id);
        if (tripOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Trip trip = tripOpt.get();
        if (trip.getUser() == null || !trip.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not allowed to save this trip"));
        }
        trip.setSaved(true);
        tripService.saveTrip(trip);
        return ResponseEntity.ok(Map.of("saved", true, "id", trip.getId()));
    }

    @PutMapping("/trips/{id}/status")
    public ResponseEntity<?> updateTripStatus(@PathVariable String id, @RequestBody Map<String, String> body, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        Optional<Trip> tripOpt = tripService.getTripById(id);
        if (tripOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Trip trip = tripOpt.get();
        if (trip.getUser() == null || !trip.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not allowed to update this trip"));
        }
        String newStatus = body.getOrDefault("status", "completed");
        trip.setStatus(newStatus);
        tripService.saveTrip(trip);
        return ResponseEntity.ok(Map.of("status", trip.getStatus(), "id", trip.getId()));
    }

    @DeleteMapping("/trips/{id}")
    public ResponseEntity<?> deleteTrip(@PathVariable String id, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        }
        tripService.getTripById(id).ifPresent(trip -> {
            if (trip.getUser() != null && trip.getUser().getId().equals(userId)) {
                tripService.deleteTrip(id);
            }
        });
        return ResponseEntity.ok(Map.of("message", "Trip deleted"));
    }

    @PostMapping("/generate-trip")
    public ResponseEntity<?> generateTrip(@RequestBody Map<String, Object> body, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Please log in to generate and save trips",
                        "details", "User session expired or not authenticated"
                ));
            }

            log.info("🔍 [Backend Prompt Input]: {}", body);

            String city = (String) body.getOrDefault("city", "");
            int numberOfDays;
            Object numberOfDaysObj = body.get("numberOfDays");
            if (numberOfDaysObj instanceof Number) {
                numberOfDays = ((Number) numberOfDaysObj).intValue();
            } else if (numberOfDaysObj instanceof String && !((String) numberOfDaysObj).isBlank()) {
                try {
                    numberOfDays = Integer.parseInt(((String) numberOfDaysObj).trim());
                } catch (NumberFormatException e) {
                    numberOfDays = 5;
                }
            } else {
                numberOfDays = 5;
            }
            String budget = (String) body.getOrDefault("budget", "Moderate");
            String travelers = (String) body.getOrDefault("travelers", "Just Me");
            String accommodationPreference = (String) body.getOrDefault("accommodationPreference", "");
            String countryCode = (String) body.getOrDefault("countryCode", "");
            String countryName = (String) body.getOrDefault("countryName", "");
            @SuppressWarnings("unchecked")
            List<String> interestsList = (List<String>) body.getOrDefault("interests", List.of());
            String[] interests = interestsList.toArray(new String[0]);

            if (city == null || city.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required field: city"));
            }
            if (numberOfDays < 1) {
                numberOfDays = 5;
            }

            TripPlanRequest request = new TripPlanRequest();
            request.setCity(city);
            request.setNumberOfDays(numberOfDays);
            request.setBudget(budget);
            request.setTravelers(travelers);
            request.setInterests(interests);
            request.setAccommodationPreference(accommodationPreference);
            request.setCountryCode(countryCode);
            request.setCountryName(countryName);

            log.info("Generating trip for: {} ({} days, {}, {})", city, numberOfDays, budget, travelers);

            ItineraryData itinerary = itineraryGenerator.generateItinerary(request);

            if (itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
                throw new RuntimeException("Generated itinerary has no days");
            }

            log.info("✅ [Gemini Success Output]: Trip generated successfully: {} days", itinerary.getDays().size());

            Trip savedTrip = saveTripFromItinerary(city, countryCode, countryName, numberOfDays, budget, travelers, interestsList, accommodationPreference, itinerary, userId, itinerary.getDestinationCurrencyCode());

            if (userId != null) {
                notificationService.createNotification(
                        userId,
                        "Trip Generated",
                        "Your " + numberOfDays + "-day trip to " + city + " is ready",
                        "trip_generated"
                );
            }

// Serialize the itinerary and attach the persisted trip id so the
            // frontend can offer an explicit "Save Trip" action targeting this trip.
            ObjectMapper mapper = new ObjectMapper();
            java.util.Map<String, Object> payload;
            try {
                payload = mapper.convertValue(itinerary, new TypeReference<java.util.Map<String, Object>>() {});
            } catch (Exception ex) {
                payload = new HashMap<>();
                payload.put("title", itinerary.getTitle());
                payload.put("overview", itinerary.getOverview());
                payload.put("totalBudget", itinerary.getTotalBudget());
                payload.put("dailyBudget", itinerary.getDailyBudget());
                payload.put("days", itinerary.getDays());
                payload.put("destination", itinerary.getDestination());
                payload.put("duration", itinerary.getDuration());
                payload.put("travelers", itinerary.getTravelers());
                payload.put("budgetTier", itinerary.getBudgetTier());
                payload.put("destinationCurrencyCode", itinerary.getDestinationCurrencyCode());
            }
            payload.put("tripId", savedTrip.getId());

            if (userId != null) {
                java.util.Optional<User> maybeUser = userRepository.findById(userId);
                if (maybeUser.isPresent()) {
                    User user = maybeUser.get();
                    String userCurrencyCode = null;
                    if (user.getCountryCode() != null && !user.getCountryCode().isBlank()) {
                        userCurrencyCode = currencyService.currencyCodeForCountryCode(user.getCountryCode());
                    }
                    if (userCurrencyCode == null) {
                        userCurrencyCode = currencyService.currencyCode(null);
                    }
                    double userRate = currencyService.usdRateForCurrency(userCurrencyCode);
                    payload.put("userCountryCurrencyCode", userCurrencyCode);
                    payload.put("userCountryRate", userRate);
                    payload.put("convertedBudget", currencyService.convertUsdToLocal(itinerary.getTotalBudget(), userCurrencyCode));
                }
            }

            return ResponseEntity.ok(payload);

} catch (Exception e) {
            log.error("🔥 [Backend Server API Error]: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to generate itinerary",
                            "details", e.getMessage() != null ? e.getMessage() : "Failed to generate trip itinerary"));
        }
    }

    private Trip saveTripFromItinerary(String city, String countryCode, String countryName, int days, String budget, String travelers, List<String> interests, String accommodationPreference, ItineraryData itinerary, String userId, String destinationCurrencyCode) {
        Trip trip = new Trip();

        String[] cityParts = city.split(",");
        trip.setDestination(cityParts[0].trim());
        if (cityParts.length > 1) {
            trip.setCountry(cityParts[1].trim());
        }

        String[] staticImages = {
            "/images/image1.jpeg", "/images/image2.jpeg", "/images/image3.jpeg",
            "/images/image4.jpeg", "/images/image5.jpeg", "/images/image6.jpeg",
            "/images/image7.jpeg", "/images/image8.jpeg", "/images/image9.jpg",
            "/images/image10.jpeg", "/images/image11.jpeg", "/images/image12.jpeg",
            "/images/image13.jpeg", "/images/image14.jpeg", "/images/image15.jpeg"
        };
        trip.setImage(staticImages[random.nextInt(staticImages.length)]);

        trip.setDays(days);
        trip.setBudget(budget);
        trip.setBudgetUsd(itinerary.getTotalBudget());
        trip.setTravelType(travelers);
        trip.setInterests(interests != null ? String.join(", ", interests) : "");
        trip.setAccommodationPreference(accommodationPreference != null ? accommodationPreference : "");
        trip.setDestinationCurrencyCode(destinationCurrencyCode);
        trip.setItineraryJson(mapItineraryToJson(itinerary));
        trip.setStatus("upcoming");
        trip.setSaved(true);
        trip.setStartDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        trip.setCreatedAt(LocalDateTime.now());

        if (userId != null) {
            userRepository.findById(userId).ifPresent(trip::setUser);
        }

        return tripService.saveTrip(trip);
    }

    private String mapItineraryToJson(ItineraryData itinerary) {
        try {
            List<Map<String, Object>> daysList = new ArrayList<>();
            if (itinerary.getDays() != null) {
                for (ItineraryData.DayData day : itinerary.getDays()) {
                    List<Map<String, Object>> actList = new ArrayList<>();
                    if (day.getActivities() != null) {
                        for (ItineraryData.ActivityData act : day.getActivities()) {
                            Map<String, Object> actMap = new HashMap<>();
                            actMap.put("time", act.getTime());
                            actMap.put("activity", act.getActivity());
                            actMap.put("description", act.getDescription());
                            actMap.put("estimatedCost", act.getEstimatedCost());
                            actMap.put("timeOfDay", act.getTimeOfDay());
                            actMap.put("timeSlot", act.getTimeSlot());
                            actMap.put("locationName", act.getLocationName());
                            actMap.put("transitInfo", act.getTransitInfo());
                            actList.add(actMap);
                        }
                    }
                    Map<String, Object> dayMap = new HashMap<>();
                    dayMap.put("day", day.getDay());
                    dayMap.put("title", day.getTitle());
                    dayMap.put("theme", day.getTheme());
                    dayMap.put("dayTitle", day.getDayTitle());
                    dayMap.put("activities", actList);
                    daysList.add(dayMap);
                }
            }

            Map<String, Object> itineraryMap = new HashMap<>();
            itineraryMap.put("title", itinerary.getTitle());
            itineraryMap.put("overview", itinerary.getOverview());
            itineraryMap.put("totalBudget", itinerary.getTotalBudget());
            itineraryMap.put("dailyBudget", itinerary.getDailyBudget());
            itineraryMap.put("destination", itinerary.getDestination());
            itineraryMap.put("duration", itinerary.getDuration());
            itineraryMap.put("travelers", itinerary.getTravelers());
            itineraryMap.put("budgetTier", itinerary.getBudgetTier());
            itineraryMap.put("destinationCurrencyCode", itinerary.getDestinationCurrencyCode());
            itineraryMap.put("days", daysList);

            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(itineraryMap);
        } catch (Exception e) {
            log.warn("Failed to serialize itinerary to JSON: {}", e.getMessage());
            return "";
        }
    }
}