-- 项目参考图 URL：人物参考、场景参考（素材准备页上传后保存到项目）
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS character_reference_url VARCHAR(500);
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS scene_reference_url VARCHAR(500);
COMMENT ON COLUMN sv_project.character_reference_url IS '人物参考图 BOS CDN URL（素材准备页上传）';
COMMENT ON COLUMN sv_project.scene_reference_url IS '场景参考图 BOS CDN URL（素材准备页上传）';
