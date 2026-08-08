package com.intellitrip.dto;

import java.util.List;

public class ItineraryData {
    private String title;
    private String overview;
    private double totalBudget;
    private double dailyBudget;
    private List<DayData> days;
    private String destination;
    private String duration;
    private String travelers;
    private String budgetTier;
    private String destinationCurrencyCode;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }
    public double getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(double dailyBudget) { this.dailyBudget = dailyBudget; }
    public List<DayData> getDays() { return days; }
    public void setDays(List<DayData> days) { this.days = days; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getTravelers() { return travelers; }
    public void setTravelers(String travelers) { this.travelers = travelers; }
    public String getBudgetTier() { return budgetTier; }
    public void setBudgetTier(String budgetTier) { this.budgetTier = budgetTier; }
    public String getDestinationCurrencyCode() { return destinationCurrencyCode; }
    public void setDestinationCurrencyCode(String destinationCurrencyCode) { this.destinationCurrencyCode = destinationCurrencyCode; }

    public static class DayData {
        private int day;
        private String title;
        private String theme;
        private String dayTitle;
        private List<ActivityData> activities;

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        public String getDayTitle() { return dayTitle; }
        public void setDayTitle(String dayTitle) { this.dayTitle = dayTitle; }
        public List<ActivityData> getActivities() { return activities; }
        public void setActivities(List<ActivityData> activities) { this.activities = activities; }
    }

    public static class ActivityData {
        private String time;
        private String activity;
        private String description;
        private double estimatedCost;
        private String timeOfDay;
        private String timeSlot;
        private String locationName;
        private String transitInfo;

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getActivity() { return activity; }
        public void setActivity(String activity) { this.activity = activity; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
        public String getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public String getLocationName() { return locationName; }
        public void setLocationName(String locationName) { this.locationName = locationName; }
        public String getTransitInfo() { return transitInfo; }
        public void setTransitInfo(String transitInfo) { this.transitInfo = transitInfo; }
    }
}
