-- ============================================================
--  V2 — Auth module
--
--  Simple custom JWT with RSA256. No Spring Authorization Server.
--  The auth server signs JWTs with an RSA private key (PEM /
--  PKCS#12 keystore). Resource servers validate using only the
--  RSA public key — private key never leaves the auth server.
--
--  Library: io.jsonwebtoken (JJWT 0.12+)
--    Jwts.builder()
--        .subject(userId)
--        .claim("role", role)
--        .expiration(Date.from(Instant.now().plus(15, MINUTES)))
--        .signWith(rsaPrivateKey)   // RS256 inferred from key type
--        .compact();
--
--  Tables:
--    users              Application user accounts (citizens + admins)
--    refresh_tokens     Issued refresh tokens (rotated on use)
--    token_revocations  Revoked access tokens before expiry (admin logout)
-- ============================================================

-- ── Users ────────────────────────────────────────────────────

CREATE TYPE user_role AS ENUM ('CITIZEN', 'ADMIN');

CREATE TABLE users (
    id                UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    email             TEXT        NOT NULL UNIQUE,
    password_hash     TEXT,                          -- null for anonymous sessions
    role              user_role   NOT NULL DEFAULT 'CITIZEN',
    full_name         TEXT,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    email_verified_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);

-- ── Refresh tokens ───────────────────────────────────────────
--  One row per issued refresh token. Rotated on every use —
--  old token deleted, new token inserted. Revoked on logout.

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL UNIQUE,        -- SHA-256 of the opaque token string
    expires_at  TIMESTAMPTZ NOT NULL,               -- 7 days for citizens, 8h for admins
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_user       ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_expires    ON refresh_tokens (expires_at);

-- ── Token revocations ────────────────────────────────────────
--  Short-lived table. Access tokens are JWTs (15 min TTL) —
--  revocation is only needed for admin forced-logout scenarios.
--  A background job prunes rows where expires_at has passed.

CREATE TABLE token_revocations (
    jti         TEXT        PRIMARY KEY,            -- JWT "jti" claim (uuid)
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,               -- matches original JWT expiry
    revoked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revocations_expires ON token_revocations (expires_at);
