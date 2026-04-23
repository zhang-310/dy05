-- ============================================================
-- 短视频模块 v3.2 设计对齐 - 数据库迁移
-- 版本: v3.2
-- 依据: docs/shortvideo/DESIGN_VS_IMPLEMENTATION_GAP.md P3
-- ============================================================

-- 1. sv_drama_character 扩展（角色身份管理：多参考图、LoRA、声音克隆）
ALTER TABLE sv_drama_character ADD COLUMN IF NOT EXISTS reference_images JSONB;
ALTER TABLE sv_drama_character ADD COLUMN IF NOT EXISTS lora_model_path VARCHAR(500);
ALTER TABLE sv_drama_character ADD COLUMN IF NOT EXISTS prompt_tags VARCHAR(500);
ALTER TABLE sv_drama_character ADD COLUMN IF NOT EXISTS voice_sample_url VARCHAR(500);
ALTER TABLE sv_drama_character ADD COLUMN IF NOT EXISTS cloned_voice_id VARCHAR(100);

COMMENT ON COLUMN sv_drama_character.reference_images IS '多参考图 JSON 数组 [{url,bos_key}]，用于角色一致性';
COMMENT ON COLUMN sv_drama_character.lora_model_path IS 'LoRA 模型路径（训练后）';
COMMENT ON COLUMN sv_drama_character.prompt_tags IS 'Prompt 标签（JSON 或逗号分隔），加权生成';
COMMENT ON COLUMN sv_drama_character.voice_sample_url IS '声音克隆样本 URL（5秒）';
COMMENT ON COLUMN sv_drama_character.cloned_voice_id IS '克隆后的固定音色 ID（ElevenLabs 等）';

-- 2. sv_project.project_type 口播类型（project_type 已为 VARCHAR(50)，支持任意值，仅更新注释）
COMMENT ON COLUMN sv_project.project_type IS '类型：viral_clone/daily/soft_ad/talking_head（口播）';

-- 3. sv_shot 音频字段（TTS 文本、音效提示、TTS/BGM URL）
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS dialogue_text TEXT;
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS sfx_hints VARCHAR(500);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS tts_url VARCHAR(500);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS bgm_url VARCHAR(500);

COMMENT ON COLUMN sv_shot.dialogue_text IS 'TTS 输入文本（与 dialogue 可同源，用于 TTS 合成）';
COMMENT ON COLUMN sv_shot.sfx_hints IS '音效提示（JSON 或逗号分隔），如 door_closed, wind';
COMMENT ON COLUMN sv_shot.tts_url IS 'TTS 合成音频 URL（BOS CDN）';
COMMENT ON COLUMN sv_shot.bgm_url IS 'BGM 背景音乐 URL（BOS CDN）';

-- 4. sv_generation_log 扩展（内容类型、路由原因、成本、音频标记）
ALTER TABLE sv_generation_log ADD COLUMN IF NOT EXISTS content_type VARCHAR(50);
ALTER TABLE sv_generation_log ADD COLUMN IF NOT EXISTS route_reason VARCHAR(200);
ALTER TABLE sv_generation_log ADD COLUMN IF NOT EXISTS cost_cents INT;
ALTER TABLE sv_generation_log ADD COLUMN IF NOT EXISTS has_audio BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN sv_generation_log.content_type IS '内容类型：image2video/audio2video/text2video';
COMMENT ON COLUMN sv_generation_log.route_reason IS '智能路由选择原因';
COMMENT ON COLUMN sv_generation_log.cost_cents IS '成本（分）';
COMMENT ON COLUMN sv_generation_log.has_audio IS '是否含音频（音视频联合）';
