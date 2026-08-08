package com.intellitrip.config;

import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiConfig {

    @Value("${GEMINI_API_KEY:#{null}}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-flash-latest}")
    private String model;

    @Value("${app.gemini.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${app.gemini.read-timeout-ms:30000}")
    private int readTimeoutMs;

    @Value("${app.gemini.max-retries:3}")
    private int maxRetries;

    @Value("${app.gemini.initial-delay-ms:2000}")
    private long initialDelayMs;

    @Bean
    public RestClient geminiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(factory)
                .build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
