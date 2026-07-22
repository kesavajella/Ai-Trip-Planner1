package com.intellitrip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itinerary_days")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDay {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String theme;

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryItem> items = new ArrayList<>();

    @Column(columnDefinition = "JSON")
    private String accommodations;

    @Column(columnDefinition = "JSON")
    private String transportation;

    @Column(columnDefinition = "JSON")
    private String costBreakdown;

    @Column(columnDefinition = "TEXT")
    private String tips;
}
