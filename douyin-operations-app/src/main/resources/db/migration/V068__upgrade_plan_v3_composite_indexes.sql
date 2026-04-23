-- upgrade-plan-v3 Phase2：补充高频复合索引（与 V001 并存，IF NOT EXISTS）

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'live_session') THEN
    CREATE INDEX IF NOT EXISTS idx_live_session_user_deleted_ctime ON live_session(user_id, deleted, create_time DESC);
    CREATE INDEX IF NOT EXISTS idx_live_session_status_deleted_ctime ON live_session(status, deleted, create_time DESC);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sv_project') THEN
    CREATE INDEX IF NOT EXISTS idx_sv_project_owner_deleted_ctime ON sv_project(owner_id, deleted, create_time DESC);
    CREATE INDEX IF NOT EXISTS idx_sv_project_status_deleted_ctime ON sv_project(status, deleted, create_time DESC);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ai_kb_document') THEN
    CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_kb_deleted_ctime ON ai_kb_document(kb_id, deleted, create_time DESC);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ai_kb_chunk') THEN
    CREATE INDEX IF NOT EXISTS idx_ai_kb_chunk_doc_deleted ON ai_kb_chunk(document_id, deleted);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'product_script_version')
     AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'product_script_version' AND column_name = 'created_at') THEN
    CREATE INDEX IF NOT EXISTS idx_product_script_version_prod_deleted_created_at ON product_script_version(product_id, deleted, created_at DESC);
  END IF;
END $$;
