package com.intellitrip.dto;

public class TripPlanRequest {
    private String city;
    private String source;
    private int numberOfDays;
    private String startDate;
    private String budget;
    private String travelers;
    private String[] interests;
    private String accommodationPreference;
    private String countryCode;
    private String countryName;

    public TripPlanRequest() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public int getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(int numberOfDays) { this.numberOfDays = numberOfDays; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public String getTravelers() { return travelers; }
    public void setTravelers(String travelers) { this.travelers = travelers; }
    public String[] getInterests() { return interests; }
    public void setInterests(String[] interests) { this.interests = interests; }
    public String getAccommodationPreference() { return accommodationPreference; }
    public void setAccommodationPreference(String accommodationPreference) { this.accommodationPreference = accommodationPreference; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
}

