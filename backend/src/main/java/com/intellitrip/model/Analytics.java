package com.intellitrip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analytics {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "total_users")
    private Long totalUsers;

    @Column(name = "total_trips")
    private Long totalTrips;

    @Column(name = "total_generations")
    private Long totalGenerations;

    @Column(name = "avg_trip_days")
    private Double avgTripDays;
}
