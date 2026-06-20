package com.lahat.muolana.auth.web;

import com.lahat.muolana.auth.domain.AuthResponse;
import com.lahat.muolana.auth.domain.AuthService;
import com.lahat.muolana.auth.web.dtos.LoginRequest;
import com.lahat.muolana.auth.web.dtos.LogoutRequest;
import com.lahat.muolana.auth.web.dtos.RefreshRequest;
import com.lahat.muolana.auth.web.dtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest.email(), loginRequest.password());
        return ResponseEntity.ok(new TokenResponse(authResponse.accessToken(), authResponse.refreshToken(), authResponse.expiresIn()));
    }

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        AuthResponse authResponse = authService.refresh(refreshRequest.refreshToken());
        return ResponseEntity.ok(new TokenResponse(authResponse.accessToken(), authResponse.refreshToken(), authResponse.expiresIn()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
