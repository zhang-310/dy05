-- ai_kb_document.metadata：历史迁移曾用 JSONB，与 JPA String + columnDefinition=text 不一致时 INSERT 会报
-- column "metadata" is of type jsonb but expression is of type character varying。
-- 统一为 TEXT（仅存 JSON 字符串，无需库内 JSON 运算符）。
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'ai_kb_document'
      AND column_name = 'metadata'
      AND udt_name = 'jsonb'
  ) THEN
    ALTER TABLE ai_kb_document ALTER COLUMN metadata TYPE TEXT USING (metadata::text);
    COMMENT ON COLUMN ai_kb_document.metadata IS '扩展元数据（JSON 字符串，如目录导入的 relativePath/dirCategory 等）';
  END IF;
END $$;
