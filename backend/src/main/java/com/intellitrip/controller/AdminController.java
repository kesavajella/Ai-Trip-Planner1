package com.intellitrip.controller;

import com.intellitrip.dto.AnalyticsResponse;
import com.intellitrip.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        Map<String, Object> data = analyticsService.getAnalytics();
        AnalyticsResponse response = AnalyticsResponse.builder()
                .totalUsers(((Number) data.get("totalUsers")).longValue())
                .totalTrips(((Number) data.get("totalTrips")).longValue())
                .generatedThisWeek(((Number) data.get("generatedThisWeek")).longValue())
                .avgTripDays((Double) data.get("avgTripDays"))
                .weeklySignups((List<Map<String, Object>>) data.get("weeklySignups"))
                .tripsByType((List<Map<String, Object>>) data.get("tripsByType"))
                .build();
        return ResponseEntity.ok(response);
    }
}
