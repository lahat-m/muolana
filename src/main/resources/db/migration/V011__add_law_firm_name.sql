-- Add optional law firm / chambers name to lawyers.
ALTER TABLE lawyers.lawyers
    ADD COLUMN IF NOT EXISTS law_firm_name TEXT;
