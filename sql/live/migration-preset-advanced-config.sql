-- 生成预设扩展：添加高级配置字段（IP类型/素材/模块/留人/互动）
-- 2026-03-23

ALTER TABLE live_generation_preset ADD COLUMN IF NOT EXISTS ip_type VARCHAR(30);
ALTER TABLE live_generation_preset ADD COLUMN IF NOT EXISTS material_type VARCHAR(30);
ALTER TABLE live_generation_preset ADD COLUMN IF NOT EXISTS script_module VARCHAR(40);
ALTER TABLE live_generation_preset ADD COLUMN IF NOT EXISTS retention_strategy VARCHAR(30);
ALTER TABLE live_generation_preset ADD COLUMN IF NOT EXISTS interaction_level VARCHAR(20);

COMMENT ON COLUMN live_generation_preset.ip_type IS 'IP 类型：phenomenal / top / 空(通用)';
COMMENT ON COLUMN live_generation_preset.material_type IS '素材类型：joke / chicken_soup / quote / interactive_game / 空';
COMMENT ON COLUMN live_generation_preset.script_module IS '话术模块：emotion_drive / value_creation / conversion_engine / trust_reinforcement / 空';
COMMENT ON COLUMN live_generation_preset.retention_strategy IS '留人策略：high_suspense / high_practical / high_climax / 空';
COMMENT ON COLUMN live_generation_preset.interaction_level IS '互动等级：light / medium / heavy / 空';
