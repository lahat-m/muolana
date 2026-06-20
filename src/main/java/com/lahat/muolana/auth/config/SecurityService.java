package com.lahat.muolana.auth.config;

import com.lahat.muolana.shared.domain.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service("ss")
public class SecurityService {

    public UserId getCurrentUserId() {
        Jwt jwt = requireJwt();
        return UserId.of(UUID.fromString(jwt.getClaimAsString("userId")));
    }

    public boolean isCurrentUser(UUID userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getPrincipal() instanceof Jwt jwt)) return false;
        return Objects.equals(userId.toString(), jwt.getClaimAsString("userId"));
    }

    public boolean isCurrentUser(String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getPrincipal() instanceof Jwt jwt)) return false;
        return Objects.equals(userId, jwt.getClaimAsString("userId"));
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_" + role));
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isCitizen() {
        return hasRole("CITIZEN");
    }

    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt;
    }

    private Jwt requireJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getPrincipal() instanceof Jwt jwt))
            throw new SecurityException("No authenticated user found");
        return jwt;
    }
}
