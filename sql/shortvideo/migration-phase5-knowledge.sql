-- Phase 5 运镜知识库 - 数据库迁移
-- 依据: docs/SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md 10.2, 10.4

-- 运镜 Prompt 知识库
CREATE TABLE IF NOT EXISTS sv_cinematic_preset (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    camera_type VARCHAR(50) NOT NULL,
    prompt_template TEXT NOT NULL,
    negative_prompt TEXT,
    quality_level VARCHAR(20) DEFAULT 'premium-fhd',
    best_model VARCHAR(50),
    success_rate DECIMAL(5,2) DEFAULT 0,
    avg_quality_score DECIMAL(5,2) DEFAULT 0,
    use_count INT DEFAULT 0,
    sample_video_url VARCHAR(500),
    owner_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE sv_cinematic_preset IS '运镜 Prompt 知识库';
COMMENT ON COLUMN sv_cinematic_preset.camera_type IS '运镜类型 (CameraType.code)';

-- 生成历史记录 (用于知识积累)
CREATE TABLE IF NOT EXISTS sv_generation_log (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT,
    shot_id BIGINT,
    camera_type VARCHAR(50),
    quality_level VARCHAR(20),
    prompt TEXT,
    ai_provider VARCHAR(50),
    success BOOLEAN DEFAULT false,
    quality_score DECIMAL(5,2),
    generation_time_ms BIGINT,
    video_url VARCHAR(500),
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sv_generation_log IS '图生视频生成历史，用于知识积累';

-- 场景-运镜推荐映射
CREATE TABLE IF NOT EXISTS sv_scene_camera_mapping (
    id BIGSERIAL PRIMARY KEY,
    scene_keyword VARCHAR(100) NOT NULL,
    recommended_camera VARCHAR(50) NOT NULL,
    confidence DECIMAL(5,2) DEFAULT 0.5,
    source VARCHAR(20) DEFAULT 'manual',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sv_scene_camera_mapping IS '场景关键词→推荐运镜映射';

-- 初始化系统预设 (10 条) - 幂等：表为空时插入
INSERT INTO sv_cinematic_preset (name, category, camera_type, prompt_template, best_model)
SELECT * FROM (VALUES
  ('电影慢推', 'cinematic', 'dolly-in', 'slow dolly in, {scene}, cinematic depth of field, {action}, film quality lighting', 'kling'),
  ('动作跟踪', 'action', 'tracking', 'tracking shot following subject, {scene}, {action}, dynamic motion, professional stabilization', 'runway'),
  ('纪录片手持', 'documentary', 'handheld', 'handheld documentary style, {scene}, natural shake, {action}, authentic feel', 'minimax'),
  ('航拍全景', 'epic', 'drone-aerial', 'aerial drone sweeping shot, {scene}, bird eye view, {action}, epic scale', 'luma'),
  ('甜宠环绕', 'romance', 'orbit', 'smooth orbit around subject, {scene}, warm golden light, {action}, romantic bokeh', 'runway'),
  ('悬疑推进', 'suspense', 'push-in', 'slow push in, {scene}, dramatic shadows, {action}, building tension', 'kling'),
  ('搞笑快摇', 'comedy', 'whip-pan', 'whip pan, {scene}, fast movement, {action}, comedic timing', 'minimax'),
  ('希区柯克', 'thriller', 'dolly-zoom', 'dolly zoom vertigo effect, {scene}, background compression, {action}, unsettling', 'runway'),
  ('对话过肩', 'dialogue', 'over-shoulder', 'over the shoulder shot, {scene}, conversation framing, {action}, natural depth', 'kling'),
  ('焦点叙事', 'narrative', 'rack-focus', 'rack focus shifting, {scene}, foreground to background, {action}, storytelling', 'luma')
) AS v(name, category, camera_type, prompt_template, best_model)
WHERE (SELECT COUNT(*) FROM sv_cinematic_preset) = 0;

-- 场景-运镜推荐初始数据 (12 条) - 幂等：表为空时插入
INSERT INTO sv_scene_camera_mapping (scene_keyword, recommended_camera, confidence)
SELECT * FROM (VALUES
  ('打斗', 'handheld', 0.85),
  ('追逐', 'tracking', 0.90),
  ('对话', 'over-shoulder', 0.80),
  ('风景', 'drone-aerial', 0.90),
  ('特写', 'push-in', 0.85),
  ('全景', 'crane-up', 0.80),
  ('紧张', 'dolly-zoom', 0.75),
  ('浪漫', 'orbit', 0.80),
  ('悲伤', 'static', 0.70),
  ('惊吓', 'whip-pan', 0.75),
  ('回忆', 'zoom-out', 0.70),
  ('揭秘', 'dolly-in', 0.85)
) AS v(scene_keyword, recommended_camera, confidence)
WHERE (SELECT COUNT(*) FROM sv_scene_camera_mapping) = 0;
