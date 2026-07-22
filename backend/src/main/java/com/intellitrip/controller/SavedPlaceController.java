package com.intellitrip.controller;

import com.intellitrip.model.SavedPlace;
import com.intellitrip.repository.SavedPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/saved-places")
public class SavedPlaceController {
    @Autowired
    private SavedPlaceRepository savedPlaceRepository;

    @GetMapping
    public ResponseEntity<?> getSavedPlaces(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<SavedPlace> places = savedPlaceRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> response = places.stream().map(p -> java.util.Map.<String, Object>of(
                "id", p.getId(),
                "name", p.getName(),
                "location", p.getLocation(),
                "rating", p.getRating(),
                "price", p.getPrice(),
                "imageUrl", p.getImageUrl(),
                "category", p.getCategory(),
                "description", p.getDescription()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
