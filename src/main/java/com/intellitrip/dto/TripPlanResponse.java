package com.intellitrip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TripPlanResponse {
    private TripOverview tripOverview;
    private Transport transport;
    private List<Hotel> hotels;
    private List<DayItinerary> itinerary;
    private List<DayItinerary> days;
    private Budget budget;
    private EstimatedDailyCosts estimatedDailyCosts;

    private String destination;
    private String duration;
    private String travelers;
    private String budgetTier;
    private String tripSummary;

    public TripPlanResponse() {}

    public TripOverview getTripOverview() { return tripOverview; }
    public void setTripOverview(TripOverview tripOverview) { this.tripOverview = tripOverview; }
    public Transport getTransport() { return transport; }
    public void setTransport(Transport transport) { this.transport = transport; }
    public List<Hotel> getHotels() { return hotels; }
    public void setHotels(List<Hotel> hotels) { this.hotels = hotels; }

    @JsonProperty("itinerary")
    public List<DayItinerary> getItinerary() { return itinerary; }
    @JsonProperty("itinerary")
    public void setItinerary(List<DayItinerary> itinerary) { this.itinerary = itinerary; }

    @JsonProperty("days")
    public List<DayItinerary> getDays() { return days; }
    @JsonProperty("days")
    public void setDays(List<DayItinerary> days) { this.days = days; }

    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }
    public EstimatedDailyCosts getEstimatedDailyCosts() { return estimatedDailyCosts; }
    public void setEstimatedDailyCosts(EstimatedDailyCosts estimatedDailyCosts) { this.estimatedDailyCosts = estimatedDailyCosts; }

    public String getDestination() {
        if (tripOverview != null && tripOverview.getDestination() != null) return tripOverview.getDestination();
        return destination;
    }
    public void setDestination(String destination) { this.destination = destination; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getTravelers() { return travelers; }
    public void setTravelers(String travelers) { this.travelers = travelers; }
    public String getBudgetTier() { return budgetTier; }
    public void setBudgetTier(String budgetTier) { this.budgetTier = budgetTier; }
    public String getTripSummary() {
        if (tripOverview != null) return tripOverview.getSeasonalHighlights();
        return tripSummary;
    }
    public void setTripSummary(String tripSummary) { this.tripSummary = tripSummary; }

    public static class TripOverview {
        private String destination;
        private String source;
        private String startDate;
        private int totalDays;
        private String seasonalHighlights;

        public TripOverview() {}
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public int getTotalDays() { return totalDays; }
        public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
        public String getSeasonalHighlights() { return seasonalHighlights; }
        public void setSeasonalHighlights(String seasonalHighlights) { this.seasonalHighlights = seasonalHighlights; }
    }

    public static class Transport {
        private RoundTrip roundTrip;
        private List<TransportApp> localTransportApps;

        public Transport() {}
        public RoundTrip getRoundTrip() { return roundTrip; }
        public void setRoundTrip(RoundTrip roundTrip) { this.roundTrip = roundTrip; }
        public List<TransportApp> getLocalTransportApps() { return localTransportApps; }
        public void setLocalTransportApps(List<TransportApp> localTransportApps) { this.localTransportApps = localTransportApps; }
    }

    public static class RoundTrip {
        private String mode;
        private String route;
        private String duration;
        private double estimatedCost;
        private String recommendedBookingApp;

        public RoundTrip() {}
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getRoute() { return route; }
        public void setRoute(String route) { this.route = route; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public double getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
        public String getRecommendedBookingApp() { return recommendedBookingApp; }
        public void setRecommendedBookingApp(String recommendedBookingApp) { this.recommendedBookingApp = recommendedBookingApp; }
    }

    public static class TransportApp {
        private String name;
        private String purpose;

        public TransportApp() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
    }

    public static class Hotel {
        private String name;
        private String area;
        private double pricePerNight;
        private String bestFor;

        public Hotel() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
        public double getPricePerNight() { return pricePerNight; }
        public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
        public String getBestFor() { return bestFor; }
        public void setBestFor(String bestFor) { this.bestFor = bestFor; }
    }

    public static class DayItinerary {
        private int day;
        private String date;
        private String theme;
        private String dayTitle;
        private List<Activity> activities;
        private String localFoodSpot;
        private List<ScheduleItem> schedule;

        public DayItinerary() {}
        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTheme() { return theme != null ? theme : dayTitle; }
        public void setTheme(String theme) { this.theme = theme; }
        public String getDayTitle() { return dayTitle != null ? dayTitle : theme; }
        public void setDayTitle(String dayTitle) { this.dayTitle = dayTitle; }
        public List<Activity> getActivities() { return activities; }
        public void setActivities(List<Activity> activities) { this.activities = activities; }
        public String getLocalFoodSpot() { return localFoodSpot; }
        public void setLocalFoodSpot(String localFoodSpot) { this.localFoodSpot = localFoodSpot; }
        public List<ScheduleItem> getSchedule() { return schedule; }
        public void setSchedule(List<ScheduleItem> schedule) { this.schedule = schedule; }
    }

    public static class Activity {
        private String timeSlot;
        private String place;
        private String transitToPlace;
        private String description;

        public Activity() {}
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public String getPlace() { return place; }
        public void setPlace(String place) { this.place = place; }
        public String getTransitToPlace() { return transitToPlace; }
        public void setTransitToPlace(String transitToPlace) { this.transitToPlace = transitToPlace; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
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

    public static class Budget {
        private String currency;
        private double estimatedAccommodationTotal;
        private double estimatedFoodTotal;
        private double estimatedTransportTotal;
        private double estimatedInterestsTotal;
        private double estimatedOtherTotal;
        private double grandTotal;

        public Budget() {}
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public double getEstimatedAccommodationTotal() { return estimatedAccommodationTotal; }
        public void setEstimatedAccommodationTotal(double estimatedAccommodationTotal) { this.estimatedAccommodationTotal = estimatedAccommodationTotal; }
        public double getEstimatedFoodTotal() { return estimatedFoodTotal; }
        public void setEstimatedFoodTotal(double estimatedFoodTotal) { this.estimatedFoodTotal = estimatedFoodTotal; }
        public double getEstimatedTransportTotal() { return estimatedTransportTotal; }
        public void setEstimatedTransportTotal(double estimatedTransportTotal) { this.estimatedTransportTotal = estimatedTransportTotal; }
        public double getEstimatedInterestsTotal() { return estimatedInterestsTotal; }
        public void setEstimatedInterestsTotal(double estimatedInterestsTotal) { this.estimatedInterestsTotal = estimatedInterestsTotal; }
        public double getEstimatedOtherTotal() { return estimatedOtherTotal; }
        public void setEstimatedOtherTotal(double estimatedOtherTotal) { this.estimatedOtherTotal = estimatedOtherTotal; }
        public double getGrandTotal() { return grandTotal; }
        public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }
    }

    public static class EstimatedDailyCosts {
        private double accommodationPerNight;
        private double foodPerDay;
        private double interestsPerDay;

        public EstimatedDailyCosts() {}
        public double getAccommodationPerNight() { return accommodationPerNight; }
        public void setAccommodationPerNight(double accommodationPerNight) { this.accommodationPerNight = accommodationPerNight; }
        public double getFoodPerDay() { return foodPerDay; }
        public void setFoodPerDay(double foodPerDay) { this.foodPerDay = foodPerDay; }
        public double getInterestsPerDay() { return interestsPerDay; }
        public void setInterestsPerDay(double interestsPerDay) { this.interestsPerDay = interestsPerDay; }
    }
}
