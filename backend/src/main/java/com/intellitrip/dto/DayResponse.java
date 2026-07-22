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
public class DayResponse {
    private Integer day;
    private String title;
    private String theme;
    private List<ActivityResponse> activities;
}
