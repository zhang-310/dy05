-- 将 ai_kb_document.metadata 从 JSONB 改为 TEXT，避免部分环境下 JDBC 绑定 jsonb 报「未被支持的类型值」
-- 仅用于存储目录导入等扩展信息，无需 JSON 查询能力
ALTER TABLE ai_kb_document
  ALTER COLUMN metadata TYPE TEXT USING (metadata::text);

COMMENT ON COLUMN ai_kb_document.metadata IS '扩展元数据（JSON 字符串，如目录导入的 relativePath/dirCategory 等）';
