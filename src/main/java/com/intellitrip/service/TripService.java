package com.intellitrip.service;

import com.intellitrip.model.SavedPlace;
import com.intellitrip.model.Trip;
import com.intellitrip.repository.SavedPlaceRepository;
import com.intellitrip.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final SavedPlaceRepository savedPlaceRepository;

    public TripService(TripRepository tripRepository, SavedPlaceRepository savedPlaceRepository) {
        this.tripRepository = tripRepository;
        this.savedPlaceRepository = savedPlaceRepository;
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

    @Transactional
    public Trip saveTripToSavedPlaces(Trip trip) {
        if (trip == null || trip.getId() == null) {
            return trip;
        }
        SavedPlace place = savedPlaceRepository.findByTripId(trip.getId()).orElseGet(SavedPlace::new);
        place.setTripId(trip.getId());
        if (trip.getUser() != null) {
            place.setUser(trip.getUser());
        }
        place.setName(trip.getDestination());
        place.setLocation(trip.getCountry());
        place.setImage(trip.getImage());
        place.setCategory("Trip");
        place.setPrice(trip.getBudget());
        StringBuilder desc = new StringBuilder();
        desc.append(trip.getDays()).append(" days");
        if (trip.getBudget() != null && !trip.getBudget().isBlank()) {
            desc.append(" · ").append(trip.getBudget());
        }
        if (trip.getTravelType() != null && !trip.getTravelType().isBlank()) {
            desc.append(" · ").append(trip.getTravelType());
        }
        place.setDescription(desc.toString());
        savedPlaceRepository.save(place);
        return trip;
    }

    @Transactional
    public void deleteTrip(String id) {
        savedPlaceRepository.deleteByTripId(id);
        tripRepository.deleteById(id);
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAllByOrderByCreatedAtDesc();
    }

    public long getTotalTrips() {
        return tripRepository.count();
    }
}
