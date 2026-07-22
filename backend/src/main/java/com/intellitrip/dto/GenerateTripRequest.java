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
public class GenerateTripRequest {
    private String city;
    private Integer numberOfDays;
    private String budget;
    private String travelers;
    private List<String> interests;
}
