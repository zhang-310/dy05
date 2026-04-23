-- 将 live_script.referenced_script_snapshot 从 JSONB 改为 TEXT
-- 避免 PostgreSQL JDBC 绑定 jsonb 报「未被支持的类型值：1,945,604,815」
-- 仅用于存储引用快照 JSON 字符串，无需 JSON 查询能力
ALTER TABLE live_script
  ALTER COLUMN referenced_script_snapshot TYPE TEXT USING (referenced_script_snapshot::text);

COMMENT ON COLUMN live_script.referenced_script_snapshot IS '引用时的话术快照（JSON 字符串：id/version/content/style/scriptType）';
