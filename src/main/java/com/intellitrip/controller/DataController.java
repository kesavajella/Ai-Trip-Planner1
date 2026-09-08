package com.intellitrip.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private static final Logger log = LoggerFactory.getLogger(DataController.class);
    private final ObjectMapper objectMapper;
    private final LocationService locationService;

    private List<Map<String, Object>> citiesCache = null;
    private List<Map<String, Object>> countriesCache = null;

    public DataController(ObjectMapper objectMapper, LocationService locationService) {
        this.objectMapper = objectMapper;
        this.locationService = locationService;
    }

    /**
     * GET /api/data/cities?search=paris&country=france&limit=10&page=1
     * Search and filter cities with pagination
     */
    @GetMapping("/cities")
    public ResponseEntity<?> getCities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "1") int page) {

        try {
            List<Map<String, Object>> cities = loadCities();
            List<Map<String, Object>> filtered = cities;

            // Filter by search query
            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase().trim();
                filtered = filtered.stream()
                    .filter(c -> {
                        String name = (String) c.getOrDefault("name", "");
                        String ctry = (String) c.getOrDefault("country", "");
                        String code = (String) c.getOrDefault("countryCode", "");
                        @SuppressWarnings("unchecked")
                        List<String> places = (List<String>) c.getOrDefault("famousPlaces", List.of());
                        @SuppressWarnings("unchecked")
                        List<String> foods = (List<String>) c.getOrDefault("localFoods", List.of());

                        boolean matchName = name.toLowerCase().contains(q);
                        boolean matchCountry = ctry.toLowerCase().contains(q);
                        boolean matchCode = code.toLowerCase().contains(q);
                        boolean matchPlace = places.stream().anyMatch(p -> p.toLowerCase().contains(q));
                        boolean matchFood = foods.stream().anyMatch(f -> f.toLowerCase().contains(q));

                        return matchName || matchCountry || matchCode || matchPlace || matchFood;
                    })
                    .collect(Collectors.toList());
            }

            // Filter by country
            if (country != null && !country.isBlank()) {
                String q = country.toLowerCase().trim();
                filtered = filtered.stream()
                    .filter(c -> {
                        String ctry = (String) c.getOrDefault("country", "");
                        String code = (String) c.getOrDefault("countryCode", "");
                        return ctry.toLowerCase().contains(q) || code.toLowerCase().contains(q);
                    })
                    .collect(Collectors.toList());
            }

            // Pagination
            int totalCount = filtered.size();
            int totalPages = (int) Math.ceil((double) totalCount / limit);
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, totalCount);

            List<Map<String, Object>> paginated;
            if (startIndex < totalCount) {
                paginated = filtered.subList(startIndex, endIndex);
            } else {
                paginated = List.of();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("cities", paginated);
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("totalCount", totalCount);
            pagination.put("totalPages", totalPages);
            response.put("pagination", pagination);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load cities data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to load cities data: " + e.getMessage()));
        }
    }

    /**
     * GET /api/data/city-details?city=Paris&country=France
     * Get detailed city info including famous places and local foods
     */
    @GetMapping("/city-details")
    public ResponseEntity<?> getCityDetails(
            @RequestParam String city,
            @RequestParam(required = false) String country) {

        try {
            if (city == null || city.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "City name is required (e.g., ?city=Paris)"));
            }

            List<Map<String, Object>> cities = loadCities();
            String query = city.toLowerCase().trim();

            // Try to find exact match
            Map<String, Object> cityData = cities.stream()
                .filter(c -> {
                    String name = (String) c.getOrDefault("name", "");
                    return name.toLowerCase().equals(query) || name.toLowerCase().contains(query) || query.contains(name.toLowerCase());
                })
                .findFirst()
                .orElse(null);

            // If no exact match, try with country
            if (cityData == null && country != null && !country.isBlank()) {
                String countryQuery = country.toLowerCase().trim();
                cityData = cities.stream()
                    .filter(c -> {
                        String name = (String) c.getOrDefault("name", "");
                        String ctry = (String) c.getOrDefault("country", "");
                        String code = (String) c.getOrDefault("countryCode", "");
                        return (name.toLowerCase().contains(query) || query.contains(name.toLowerCase())) &&
                               (ctry.toLowerCase().contains(countryQuery) || code.toLowerCase().contains(countryQuery));
                    })
                    .findFirst()
                    .orElse(null);
            }

            // Broader search
            if (cityData == null) {
                cityData = cities.stream()
                    .filter(c -> {
                        String name = (String) c.getOrDefault("name", "");
                        String ctry = (String) c.getOrDefault("country", "");
                        return name.toLowerCase().contains(query) || ctry.toLowerCase().contains(query);
                    })
                    .findFirst()
                    .orElse(null);
            }

            if (cityData == null) {
                return ResponseEntity.ok(Map.of(
                    "found", false,
                    "city", city,
                    "country", country != null ? country : "",
                    "countryCode", "",
                    "coordinates", null,
                    "population", "unknown",
                    "timezone", "",
                    "famousPlaces", List.of(),
                    "localFoods", List.of()
                ));
            }

            // Get country info
            Map<String, Object> countryInfo = null;
            try {
                String countryCode = (String) cityData.getOrDefault("countryCode", "");
                List<Map<String, Object>> countries = loadCountries();
                String finalCountryCode = countryCode;
                Map<String, Object> finalCityData = cityData;
                countryInfo = countries.stream()
                    .filter(c -> {
                        String alpha2 = (String) c.getOrDefault("alpha2", "");
                        String name = (String) c.getOrDefault("name", "");
                        return alpha2.equals(finalCountryCode) || name.equals(finalCityData.get("country"));
                    })
                    .findFirst()
                    .orElse(null);
            } catch (Exception e) {
                // Non-fatal
            }

            Map<String, Object> response = new HashMap<>();
            response.put("found", true);
            response.put("city", cityData.get("name"));
            response.put("country", cityData.get("country"));
            response.put("countryCode", cityData.get("countryCode"));
            response.put("coordinates", cityData.get("coordinates"));
            response.put("population", cityData.getOrDefault("population", "unknown"));
            response.put("timezone", cityData.getOrDefault("timezone", ""));
            response.put("famousPlaces", cityData.getOrDefault("famousPlaces", List.of()));
            response.put("localFoods", cityData.getOrDefault("localFoods", List.of()));

            if (countryInfo != null) {
                Map<String, Object> info = new HashMap<>();
                info.put("name", countryInfo.get("name"));
                info.put("officialName", countryInfo.get("officialName"));
                info.put("capital", countryInfo.get("capital"));
                info.put("region", countryInfo.get("region"));
                info.put("subregion", countryInfo.get("subregion"));
                info.put("languages", countryInfo.get("languages"));
                info.put("coordinates", countryInfo.get("coordinates"));

                Object currencyObj = countryInfo.get("currency");
                if (currencyObj instanceof Map) {
                    Map<String, Object> currencyMap = (Map<String, Object>) currencyObj;
                    info.put("currencyCode", currencyMap.get("code"));
                    info.put("currencyName", currencyMap.get("name"));
                    info.put("currencySymbol", currencyMap.get("symbol"));
                    info.put("currency", currencyMap.get("code"));
                } else {
                    info.put("currency", currencyObj);
                    info.put("currencyCode", currencyObj);
                    info.put("currencyName", currencyObj);
                    info.put("currencySymbol", currencyObj);
                }

                response.put("countryInfo", info);
            } else {
                response.put("countryInfo", null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load city details", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to load city details: " + e.getMessage()));
        }
    }

    /**
     * GET /api/data/countries?search=france&limit=10
     * Search countries
     */
    @GetMapping("/countries")
    public ResponseEntity<?> getCountries(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "50") int limit) {

        try {
            List<Map<String, Object>> countries = loadCountries();
            List<Map<String, Object>> filtered = countries;

            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase().trim();
                filtered = filtered.stream()
                    .filter(c -> {
                        String name = (String) c.getOrDefault("name", "");
                        String official = (String) c.getOrDefault("officialName", "");
                        String alpha2 = (String) c.getOrDefault("alpha2", "");
                        String alpha3 = (String) c.getOrDefault("alpha3", "");
                        String capital = (String) c.getOrDefault("capital", "");
                        return name.toLowerCase().contains(q) ||
                               official.toLowerCase().contains(q) ||
                               alpha2.toLowerCase().contains(q) ||
                               alpha3.toLowerCase().contains(q) ||
                               capital.toLowerCase().contains(q);
                    })
                    .collect(Collectors.toList());
            }

            if (filtered.size() > limit) {
                filtered = filtered.subList(0, limit);
            }

            return ResponseEntity.ok(Map.of("countries", filtered, "count", filtered.size()));
        } catch (Exception e) {
            log.error("Failed to load countries data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to load countries data: " + e.getMessage()));
        }
    }

    /**
     * GET /api/data/autocomplete-index
     * Returns the full lightweight flat index for client-side autocomplete.
     */
    @GetMapping("/autocomplete-index")
    public ResponseEntity<?> getAutocompleteIndex() {
        try {
            List<Map<String, Object>> index = locationService.getIndex();
            return ResponseEntity.ok()
                    .header("Cache-Control", "public, max-age=86400")
                    .body(index);
        } catch (Exception e) {
            log.error("Failed to load autocomplete index", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load autocomplete index: " + e.getMessage()));
        }
    }

    /**
     * GET /api/data/autocomplete?q=lon&limit=10
     * Live autocomplete across countries, states, and cities.
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<?> getAutocomplete(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<Map<String, Object>> results = locationService.search(q, limit);
            return ResponseEntity.ok(Map.of("results", results, "count", results.size()));
        } catch (Exception e) {
            log.error("Failed to search locations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to search locations: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadCities() throws Exception {
        if (citiesCache == null) {
            InputStream input = new ClassPathResource("static/data/cities.json").getInputStream();
            citiesCache = objectMapper.readValue(input, List.class);
        }
        return citiesCache;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadCountries() throws Exception {
        if (countriesCache == null) {
            InputStream input = new ClassPathResource("static/data/countries.json").getInputStream();
            countriesCache = objectMapper.readValue(input, List.class);
        }
        return countriesCache;
    }
}
