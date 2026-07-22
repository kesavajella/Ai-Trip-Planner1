package com.intellitrip.service;

import com.intellitrip.model.Analytics;
import com.intellitrip.repository.AnalyticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalyticsService {
    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics() {
        Optional<Analytics> analyticsOpt = analyticsRepository.findAll().stream().findFirst();
        Analytics analytics = analyticsOpt.orElse(null);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", analytics != null ? analytics.getTotalUsers() : 12480L);
        result.put("totalTrips", analytics != null ? analytics.getTotalTrips() : 38291L);
        result.put("generatedThisWeek", analytics != null ? analytics.getTotalGenerations() : 1842L);
        result.put("avgTripDays", analytics != null ? analytics.getAvgTripDays() : 6.4);

        List<Map<String, Object>> weeklySignups = Arrays.asList(
                Map.of("day", "Mon", "users", 180),
                Map.of("day", "Tue", "users", 220),
                Map.of("day", "Wed", "users", 280),
                Map.of("day", "Thu", "users", 240),
                Map.of("day", "Fri", "users", 360),
                Map.of("day", "Sat", "users", 410),
                Map.of("day", "Sun", "users", 320)
        );
        result.put("weeklySignups", weeklySignups);

        List<Map<String, Object>> tripsByType = Arrays.asList(
                Map.of("type", "Solo", "count", 9820),
                Map.of("type", "Couple", "count", 14210),
                Map.of("type", "Family", "count", 8900),
                Map.of("type", "Friends", "count", 5361)
        );
        result.put("tripsByType", tripsByType);

        return result;
    }
}
