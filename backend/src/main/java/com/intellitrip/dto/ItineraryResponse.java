package com.intellitrip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryResponse {
    private String title;
    private String overview;
    private BigDecimal totalBudget;
    private BigDecimal dailyBudget;
    private List<DayResponse> days;
    private List<AccommodationResponse> accommodation;
    private List<TransportResponse> transportation;
    private CostBreakdownResponse costBreakdown;
    private List<String> tips;
}
