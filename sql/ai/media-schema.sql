-- AI 图像生成记录表
CREATE TABLE IF NOT EXISTS ai_image_generation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt TEXT,
    negative_prompt TEXT,
    image_url TEXT,
    generation_type VARCHAR(32),
    parameters TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_image_gen_user_id ON ai_image_generation(user_id);
CREATE INDEX idx_image_gen_deleted ON ai_image_generation(deleted);

COMMENT ON TABLE ai_image_generation IS 'AI 图像生成记录表';
COMMENT ON COLUMN ai_image_generation.prompt IS '生成提示词';
COMMENT ON COLUMN ai_image_generation.negative_prompt IS '负面提示词';
COMMENT ON COLUMN ai_image_generation.image_url IS '图像 URL';
COMMENT ON COLUMN ai_image_generation.generation_type IS '生成类型：text2img/img2img/edit';
COMMENT ON COLUMN ai_image_generation.parameters IS '生成参数 JSON';
COMMENT ON COLUMN ai_image_generation.status IS '状态：0=生成中 1=完成';

-- AI 语音合成记录表
CREATE TABLE IF NOT EXISTS ai_tts_generation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    text TEXT,
    voice VARCHAR(64),
    language VARCHAR(16),
    audio_url VARCHAR(512),
    duration BIGINT,
    file_size BIGINT,
    status INTEGER NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tts_gen_user_id ON ai_tts_generation(user_id);
CREATE INDEX idx_tts_gen_deleted ON ai_tts_generation(deleted);

COMMENT ON TABLE ai_tts_generation IS 'AI 语音合成记录表';
COMMENT ON COLUMN ai_tts_generation.text IS '合成文本';
COMMENT ON COLUMN ai_tts_generation.voice IS '音色';
COMMENT ON COLUMN ai_tts_generation.language IS '语言';
COMMENT ON COLUMN ai_tts_generation.audio_url IS '音频 URL';
COMMENT ON COLUMN ai_tts_generation.duration IS '时长（毫秒）';
COMMENT ON COLUMN ai_tts_generation.file_size IS '文件大小（字节）';
COMMENT ON COLUMN ai_tts_generation.status IS '状态：0=生成中 1=完成';
