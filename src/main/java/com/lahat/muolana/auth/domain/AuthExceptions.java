package com.lahat.muolana.auth.domain;

import com.lahat.muolana.shared.exceptions.DomainException;

final class AuthExceptions {

    private AuthExceptions() {
    }

    static class InvalidCredentialsException extends DomainException {
        InvalidCredentialsException() {
            super("Invalid credentials");
        }
    }

    static class EmailAlreadyInUseException extends DomainException {
        EmailAlreadyInUseException() {
            super("Email already in use");
        }
    }

    static class InvalidRefreshTokenException extends DomainException {
        InvalidRefreshTokenException() {
            super("Invalid or expired refresh token");
        }
    }

    static class TokenRevokedException extends DomainException {
        TokenRevokedException() {
            super("Refresh token has been revoked");
        }
    }

    static class AuthenticatedUserMissingException extends DomainException {
        AuthenticatedUserMissingException() {
            super("Authenticated user not found in database");
        }
    }
}
