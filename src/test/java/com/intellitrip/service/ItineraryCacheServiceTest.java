package com.intellitrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.dto.ItineraryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ItineraryCacheServiceTest {

    private ItineraryCacheService cacheService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cacheService = new ItineraryCacheService(objectMapper);
    }

    @Test
    void testBuildKeyNormalization() {
        String key1 = cacheService.buildKey("Tokyo, Japan", 5, "Moderate", "Solo");
        String key2 = cacheService.buildKey("  tokyo, japan  ", 5, "moderate", "solo  ");

        assertEquals(key1, key2);
        assertEquals("tokyo, japan|5|moderate|solo", key1);
    }

    @Test
    void testPutAndGetCacheHit() {
        ItineraryData sample = new ItineraryData();
        sample.setTitle("5-Day Tokyo Trip");
        sample.setDestination("Tokyo, Japan");
        sample.setTotalBudget(1500.0);
        sample.setDays(new ArrayList<>());

        cacheService.put("Tokyo, Japan", 5, "Moderate", "Solo", sample);

        Optional<ItineraryData> cached = cacheService.get("tokyo, japan", 5, "moderate", "solo");
        assertTrue(cached.isPresent());
        assertEquals("5-Day Tokyo Trip", cached.get().getTitle());
        assertEquals(1500.0, cached.get().getTotalBudget());
    }

    @Test
    void testCacheMissOnUnknownKey() {
        Optional<ItineraryData> cached = cacheService.get("Paris, France", 3, "Luxury", "Couple");
        assertTrue(cached.isEmpty());
    }

    @Test
    void testDeepCopyIsolation() {
        ItineraryData sample = new ItineraryData();
        sample.setTitle("Original Title");
        sample.setTotalBudget(2000.0);

        cacheService.put("Rome, Italy", 4, "Budget", "Family", sample);

        Optional<ItineraryData> firstRetrieve = cacheService.get("Rome, Italy", 4, "Budget", "Family");
        assertTrue(firstRetrieve.isPresent());
        // Mutate first retrieved object
        firstRetrieve.get().setTitle("Mutated Title");
        firstRetrieve.get().setTotalBudget(9999.0);

        // Second retrieve should still have original values
        Optional<ItineraryData> secondRetrieve = cacheService.get("Rome, Italy", 4, "Budget", "Family");
        assertTrue(secondRetrieve.isPresent());
        assertEquals("Original Title", secondRetrieve.get().getTitle());
        assertEquals(2000.0, secondRetrieve.get().getTotalBudget());
    }

    @Test
    void testClear() {
        ItineraryData sample = new ItineraryData();
        sample.setTitle("Kyoto Trip");
        cacheService.put("Kyoto, Japan", 3, "Moderate", "Solo", sample);

        assertEquals(1, cacheService.size());
        cacheService.clear();
        assertEquals(0, cacheService.size());
        assertTrue(cacheService.get("Kyoto, Japan", 3, "Moderate", "Solo").isEmpty());
    }
}

