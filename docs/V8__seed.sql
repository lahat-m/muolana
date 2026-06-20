-- ============================================================
--  V8 — Seed data
--
--  Inserts the default admin user.
--  Password hash must be rotated before any deployment.
--
--  No OAuth2 client registry rows — the custom JWT auth server
--  has no registered-client concept. Clients are identified by
--  the JWT claims (sub, role) embedded in the signed token.
-- ============================================================

-- ── Default admin user ───────────────────────────────────────
--  BCrypt hash below is a placeholder (cost 12).
--  Replace with a real hash before deploying:
--    String hash = new BCryptPasswordEncoder(12).encode("your-password");

INSERT INTO users (id, email, password_hash, role, full_name, is_active, email_verified_at)
VALUES (
    uuid_generate_v4(),
    'admin@muolana.ss',
    '$2a$12$placeholder_replace_before_deploy________________',
    'ADMIN',
    'System Administrator',
    TRUE,
    NOW()
)
ON CONFLICT (email) DO NOTHING;
