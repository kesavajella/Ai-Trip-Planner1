package com.intellitrip.dto;

import com.intellitrip.model.Trip;
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
public class TripResponse {
    private String id;
    private String destination;
    private String country;
    private String image;
    private Integer days;
    private String budget;
    private BigDecimal budgetUsd;
    private String travelType;
    private List<String> interests;
    private String startDate;
    private String status;
    private String createdAt;
}
