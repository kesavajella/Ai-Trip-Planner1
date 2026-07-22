package com.intellitrip.controller;

import com.intellitrip.dto.GenerateTripRequest;
import com.intellitrip.dto.ItineraryResponse;
import com.intellitrip.dto.TripResponse;
import com.intellitrip.model.Trip;
import com.intellitrip.service.ItineraryService;
import com.intellitrip.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TripController {
    @Autowired
    private TripService tripService;

    @Autowired
    private ItineraryService itineraryService;

    @GetMapping("/trips")
    public ResponseEntity<?> getTrips(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<Trip> trips = tripService.getTripsByUser(userId);
        List<TripResponse> responses = trips.stream().map(t -> TripResponse.builder()
                .id(t.getId())
                .destination(t.getDestination())
                .country(t.getCountry())
                .image(t.getImageUrl())
                .days(t.getDays())
                .budget(t.getBudget().name().toLowerCase())
                .budgetUsd(t.getBudgetUsd())
                .travelType(t.getTravelType().name().toLowerCase())
                .interests(List.of())
                .startDate(t.getStartDate() != null ? t.getStartDate().toString() : null)
                .status(t.getStatus().name().toLowerCase())
                .createdAt(t.getCreatedAt().toString())
                .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/trips")
    public ResponseEntity<?> createTrip(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                        @RequestBody ItineraryResponse itinerary) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        Trip saved = tripService.saveGeneratedTrip(userId, itinerary);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", saved.getStatus().name().toLowerCase()));
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<?> getTrip(@PathVariable String tripId) {
        return tripService.findById(tripId)
                .map(t -> ResponseEntity.ok(Map.of(
                        "id", t.getId(),
                        "destination", t.getDestination(),
                        "country", t.getCountry(),
                        "image", t.getImageUrl(),
                        "days", t.getDays(),
                        "budget", t.getBudget().name().toLowerCase(),
                        "budgetUsd", t.getBudgetUsd(),
                        "travelType", t.getTravelType().name().toLowerCase(),
                        "startDate", t.getStartDate() != null ? t.getStartDate().toString() : "",
                        "status", t.getStatus().name().toLowerCase()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate-trip")
    public ResponseEntity<?> generateTrip(@RequestBody GenerateTripRequest request) {
        try {
            String destination = request.getCity() != null ? request.getCity() : "Unknown";
            String budget = request.getBudget() != null ? request.getBudget() : "medium";
            Integer days = request.getNumberOfDays() != null ? request.getNumberOfDays() : 5;
            java.util.List<String> interests = request.getInterests() != null ? request.getInterests() : java.util.List.of("culture", "food");

            if (destination == null || destination.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Destination is required"));
            }

            ItineraryResponse itinerary = itineraryService.generateItinerary(destination, budget, days, interests);
            return ResponseEntity.ok(itinerary);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate trip: " + e.getMessage()));
        }
    }
}
