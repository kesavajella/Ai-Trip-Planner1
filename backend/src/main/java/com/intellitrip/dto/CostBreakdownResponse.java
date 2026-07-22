package com.intellitrip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostBreakdownResponse {
    private BigDecimal accommodation;
    private BigDecimal food;
    private BigDecimal activities;
    private BigDecimal transportation;
    private BigDecimal other;
}
