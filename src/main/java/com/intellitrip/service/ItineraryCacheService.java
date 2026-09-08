package com.intellitrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.dto.ItineraryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory cache for generated itineraries with a 24-hour Time-To-Live (TTL).
 * Keyed by: destination + days + budget + travelType.
 * Prevents redundant Gemini API calls when identical requests are made within 24 hours.
 */
@Service
public class ItineraryCacheService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryCacheService.class);
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private record CachedItinerary(ItineraryData itinerary, Instant expiresAt) {}

    private final ConcurrentHashMap<String, CachedItinerary> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ItineraryCacheService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a standardized cache key from destination, source, startDate, days, budget, and travel type.
     */
    public String buildKey(String destination, String source, String startDate, int days, String budget, String travelType) {
        String normDest = destination != null ? destination.trim().toLowerCase() : "";
        String normSource = source != null ? source.trim().toLowerCase() : "";
        String normDate = startDate != null ? startDate.trim().toLowerCase() : "";
        String normBudget = budget != null ? budget.trim().toLowerCase() : "";
        String normTravel = travelType != null ? travelType.trim().toLowerCase() : "";
        return normDest + "|" + normSource + "|" + normDate + "|" + days + "|" + normBudget + "|" + normTravel;
    }

    public String buildKey(String destination, int days, String budget, String travelType) {
        return buildKey(destination, "", "", days, budget, travelType);
    }

    /**
     * Retrieves a cached itinerary if present and not expired.
     */
    public Optional<ItineraryData> get(String destination, String source, String startDate, int days, String budget, String travelType) {
        String key = buildKey(destination, source, startDate, days, budget, travelType);
        CachedItinerary entry = cache.get(key);

        if (entry == null) {
            log.debug("Cache MISS for itinerary key: '{}'", key);
            return Optional.empty();
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            log.debug("Cache EXPIRED for itinerary key: '{}'", key);
            cache.remove(key);
            return Optional.empty();
        }

        log.info("🎯 Cache HIT for itinerary key: '{}' (Saved 1 Gemini API call)", key);
        return Optional.ofNullable(deepCopy(entry.itinerary()));
    }

    public Optional<ItineraryData> get(String destination, int days, String budget, String travelType) {
        return get(destination, "", "", days, budget, travelType);
    }

    /**
     * Caches an itinerary for 24 hours.
     */
    public void put(String destination, String source, String startDate, int days, String budget, String travelType, ItineraryData itinerary) {
        if (itinerary == null) return;
        String key = buildKey(destination, source, startDate, days, budget, travelType);
        Instant expiresAt = Instant.now().plus(DEFAULT_TTL);
        cache.put(key, new CachedItinerary(deepCopy(itinerary), expiresAt));
        log.debug("Cached itinerary for key: '{}', expires at: {}", key, expiresAt);

        // Opportunistic cleanup if cache gets large
        if (cache.size() > 500) {
            evictExpired();
        }
    }

    public void put(String destination, int days, String budget, String travelType, ItineraryData itinerary) {
        put(destination, "", "", days, budget, travelType, itinerary);
    }

    /**
     * Cleans up expired entries.
     */
    public void evictExpired() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    /**
     * Clears all cache entries.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Returns the active cache size.
     */
    public int size() {
        evictExpired();
        return cache.size();
    }

    /**
     * Creates a deep copy of ItineraryData so caller modifications (e.g. currency conversion, trip ID)
     * do not corrupt the cached object.
     */
    private ItineraryData deepCopy(ItineraryData original) {
        if (original == null) return null;
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(original), ItineraryData.class);
        } catch (Exception e) {
            log.warn("Failed to deep-copy ItineraryData, returning direct reference", e);
            return original;
        }
    }
}

