package com.intellitrip.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "country")
    private String country;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "preferred_budget")
    private String preferredBudget;

    @Column(name = "travel_style")
    private String travelStyle;

    @Column(name = "accommodation")
    private String accommodation;

    @Column(name = "interests")
    private String interests;

    @Column(name = "food_preference")
    private String foodPreference;

    @Column(name = "trip_reminders")
    private Boolean tripReminders = true;

    @Column(name = "travel_recommendations")
    private Boolean travelRecommendations = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (tripReminders == null) tripReminders = true;
        if (travelRecommendations == null) travelRecommendations = true;
    }

    public User() {}

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getPreferredBudget() { return preferredBudget; }
    public void setPreferredBudget(String preferredBudget) { this.preferredBudget = preferredBudget; }
    public String getTravelStyle() { return travelStyle; }
    public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }
    public String getAccommodation() { return accommodation; }
    public void setAccommodation(String accommodation) { this.accommodation = accommodation; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getFoodPreference() { return foodPreference; }
    public void setFoodPreference(String foodPreference) { this.foodPreference = foodPreference; }
    public Boolean getTripReminders() { return tripReminders != null ? tripReminders : true; }
    public void setTripReminders(Boolean tripReminders) { this.tripReminders = tripReminders; }
    public Boolean getTravelRecommendations() { return travelRecommendations != null ? travelRecommendations : true; }
    public void setTravelRecommendations(Boolean travelRecommendations) { this.travelRecommendations = travelRecommendations; }
}
