package com.intellitrip.repository;

import com.intellitrip.model.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsRepository extends JpaRepository<Analytics, String> {
}
