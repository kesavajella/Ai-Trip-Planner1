package com.intellitrip.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dedicated lightweight health check endpoint.
 * Returns HTTP 200 OK without invoking Gemini SDK, database, or Firebase,
 * ensuring external uptime pingers/cron jobs never consume external API quota or database connections.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}

