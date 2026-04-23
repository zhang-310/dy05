-- Entity 使用 Integer；历史库可能为 BOOLEAN 或已为 INTEGER
-- USING 需对 boolean/integer 均合法，避免 CASE WHEN col THEN（col 为 int 时报错）

ALTER TABLE ai_prompt_template
    ALTER COLUMN is_active DROP DEFAULT;
ALTER TABLE ai_prompt_template
    ALTER COLUMN is_active TYPE INTEGER USING (
        CASE
            WHEN is_active::text IN ('t', 'true', '1') THEN 1
            WHEN is_active::text IN ('f', 'false', '0') THEN 0
            WHEN is_active::text ~ '^[0-9]+$' THEN is_active::text::integer
            ELSE NULL
        END
    );
ALTER TABLE ai_prompt_template
    ALTER COLUMN is_active SET DEFAULT 1;

ALTER TABLE ai_prompt_template
    ALTER COLUMN is_default DROP DEFAULT;
ALTER TABLE ai_prompt_template
    ALTER COLUMN is_default TYPE INTEGER USING (
        CASE
            WHEN is_default::text IN ('t', 'true', '1') THEN 1
            WHEN is_default::text IN ('f', 'false', '0') THEN 0
            WHEN is_default::text ~ '^[0-9]+$' THEN is_default::text::integer
            ELSE NULL
        END
    );
ALTER TABLE ai_prompt_template
    ALTER COLUMN is_default SET DEFAULT 0;

ALTER TABLE ai_prompt_template
    ALTER COLUMN version TYPE VARCHAR(32) USING (version::text);
ALTER TABLE ai_prompt_template
    ALTER COLUMN version SET DEFAULT '1.0';
