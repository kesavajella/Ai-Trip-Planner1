package com.intellitrip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String destination;

    @Column
    private String country;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private Integer days;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetTier budget;

    @Column(name = "budget_usd", precision = 12, scale = 2)
    private BigDecimal budgetUsd;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_type", nullable = false)
    private TravelType travelType;

    @Column(columnDefinition = "JSON")
    private String interests;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.DRAFT;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryDay> itineraryDays = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BudgetTier {
        LOW, MEDIUM, LUXURY
    }

    public enum TravelType {
        SOLO, FAMILY, FRIENDS, COUPLE
    }

    public enum TripStatus {
        UPCOMING, DRAFT, COMPLETED
    }
}
