package com.intellitrip.service;

import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User signUp(String name, String email, String password, String country, String countryCode) {
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            throw new RuntimeException("An account with this email already exists.");
        }
        User user = new User(name, normalized, passwordEncoder.encode(password));
        user.setCountry(country);
        user.setCountryCode(countryCode);
        return userRepository.save(user);
    }

    @Transactional
    public User updateCountry(String userId, String country, String countryCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));
        user.setCountry(country);
        user.setCountryCode(countryCode);
        return userRepository.save(user);
    }

    public User signIn(String email, String password) {
        String normalized = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new RuntimeException("Invalid email or password."));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password.");
        }
        return user;
    }

    @Transactional
    public User signInWithGoogle(String email, String name) {
        String normalized = email.trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmail(normalized);
        if (existing.isPresent()) {
            User user = existing.get();
            user.setName(name.trim());
            return userRepository.save(user);
        }
        User user = new User(name.trim(), normalized, passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        return userRepository.save(user);
    }

    public Optional<User> getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId);
    }

    public void setSession(HttpSession session, User user) {
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        if (user.isAdmin()) {
            session.setAttribute("isAdmin", true);
        }
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}

