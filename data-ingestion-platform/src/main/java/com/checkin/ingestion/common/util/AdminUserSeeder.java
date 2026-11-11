package com.checkin.ingestion.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.checkin.ingestion.user.Role;
import com.checkin.ingestion.user.User;
import com.checkin.ingestion.user.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default.admin.email:admin@gmail.com}")
    private String defaultAdminEmail;
    
    @Value("${app.default.admin.password:Admin@12345}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {

        if (userRepository.existsByEmail(defaultAdminEmail)) {
            log.info("System Boot: Default admin '{}' already exists. Skipping creation.", defaultAdminEmail);
            return;
        }

        log.info("System Boot: Default admin not found. Creating '{}'...", defaultAdminEmail);

        User adminUser = User.builder()
                .email(defaultAdminEmail)
                .passwordHash(passwordEncoder.encode(defaultAdminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(adminUser);
        log.info("System Boot: Default admin created successfully.");
    }
}