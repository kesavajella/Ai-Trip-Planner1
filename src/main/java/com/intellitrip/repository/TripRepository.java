package com.intellitrip.repository;

import com.intellitrip.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {
List<Trip> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Trip> findByUserIdAndSavedTrueOrderByCreatedAtDesc(String userId);
    List<Trip> findByStatus(String status);
    List<Trip> findAllByOrderByCreatedAtDesc();
    long count();
}

