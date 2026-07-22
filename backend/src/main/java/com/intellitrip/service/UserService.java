package com.intellitrip.service;

import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User createUser(String email, String password, String fullName) {
        User user = User.builder()
                .email(email.toLowerCase().trim())
                .password(passwordEncoder.encode(password))
                .fullName(fullName.trim())
                .role(User.Role.USER)
                .build();
        return userRepository.save(user);
    }
}
