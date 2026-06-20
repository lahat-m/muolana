-- Allow anonymous (unauthenticated) chat sessions.
-- Drop the FK to auth.users and the NOT NULL constraint on user_id.
ALTER TABLE conversations.sessions
    DROP CONSTRAINT IF EXISTS sessions_user_id_fkey;

ALTER TABLE conversations.sessions
    ALTER COLUMN user_id DROP NOT NULL;
