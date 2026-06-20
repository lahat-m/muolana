package com.lahat.muolana.auth;

import com.lahat.muolana.auth.domain.AuthService;
import com.lahat.muolana.auth.domain.UserVM;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthAPI {

    private final AuthService authService;

    public AuthAPI(AuthService authService) {
        this.authService = authService;
    }

    public UserVM getUserById(UUID userId) {
        return authService.getUserById(userId);
    }

    public boolean existsByEmail(String email) {
        return authService.existsByEmail(email);
    }
}
