package com.intellitrip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.dto.ItineraryData;
import com.intellitrip.dto.TripPlanResponse;
import com.intellitrip.model.Trip;
import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import com.intellitrip.service.CurrencyService;
import com.intellitrip.service.TripService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

 @Controller
 @RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final TripService tripService;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;
    private final ObjectMapper objectMapper;
    private static final String[] INTERESTS_LIST = {"adventure", "food", "culture", "nature", "nightlife"};
    private static final String[] BUDGETS = {"Budget Friendly", "Moderate", "Luxury"};
    private static final String[] TRAVELERS = {"Just Me", "A Couple", "Family", "Friends"};

    public DashboardController(TripService tripService, UserRepository userRepository, CurrencyService currencyService, ObjectMapper objectMapper) {
        this.tripService = tripService;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
        this.objectMapper = objectMapper;
    }

    private User getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("dashboardTitle", "Welcome back, " + user.getName() + ".");
        model.addAttribute("dashboardSubtitle", "Here's what's on your horizon. Ready to plan another?");

        List<Trip> userTrips = tripService.getUserTrips(user.getId());
        List<Trip> upcoming = userTrips.stream().filter(t -> "upcoming".equals(t.getStatus())).toList();

        List<Trip> upcomingPreview = upcoming.stream().limit(3).toList();

        // A trip is "past" when its start date + duration falls on or before today.
        LocalDate today = LocalDate.now();
        List<Trip> pastTrips = userTrips.stream()
                .filter(t -> isPast(t, today))
                .sorted(Comparator.comparing(Trip::getCreatedAt).reversed())
                .limit(3)
                .toList();

        model.addAttribute("upcoming", upcomingPreview);
        model.addAttribute("upcomingCount", upcoming.size());
        model.addAttribute("pastTrips", pastTrips);
        model.addAttribute("pastCount", pastTrips.size());
        model.addAttribute("totalTrips", userTrips.size());
        model.addAttribute("upcomingDays", upcoming.stream().mapToInt(Trip::getDays).sum());
        model.addAttribute("totalBudget", userTrips.stream().mapToDouble(Trip::getBudgetUsd).sum());
        addUserCountryCurrency(model, user);

        return "dashboard/dashboard";
    }

    private boolean isPast(Trip trip, LocalDate today) {
        if (trip.getStartDate() == null || trip.getStartDate().isBlank()) {
            return "completed".equalsIgnoreCase(trip.getStatus());
        }
        try {
            LocalDate start = LocalDate.parse(trip.getStartDate().trim());
            return start.plusDays(trip.getDays()).isBefore(today) || start.plusDays(trip.getDays()).equals(today);
        } catch (Exception e) {
            return "completed".equalsIgnoreCase(trip.getStatus());
        }
    }

@GetMapping("/generate")
    public String generatePage(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("interests", INTERESTS_LIST);
        model.addAttribute("budgets", BUDGETS);
        model.addAttribute("travelers", TRAVELERS);
        addUserCountryCurrency(model, user);
        return "dashboard/generate";
    }

    @GetMapping("/saved")
    public String savedPage(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "dashboard/saved";
    }

    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        addUserCountryCurrency(model, user);
        return "dashboard/settings";
    }

    @GetMapping("/trips")
    public String tripsPage(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);

        List<Trip> allTrips = tripService.getUserTrips(user.getId());
        model.addAttribute("trips", allTrips);
        model.addAttribute("tripsSubtitle", "Every itinerary you've planned, one place.");
        return "dashboard/trips";
    }

@GetMapping("/trips/{id}")
    public String tripDetail(@PathVariable String id, HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        addUserCountryCurrency(model, user);

        Optional<Trip> tripOpt = tripService.getTripById(id);
        if (tripOpt.isEmpty()) return "redirect:/dashboard/trips";
        Trip trip = tripOpt.get();

        if (trip.getRawGeminiResponse() != null && !trip.getRawGeminiResponse().isBlank()) {
            try {
                TripPlanResponse rawResponse = objectMapper.readValue(trip.getRawGeminiResponse(), TripPlanResponse.class);
                ItineraryData hydrated = hydrateItineraryFromRaw(rawResponse);
                String hydratedJson = objectMapper.writeValueAsString(hydrated);
                trip.setItineraryJson(hydratedJson);
            } catch (Exception e) {
                log.warn("Failed to hydrate itinerary from raw Gemini response for trip {}: {}", id, e.getMessage());
            }
        }

        model.addAttribute("trip", trip);
        return "dashboard/trip-detail";
    }

    private ItineraryData hydrateItineraryFromRaw(TripPlanResponse raw) {
        ItineraryData data = new ItineraryData();
        data.setTitle("%d-Day %s Trip".formatted(
                raw.getItinerary() != null ? raw.getItinerary().size() : (raw.getDays() != null ? raw.getDays().size() : 0),
                raw.getDestination() != null ? raw.getDestination() : "Trip"
        ));
        data.setOverview(raw.getTripSummary());
        data.setDestination(raw.getDestination());
        data.setDuration(raw.getDuration());
        data.setTravelers(raw.getTravelers());
        data.setBudgetTier(raw.getBudgetTier());

        List<ItineraryData.DayData> days = new ArrayList<>();
        List<TripPlanResponse.DayItinerary> dayItinerary = raw.getItinerary() != null ? raw.getItinerary() : raw.getDays();
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

        if (raw.getHotels() != null) data.setHotels(raw.getHotels());
        if (raw.getTransport() != null) data.setTransport(raw.getTransport());
        if (raw.getBudget() != null) data.setBudgetBreakdown(raw.getBudget());
        if (raw.getTripOverview() != null) data.setTripOverview(raw.getTripOverview());

        return data;
    }

    private double parseCostEstimate(String estimate) {
        if (estimate == null) return 30;
        String trimmed = estimate.trim().toLowerCase();
        if (trimmed.equals("free")) return 0;
        if (trimmed.equals("$")) return 25;
        if (trimmed.equals("$$")) return 50;
        if (trimmed.equals("$$$")) return 150;
        String sanitized = estimate.replaceAll("[^\\d.,]", "");
        if (sanitized.isBlank()) return 30;
        try {
            sanitized = sanitized.replaceAll("(?<=\\d)[.,](?=\\d{3}(?:[.,]\\d{3})*$)", "");
            return Double.parseDouble(sanitized);
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    /**
     * Expose the logged-in user's country currency code and USD rate so
     * views can render a second "Total Cost" in the user's local currency.
     * Falls back to the requesting locale's currency when the user has no
     * country set.
     */
    private void addUserCountryCurrency(Model model, User user) {
        String currencyCode = null;
        if (user != null && user.getCountryCode() != null && !user.getCountryCode().isBlank()) {
            currencyCode = currencyService.currencyCodeForCountryCode(user.getCountryCode());
        }
        if (currencyCode == null) {
            currencyCode = currencyService.currencyCode(null);
        }
        model.addAttribute("userCountryCurrencyCode", currencyCode);
        model.addAttribute("userCountryRate", currencyService.usdRateForCurrency(currencyCode));
    }

    @PostMapping("/trips/save")
    public String saveTrip(@ModelAttribute Trip trip, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        trip.setUser(user);
        tripService.saveTrip(trip);
        return "redirect:/dashboard/trips";
    }

    @PostMapping("/trips/delete/{id}")
    public String deleteTrip(@PathVariable String id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) return "redirect:/login";
        tripService.getTripById(id).ifPresent(trip -> {
            if (trip.getUser() != null && trip.getUser().getId().equals(user.getId())) {
                tripService.deleteTrip(id);
            }
        });
        return "redirect:/dashboard/trips";
    }
}
