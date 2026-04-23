-- Entity 使用 Integer；历史库可能为 BIGINT
ALTER TABLE live_session
    ALTER COLUMN viewers TYPE INTEGER USING (viewers::integer);
