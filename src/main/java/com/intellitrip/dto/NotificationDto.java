package com.intellitrip.dto;

import java.time.LocalDateTime;

public class NotificationDto {
    private String id;
    private String destination;
    private String generatedAt;
    private int days;

    public NotificationDto() {}

    public NotificationDto(String id, String destination, LocalDateTime generatedAt, int days) {
        this.id = id;
        this.destination = destination;
        this.generatedAt = generatedAt != null ? generatedAt.toString() : LocalDateTime.now().toString();
        this.days = days;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
}

