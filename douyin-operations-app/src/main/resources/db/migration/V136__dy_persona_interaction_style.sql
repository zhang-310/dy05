ALTER TABLE dy_persona
    ADD COLUMN IF NOT EXISTS interaction_style VARCHAR(64);
ALTER TABLE dy_persona
    ADD COLUMN IF NOT EXISTS language_style VARCHAR(64);
