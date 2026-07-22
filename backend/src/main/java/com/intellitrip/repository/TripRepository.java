package com.intellitrip.repository;

import com.intellitrip.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, String> {
    List<Trip> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Trip> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Trip.TripStatus status);
    long countByUserId(String userId);
}
