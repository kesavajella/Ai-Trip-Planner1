package com.intellitrip.controller;

import com.intellitrip.model.Trip;
import com.intellitrip.model.User;
import com.intellitrip.repository.TripRepository;
import com.intellitrip.repository.UserRepository;
import com.intellitrip.service.TripService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;

    public AdminController(UserRepository userRepository, TripRepository tripRepository, TripService tripService) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripService = tripService;
    }

    @GetMapping
    public String adminDashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null || !user.isAdmin()) return "redirect:/login";
        model.addAttribute("user", user);

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalTrips", tripRepository.count());

        List<Trip> recentTrips = tripService.getAllTrips();
        if (recentTrips.size() > 3) recentTrips = recentTrips.subList(0, 3);
        model.addAttribute("recentTrips", recentTrips);

        return "admin/admin";
    }

    @GetMapping("/users")
    public String adminUsers(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null || !user.isAdmin()) return "redirect:/login";
        model.addAttribute("user", user);

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        return "admin/users";
    }

    @GetMapping("/trips")
    public String adminTrips(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null || !user.isAdmin()) return "redirect:/login";
        model.addAttribute("user", user);

        List<Trip> allTrips = tripService.getAllTrips();
        model.addAttribute("trips", allTrips);
        return "admin/trips";
    }

    private User getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @PostMapping("/users/status/{id}")
    public String toggleUserStatus(@PathVariable String id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setStatus("active".equals(u.getStatus()) ? "suspended" : "active");
            userRepository.save(u);
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/trips/delete/{id}")
    public String deleteTrip(@PathVariable String id) {
        tripService.deleteTrip(id);
        return "redirect:/admin/trips";
    }
}
