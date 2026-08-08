package com.intellitrip.controller;

import com.intellitrip.model.Trip;
import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import com.intellitrip.service.CurrencyService;
import com.intellitrip.service.TripService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

 @Controller
 @RequestMapping("/dashboard")
public class DashboardController {

    private final TripService tripService;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;
    private static final String[] INTERESTS_LIST = {"adventure", "food", "culture", "nature", "nightlife"};
    private static final String[] BUDGETS = {"Budget Friendly", "Moderate", "Luxury"};
    private static final String[] TRAVELERS = {"Just Me", "A Couple", "Family", "Friends"};

    public DashboardController(TripService tripService, UserRepository userRepository, CurrencyService currencyService) {
        this.tripService = tripService;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
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
        List<Trip> drafts = userTrips.stream().filter(t -> "draft".equals(t.getStatus())).toList();
        List<Trip> completed = userTrips.stream().filter(t -> "completed".equals(t.getStatus())).toList();

        model.addAttribute("upcoming", upcoming);
        model.addAttribute("drafts", drafts);
        model.addAttribute("completed", completed);
        model.addAttribute("totalTrips", userTrips.size());
        model.addAttribute("upcomingDays", upcoming.stream().mapToInt(Trip::getDays).sum());
        model.addAttribute("totalBudget", upcoming.stream().mapToDouble(Trip::getBudgetUsd).sum());
        addUserCountryCurrency(model, user);

        return "dashboard/dashboard";
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
        model.addAttribute("trip", tripOpt.get());
        return "dashboard/trip-detail";
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
