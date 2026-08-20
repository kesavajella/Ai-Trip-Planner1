package com.intellitrip.dto;

public class TripPlanRequest {
    private String city;
    private int numberOfDays;
    private String budget;
    private String travelers;
    private String[] interests;
    private String accommodationPreference;
    private String countryCode;
    private String countryName;

    public TripPlanRequest() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public int getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(int numberOfDays) { this.numberOfDays = numberOfDays; }
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

