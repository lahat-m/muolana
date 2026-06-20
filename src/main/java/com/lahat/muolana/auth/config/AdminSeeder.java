package com.lahat.muolana.auth.config;

import com.lahat.muolana.auth.domain.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AuthService authService;

    @Value("${app.admin.email:admin@muolana.ss}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@2024!}")
    private String adminPassword;

    @Value("${app.admin.full-name:System Admin}")
    private String adminFullName;

    AdminSeeder(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.createAdminIfAbsent(adminEmail, adminPassword, adminFullName);
        log.info("Admin account ready: {}", adminEmail);
    }
}
