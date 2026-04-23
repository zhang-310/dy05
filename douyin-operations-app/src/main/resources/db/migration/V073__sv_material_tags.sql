-- 素材标签字段
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sv_material') THEN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sv_material' AND column_name = 'tags') THEN
      ALTER TABLE sv_material ADD COLUMN tags VARCHAR(512);
      COMMENT ON COLUMN sv_material.tags IS '素材标签，逗号分隔（如：产品,美妆,特效）';
    END IF;
  END IF;
END $$;

-- 标签搜索索引
CREATE INDEX IF NOT EXISTS idx_sv_material_tags ON sv_material(tags) WHERE tags IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sv_material_owner_type ON sv_material(owner_id, material_type, deleted);
