-- ============================================================
--  V1 — Enable pgvector extension
--  Must run before any table that uses the vector type.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;
