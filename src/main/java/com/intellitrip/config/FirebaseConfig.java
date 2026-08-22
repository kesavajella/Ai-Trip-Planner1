package com.intellitrip.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.config.json:}")
    private String firebaseConfigJson;

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                if (firebaseConfigJson != null && !firebaseConfigJson.isBlank()) {
                    // Initialize from environment variable (JSON string)
                    InputStream serviceAccount = new ByteArrayInputStream(
                        firebaseConfigJson.getBytes(StandardCharsets.UTF_8)
                    );
                    FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialized from environment variable");
                } else {
                    // Try loading from classpath (for local dev)
                    InputStream serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");
                    if (serviceAccount != null) {
                        FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                        FirebaseApp.initializeApp(options);
                        log.info("Firebase initialized from classpath file");
                    } else {
                        log.warn("No Firebase configuration found. Firebase auth features will be disabled.");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to initialize Firebase", e);
            }
        }
    }
}

