package com.intellitrip.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);

    /**
     * Verify a Firebase ID token and return the decoded token.
     */
    public FirebaseToken verifyIdToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException | IllegalStateException e) {
            log.error("Failed to verify Firebase token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a password reset link for the given email.
     */
    public String generatePasswordResetLink(String email) {
        try {
            return FirebaseAuth.getInstance().generatePasswordResetLink(email);
        } catch (FirebaseAuthException | IllegalStateException e) {
            log.error("Failed to generate password reset link: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get or create a Firebase user by email.
     */
    public UserRecord getUserByEmail(String email) {
        try {
            return FirebaseAuth.getInstance().getUserByEmail(email);
        } catch (FirebaseAuthException | IllegalStateException e) {
            return null;
        }
    }

    /**
     * Check if Firebase is initialized and available.
     */
    public boolean isAvailable() {
        try {
            FirebaseAuth.getInstance();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}

