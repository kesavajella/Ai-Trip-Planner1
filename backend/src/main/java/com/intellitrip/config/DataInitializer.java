package com.intellitrip.config;

import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .email("admin@intellitrip.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Admin User")
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);

            User demoUser = User.builder()
                    .email("amelia@example.com")
                    .password(passwordEncoder.encode("password"))
                    .fullName("Amelia Hart")
                    .role(User.Role.USER)
                    .build();
            userRepository.save(demoUser);
        }
    }
}
