-- Phase 3 短剧模块 - 数据库迁移
-- 依据: docs/SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md 6.2, docs/IMPLEMENTATION_CHECKLIST.md Phase 3

-- 短剧主表
CREATE TABLE IF NOT EXISTS sv_drama (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    genre VARCHAR(50),
    total_episodes INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'draft',
    cover_url VARCHAR(500),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_drama_owner ON sv_drama(owner_id);
CREATE INDEX IF NOT EXISTS idx_sv_drama_deleted ON sv_drama(deleted);
COMMENT ON TABLE sv_drama IS '短剧主表';
COMMENT ON COLUMN sv_drama.genre IS '类型: 都市/古装/悬疑/甜宠/搞笑';
COMMENT ON COLUMN sv_drama.status IS '状态: draft/processing/completed';

-- 剧集表
CREATE TABLE IF NOT EXISTS sv_drama_episode (
    id BIGSERIAL PRIMARY KEY,
    drama_id BIGINT NOT NULL,
    episode_number INTEGER NOT NULL,
    title VARCHAR(200),
    project_id BIGINT,
    synopsis TEXT,
    cliffhanger TEXT,
    status VARCHAR(20) DEFAULT 'draft',
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_drama_episode_drama ON sv_drama_episode(drama_id);
CREATE INDEX IF NOT EXISTS idx_sv_drama_episode_project ON sv_drama_episode(project_id);
COMMENT ON TABLE sv_drama_episode IS '短剧剧集表';
COMMENT ON COLUMN sv_drama_episode.cliffhanger IS '悬念/钩子 (留住观众看下一集)';

-- 角色表
CREATE TABLE IF NOT EXISTS sv_drama_character (
    id BIGSERIAL PRIMARY KEY,
    drama_id BIGINT NOT NULL,
    character_name VARCHAR(100) NOT NULL,
    description TEXT,
    reference_image_url VARCHAR(500),
    reference_bos_key VARCHAR(500),
    voice_id VARCHAR(100),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_drama_character_drama ON sv_drama_character(drama_id);
COMMENT ON TABLE sv_drama_character IS '短剧角色表';
COMMENT ON COLUMN sv_drama_character.reference_image_url IS '角色参考图 (跨集一致性的关键)';
