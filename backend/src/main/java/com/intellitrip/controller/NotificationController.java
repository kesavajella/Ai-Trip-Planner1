package com.intellitrip.controller;

import com.intellitrip.model.Notification;
import com.intellitrip.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> response = notifications.stream().map(n -> java.util.Map.<String, Object>of(
                "id", n.getId(),
                "destination", n.getDestination(),
                "days", n.getDays(),
                "message", n.getMessage(),
                "isRead", n.getIsRead(),
                "createdAt", n.getCreatedAt().toString()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
