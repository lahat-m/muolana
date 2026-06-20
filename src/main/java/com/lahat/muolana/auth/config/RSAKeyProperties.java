package com.lahat.muolana.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "jwt")
public record RSAKeyProperties(
        RSAPublicKey publicKey,
        RSAPrivateKey privateKey,
        Long expiration,
        Long refreshExpiration
) {
}
