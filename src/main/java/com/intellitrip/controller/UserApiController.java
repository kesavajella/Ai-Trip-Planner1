package com.intellitrip.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import com.intellitrip.service.AuthService;
import com.intellitrip.service.CurrencyService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    private static final Logger log = LoggerFactory.getLogger(UserApiController.class);

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CurrencyService currencyService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserApiController(UserRepository userRepository, AuthService authService, CurrencyService currencyService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.currencyService = currencyService;
        this.passwordEncoder = passwordEncoder;
    }

    private User getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @GetMapping("/countries")
    public ResponseEntity<?> getCountries() {
        try (InputStream in = new ClassPathResource("static/data/countries.json").getInputStream()) {
            List<Map<String, Object>> countries = objectMapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {});
            return ResponseEntity.ok(Map.of("countries", countries));
        } catch (Exception e) {
            log.error("Failed to load countries", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load countries"));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("name", user.getName());
        data.put("country", user.getCountry());
        data.put("countryCode", user.getCountryCode());
        data.put("email", user.getEmail());
        data.put("preferredBudget", user.getPreferredBudget());
        data.put("travelStyle", user.getTravelStyle());
        data.put("accommodation", user.getAccommodation());
        data.put("interests", user.getInterests());
        data.put("foodPreference", user.getFoodPreference());
        data.put("tripReminders", user.getTripReminders());
        data.put("travelRecommendations", user.getTravelRecommendations());
        return ResponseEntity.ok(data);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String name = body.get("name");
        String country = body.get("country");
        String countryCode = body.get("countryCode");

        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        if (country != null) {
            user.setCountry(country.isBlank() ? null : country.trim());
        }
        if (countryCode != null) {
            user.setCountryCode(countryCode.isBlank() ? null : countryCode.trim().toUpperCase());
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String newEmail = body.get("email");
        String password = body.get("password");

        if (newEmail == null || newEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required for confirmation"));
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        String normalized = newEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            return ResponseEntity.badRequest().body(Map.of("error", "An account with this email already exists"));
        }

        user.setEmail(normalized);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Email updated successfully"));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (currentPassword == null || newPassword == null || confirmPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All password fields are required"));
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "New passwords do not match"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters"));
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PutMapping("/travel-preferences")
    public ResponseEntity<?> updateTravelPreferences(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
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
        if (body.containsKey("foodPreference")) {
            user.setFoodPreference((String) body.get("foodPreference"));
        }
        if (body.containsKey("interests")) {
            Object interests = body.get("interests");
            if (interests instanceof List) {
                user.setInterests(String.join(", ", (List<String>) interests));
            } else if (interests instanceof String) {
                user.setInterests((String) interests);
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Travel preferences saved"));
    }

    @PutMapping("/notification-settings")
    public ResponseEntity<?> updateNotificationSettings(@RequestBody Map<String, Boolean> body, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (body.containsKey("tripReminders")) {
            user.setTripReminders(body.get("tripReminders"));
        }
        if (body.containsKey("travelRecommendations")) {
            user.setTravelRecommendations(body.get("travelRecommendations"));
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Notification settings updated"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String password = body.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required to delete account"));
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is incorrect"));
        }

        userRepository.delete(user);
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }
}