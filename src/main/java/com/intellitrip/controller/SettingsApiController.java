package com.intellitrip.controller;

import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import com.intellitrip.service.TripService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class SettingsApiController {

    private static final Logger log = LoggerFactory.getLogger(SettingsApiController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TripService tripService;

    public SettingsApiController(UserRepository userRepository, PasswordEncoder passwordEncoder, TripService tripService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tripService = tripService;
    }

    private User getAuthenticatedUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getUserSettings(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("name", user.getName());
        resp.put("email", user.getEmail());
        resp.put("country", user.getCountry() != null ? user.getCountry() : "");
        resp.put("countryCode", user.getCountryCode() != null ? user.getCountryCode() : "");
        resp.put("preferredBudget", user.getPreferredBudget() != null ? user.getPreferredBudget() : "Moderate");
        resp.put("travelStyle", user.getTravelStyle() != null ? user.getTravelStyle() : "Just Me");
        resp.put("accommodation", user.getAccommodation() != null ? user.getAccommodation() : "Boutique Hotel");
        resp.put("interests", user.getInterests() != null ? user.getInterests() : "food, culture");
        resp.put("foodPreference", user.getFoodPreference() != null ? user.getFoodPreference() : "All");
        resp.put("tripReminders", user.getTripReminders());
        resp.put("travelRecommendations", user.getTravelRecommendations());

        return ResponseEntity.ok(resp);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String name = body.get("name");
        String country = body.get("country");
        String countryCode = body.get("countryCode");

        if (name != null && !name.trim().isBlank()) {
            user.setName(name.trim());
            session.setAttribute("userName", user.getName());
        }
        if (country != null) user.setCountry(country.trim());
        if (countryCode != null) user.setCountryCode(countryCode.trim());

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully", "name", user.getName()));
    }

    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String newEmail = body.get("email");
        String password = body.get("password");

        if (newEmail == null || newEmail.trim().isBlank() || !newEmail.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email address is required"));
        }

        if (password != null && !password.isBlank()) {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Incorrect password"));
            }
        }

        String normalizedEmail = newEmail.trim().toLowerCase();
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("error", "An account with this email already exists"));
        }

        user.setEmail(normalizedEmail);
        userRepository.save(user);
        session.setAttribute("userEmail", user.getEmail());

        return ResponseEntity.ok(Map.of("message", "Email updated successfully", "email", user.getEmail()));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters long"));
        }

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PutMapping("/travel-preferences")
    public ResponseEntity<?> updateTravelPreferences(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        if (body.containsKey("preferredBudget")) {
            user.setPreferredBudget((String) body.get("preferredBudget"));
        }
        if (body.containsKey("travelStyle")) {
            user.setTravelStyle((String) body.get("travelStyle"));
        }
        if (body.containsKey("accommodation")) {
            user.setAccommodation((String) body.get("accommodation"));
        }
        if (body.containsKey("interests")) {
            Object interestsObj = body.get("interests");
            if (interestsObj instanceof java.util.List) {
                user.setInterests(String.join(", ", (java.util.List<String>) interestsObj));
            } else if (interestsObj instanceof String) {
                user.setInterests((String) interestsObj);
            }
        }
        if (body.containsKey("foodPreference")) {
            user.setFoodPreference((String) body.get("foodPreference"));
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Travel preferences saved successfully"));
    }

    @PutMapping("/notification-settings")
    public ResponseEntity<?> updateNotificationSettings(@RequestBody Map<String, Boolean> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        if (body.containsKey("tripReminders")) {
            user.setTripReminders(body.get("tripReminders"));
        }
        if (body.containsKey("travelRecommendations")) {
            user.setTravelRecommendations(body.get("travelRecommendations"));
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Notification preferences saved"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@RequestBody(required = false) Map<String, String> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String password = body != null ? body.get("password") : null;
        if (password != null && !password.isBlank()) {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Incorrect password"));
            }
        }

        try {
            userRepository.delete(user);
            session.invalidate();
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete user account: {}", user.getId(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete account: " + e.getMessage()));
        }
    }
}
