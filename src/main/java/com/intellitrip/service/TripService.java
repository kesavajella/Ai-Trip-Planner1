package com.intellitrip.service;

import com.intellitrip.model.Trip;
import com.intellitrip.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;

    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

public List<Trip> getUserTrips(String userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Trip> getSavedTrips(String userId) {
        return tripRepository.findByUserIdAndSavedTrueOrderByCreatedAtDesc(userId);
    }

    public Optional<Trip> getTripById(String id) {
        return tripRepository.findById(id);
    }

    @Transactional
    public Trip saveTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    public void deleteTrip(String id) {
        tripRepository.deleteById(id);
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAllByOrderByCreatedAtDesc();
    }

    public long getTotalTrips() {
        return tripRepository.count();
    }
}
