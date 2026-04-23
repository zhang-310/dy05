-- Phase 1 电影级质量升级 - 数据库迁移
-- 依据: docs/PHASE1_IMPLEMENTATION_PLAN.md, docs/SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md

-- sv_shot 新增运镜、质量、AI 模型相关字段
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS camera_type VARCHAR(50);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS camera_params TEXT;
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS quality_level VARCHAR(20);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS ai_model VARCHAR(50);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS quality_score DECIMAL(5,2);

COMMENT ON COLUMN sv_shot.camera_type IS '运镜类型 (CameraType.code)';
COMMENT ON COLUMN sv_shot.camera_params IS '运镜参数 (JSON)';
COMMENT ON COLUMN sv_shot.quality_level IS '质量级别 (QualityLevel.code)';
COMMENT ON COLUMN sv_shot.ai_model IS 'AI 视频生成模型名称';
COMMENT ON COLUMN sv_shot.quality_score IS '质量评分 (0-100)';

-- sv_material 新增后期处理、AI 提供者配置
ALTER TABLE sv_material ADD COLUMN IF NOT EXISTS post_processing_config TEXT;
ALTER TABLE sv_material ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(50);

COMMENT ON COLUMN sv_material.post_processing_config IS '后期处理配置 (JSON: 调色/稳定/降噪)';
COMMENT ON COLUMN sv_material.ai_provider IS 'AI 视频生成提供者 (kling/minimax/runway 等)';
