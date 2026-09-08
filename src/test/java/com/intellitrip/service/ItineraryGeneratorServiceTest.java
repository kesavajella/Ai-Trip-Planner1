package com.intellitrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.dto.ItineraryData;
import com.intellitrip.dto.TripPlanRequest;
import com.intellitrip.dto.TripPlanResponse;
import com.intellitrip.exception.GeminiQuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ItineraryGeneratorServiceTest {

    private GeminiAiService geminiAiService;
    private ItineraryCacheService itineraryCacheService;
    private ObjectMapper objectMapper;
    private CurrencyService currencyService;
    private ItineraryGeneratorService itineraryGeneratorService;

    @BeforeEach
    void setUp() {
        geminiAiService = Mockito.mock(GeminiAiService.class);
        objectMapper = new ObjectMapper();
        itineraryCacheService = new ItineraryCacheService(objectMapper);
        currencyService = Mockito.mock(CurrencyService.class);
        itineraryGeneratorService = new ItineraryGeneratorService(geminiAiService, itineraryCacheService, objectMapper, currencyService);
    }

    @Test
    void testServesFromCacheWithoutCallingGemini() {
        TripPlanRequest request = new TripPlanRequest();
        request.setCity("Paris, France");
        request.setNumberOfDays(3);
        request.setBudget("Moderate");
        request.setTravelers("Just Me");

        ItineraryData preCached = new ItineraryData();
        preCached.setTitle("Cached 3-Day Paris Trip");
        preCached.setDestination("Paris, France");
        preCached.setDays(new ArrayList<>());

        itineraryCacheService.put("Paris, France", 3, "Moderate", "Just Me", preCached);

        ItineraryGeneratorService.ItineraryGenerationResult result = itineraryGeneratorService.generateItineraryWithRawResponse(request);

        assertNotNull(result);
        assertEquals("Cached 3-Day Paris Trip", result.itinerary().getTitle());
        assertNull(result.rawResponse());
        verify(geminiAiService, never()).generateTripPlan(anyString());
    }

    @Test
    void testPropagatesQuotaExceededExceptionWithoutRetryingPrompt() {
        TripPlanRequest request = new TripPlanRequest();
        request.setCity("London, UK");
        request.setNumberOfDays(4);
        request.setBudget("Luxury");
        request.setTravelers("A Couple");

        when(geminiAiService.generateTripPlan(anyString()))
                .thenThrow(new GeminiQuotaExceededException("Daily limit reached, please try again shortly"));

        GeminiQuotaExceededException ex = assertThrows(GeminiQuotaExceededException.class, () -> {
            itineraryGeneratorService.generateItineraryWithRawResponse(request);
        });

        assertEquals("Daily limit reached, please try again shortly", ex.getMessage());
        verify(geminiAiService, times(1)).generateTripPlan(anyString());
    }
}

