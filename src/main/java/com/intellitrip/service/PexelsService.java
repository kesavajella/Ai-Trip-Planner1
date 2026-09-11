package com.intellitrip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PexelsService {

    private static final Logger log = LoggerFactory.getLogger(PexelsService.class);
    private static final String PEXELS_BASE_URL = "https://api.pexels.com";
    private static final String PEXELS_SEARCH_PATH = "/v1/search";
    private static final String FALLBACK_IMAGE_URL = "https://images.unsplash.com/photo-1488646953014-85cb44e45868?auto=format&fit=crop&w=1600&q=80";

    private final RestClient restClient;
    private final String apiKey;

    public PexelsService(@Value("${pexels.api.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(PEXELS_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    public String fetchCoverImage(String destination) {
        if (destination == null || destination.isBlank()) {
            return FALLBACK_IMAGE_URL;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("PEXELS_API_KEY is not set, using fallback image for destination: {}", destination);
            return FALLBACK_IMAGE_URL;
        }

        try {
            String query = URLEncoder.encode(destination + " travel landmark", StandardCharsets.UTF_8);
            String url = PEXELS_SEARCH_PATH + "?query=" + query + "&per_page=1&orientation=landscape";

            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header("Authorization", apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("photos") instanceof java.util.List photos) {
                if (!photos.isEmpty() && photos.get(0) instanceof Map photo) {
                    Object srcObj = photo.get("src");
                    if (srcObj instanceof Map src) {
                        Object largeUrl = src.get("large");
                        if (largeUrl instanceof String large && !large.isBlank()) {
                            return large;
                        }
                    }
                }
            }

            log.warn("No Pexels photo found for destination: {}", destination);
            return FALLBACK_IMAGE_URL;

        } catch (Exception e) {
            log.warn("Failed to fetch Pexels image for destination '{}': {}", destination, e.getMessage());
            return FALLBACK_IMAGE_URL;
        }
    }
}
