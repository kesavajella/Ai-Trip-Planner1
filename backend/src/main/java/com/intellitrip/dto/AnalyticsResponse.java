package com.intellitrip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private Long totalUsers;
    private Long totalTrips;
    private Long generatedThisWeek;
    private Double avgTripDays;
    private List<Map<String, Object>> weeklySignups;
    private List<Map<String, Object>> tripsByType;
}
