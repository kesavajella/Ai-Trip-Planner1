package com.intellitrip.service;

import com.intellitrip.dto.ItineraryResponse;
import com.intellitrip.model.Trip;
import com.intellitrip.model.User;
import com.intellitrip.repository.TripRepository;
import com.intellitrip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {
    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItineraryService itineraryService;

    @Transactional(readOnly = true)
    public List<Trip> getTripsByUser(String userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Trip> getTripsByStatus(String userId, Trip.TripStatus status) {
        return tripRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    @Transactional
    public Trip saveGeneratedTrip(String userId, ItineraryResponse itinerary) {
        User user = userRepository.findById(userId).orElseThrow();
        Trip trip = Trip.builder()
                .user(user)
                .destination(itinerary.getTitle())
                .country("")
                .days(itinerary.getDays() != null ? itinerary.getDays().size() : 1)
                .budget(Trip.BudgetTier.MEDIUM)
                .budgetUsd(itinerary.getTotalBudget() != null ? itinerary.getTotalBudget() : BigDecimal.ZERO)
                .travelType(Trip.TravelType.SOLO)
                .status(Trip.TripStatus.UPCOMING)
                .startDate(LocalDate.now())
                .build();
        return tripRepository.save(trip);
    }

    @Transactional
    public Trip updateTripStatus(String tripId, Trip.TripStatus status) {
        Trip trip = tripRepository.findById(tripId).orElseThrow();
        trip.setStatus(status);
        return tripRepository.save(trip);
    }

    @Transactional
    public void deleteTrip(String tripId) {
        tripRepository.deleteById(tripId);
    }

    public Optional<Trip> findById(String tripId) {
        return tripRepository.findById(tripId);
    }

    public long countTrips(String userId) {
        return tripRepository.countByUserId(userId);
    }
}
