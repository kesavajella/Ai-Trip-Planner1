package com.intellitrip.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String destination;

    private String country;

    private String image;

    @Column(nullable = false)
    private int days;

    private String budget;

    private double budgetUsd;

    private String travelType;

    private String interests;

    private String accommodationPreference;

    @Column(length = 10)
    private String destinationCurrencyCode;

    @Lob
    @Column(nullable = false)
    private String itineraryJson = "";

private String status = "draft";

    @Column(name = "saved", nullable = false)
    private boolean saved = false;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Trip() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public double getBudgetUsd() { return budgetUsd; }
    public void setBudgetUsd(double budgetUsd) { this.budgetUsd = budgetUsd; }
    public String getTravelType() { return travelType; }
    public void setTravelType(String travelType) { this.travelType = travelType; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getAccommodationPreference() { return accommodationPreference; }
    public void setAccommodationPreference(String accommodationPreference) { this.accommodationPreference = accommodationPreference; }
    public String getDestinationCurrencyCode() { return destinationCurrencyCode; }
    public void setDestinationCurrencyCode(String destinationCurrencyCode) { this.destinationCurrencyCode = destinationCurrencyCode; }
    public String getItineraryJson() { return itineraryJson; }
    public void setItineraryJson(String itineraryJson) { this.itineraryJson = itineraryJson; }
public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

