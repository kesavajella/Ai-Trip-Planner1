package com.intellitrip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TripPlanResponse {
    private String destination;
    private String duration;
    private String travelers;
    private String budgetTier;
    private String estimatedTotalCost;
    private String tripSummary;
    private List<DayItinerary> itinerary;
    private List<DayItinerary> days;

    public TripPlanResponse() {}

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getTravelers() { return travelers; }
    public void setTravelers(String travelers) { this.travelers = travelers; }
    public String getBudgetTier() { return budgetTier; }
    public void setBudgetTier(String budgetTier) { this.budgetTier = budgetTier; }
    public String getEstimatedTotalCost() { return estimatedTotalCost; }
    public void setEstimatedTotalCost(String estimatedTotalCost) { this.estimatedTotalCost = estimatedTotalCost; }
    public String getTripSummary() { return tripSummary; }
    public void setTripSummary(String tripSummary) { this.tripSummary = tripSummary; }

    @JsonProperty("itinerary")
    public List<DayItinerary> getItinerary() { return itinerary; }
    @JsonProperty("itinerary")
    public void setItinerary(List<DayItinerary> itinerary) { this.itinerary = itinerary; }

    @JsonProperty("days")
    public List<DayItinerary> getDays() { return days; }
    @JsonProperty("days")
    public void setDays(List<DayItinerary> days) { this.days = days; }

    public static class DayItinerary {
        private int day;
        private String dayTitle;
        private List<ScheduleItem> schedule;

        public DayItinerary() {}
        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public String getDayTitle() { return dayTitle; }
        public void setDayTitle(String dayTitle) { this.dayTitle = dayTitle; }
        public List<ScheduleItem> getSchedule() { return schedule; }
        public void setSchedule(List<ScheduleItem> schedule) { this.schedule = schedule; }
    }

    public static class ScheduleItem {
        private String timeOfDay;
        private String timeSlot;
        private String activityTitle;
        private String description;
        private String locationName;
        private String estimatedCost;
        private String transitInfo;

        public ScheduleItem() {}
        public String getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public String getActivityTitle() { return activityTitle; }
        public void setActivityTitle(String activityTitle) { this.activityTitle = activityTitle; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLocationName() { return locationName; }
        public void setLocationName(String locationName) { this.locationName = locationName; }
        public String getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(String estimatedCost) { this.estimatedCost = estimatedCost; }
        public String getTransitInfo() { return transitInfo; }
        public void setTransitInfo(String transitInfo) { this.transitInfo = transitInfo; }
    }
}
