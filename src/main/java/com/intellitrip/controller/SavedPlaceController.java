package com.intellitrip.controller;

import com.intellitrip.model.SavedPlace;
import com.intellitrip.model.User;
import com.intellitrip.repository.SavedPlaceRepository;
import com.intellitrip.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/saved-places")
public class SavedPlaceController {

    private final SavedPlaceRepository savedPlaceRepository;
    private final UserRepository userRepository;

    public SavedPlaceController(SavedPlaceRepository savedPlaceRepository, UserRepository userRepository) {
        this.savedPlaceRepository = savedPlaceRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<SavedPlace> getSavedPlaces(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return List.of();
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        return savedPlaceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @PostMapping
    public ResponseEntity<?> savePlace(@RequestBody Map<String, Object> body, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        SavedPlace place = new SavedPlace();
        place.setUser(userOpt.get());
        place.setName((String) body.getOrDefault("name", ""));
        place.setLocation((String) body.getOrDefault("location", ""));
        place.setRating(body.containsKey("rating") ? ((Number) body.get("rating")).doubleValue() : 0.0);
        place.setPrice((String) body.getOrDefault("price", ""));
        place.setImage((String) body.getOrDefault("image", ""));
        place.setCategory((String) body.getOrDefault("category", ""));
        place.setDescription((String) body.getOrDefault("description", ""));

        SavedPlace saved = savedPlaceRepository.save(place);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlace(@PathVariable String id, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        savedPlaceRepository.findById(id).ifPresent(place -> {
            if (place.getUser() != null && place.getUser().getId().equals(userId)) {
                savedPlaceRepository.delete(place);
            }
        });
        return ResponseEntity.ok(Map.of("message", "Place deleted"));
    }
}