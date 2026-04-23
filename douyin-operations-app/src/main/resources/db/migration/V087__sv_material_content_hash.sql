-- P2 A-6：素材内容 SHA-256（去重/溯源）；感知哈希占位
ALTER TABLE sv_material ADD COLUMN IF NOT EXISTS content_sha256 VARCHAR(64);
ALTER TABLE sv_material ADD COLUMN IF NOT EXISTS perceptual_hash VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_sv_material_owner_sha ON sv_material(owner_id, content_sha256) WHERE deleted = 0 AND content_sha256 IS NOT NULL;

COMMENT ON COLUMN sv_material.content_sha256 IS '文件内容 SHA-256（富化阶段写入）';
COMMENT ON COLUMN sv_material.perceptual_hash IS '感知哈希预留，真 pHash 接入后写入';
