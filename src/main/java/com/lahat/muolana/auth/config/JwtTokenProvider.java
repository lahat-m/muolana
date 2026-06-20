package com.lahat.muolana.auth.config;

import com.lahat.muolana.auth.domain.UserEntity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RSAKeyProperties rsaKeys;

    public JwtTokenProvider(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, RSAKeyProperties rsaKeys) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.rsaKeys = rsaKeys;
    }

    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(rsaKeys.expiration(), ChronoUnit.MILLIS))
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateRefreshToken(UserEntity user, String jti) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(rsaKeys.refreshExpiration(), ChronoUnit.MILLIS))
                .subject(user.getEmail())
                .claim("type", "refresh")
                .claim("jti", jti)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public String extractJti(String token) {
        return decode(token).getClaimAsString("jti");
    }

    public long getAccessExpiration() {
        return rsaKeys.expiration();
    }

    public long getRefreshExpiration() {
        return rsaKeys.refreshExpiration();
    }
}
