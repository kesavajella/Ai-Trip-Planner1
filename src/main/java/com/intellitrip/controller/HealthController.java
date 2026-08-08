package com.intellitrip.controller;

import com.intellitrip.repository.TripRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

@RestController
public class HealthController {

    private final DataSource dataSource;
    private final TripRepository tripRepository;

    public HealthController(DataSource dataSource, TripRepository tripRepository) {
        this.dataSource = dataSource;
        this.tripRepository = tripRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = false;
        String dbProduct = "unknown";
        try {
            Connection conn = dataSource.getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            dbProduct = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
            dbUp = conn.isValid(2);
            conn.close();
        } catch (SQLException e) {
            dbProduct = "error: " + e.getMessage();
        }
        return ResponseEntity.ok(Map.of(
                "status", dbUp ? "UP" : "DOWN",
                "service", "intellitrip",
                "database", dbProduct,
                "totalTrips", tripRepository.count()
        ));
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("OK");
    }
}

