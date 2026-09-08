package com.intellitrip.repository;

import com.intellitrip.model.SavedPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPlaceRepository extends JpaRepository<SavedPlace, String> {
    List<SavedPlace> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<SavedPlace> findByTripId(String tripId);
    List<SavedPlace> findByUserIdAndTripIdIsNotNullOrderByCreatedAtDesc(String userId);
    void deleteByTripId(String tripId);
}

