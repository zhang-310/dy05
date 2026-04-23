-- P1：二创状态变迁日志 + 用户编辑变量表（爆款深度分析升级）

CREATE TABLE IF NOT EXISTS sv_viral_remake_log (
    id BIGSERIAL PRIMARY KEY,
    viral_video_id BIGINT NOT NULL,
    from_status INTEGER,
    to_status INTEGER NOT NULL,
    operator_id BIGINT,
    remark TEXT,
    create_time TIMESTAMP DEFAULT NOW(),
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_remake_log_viral ON sv_viral_remake_log(viral_video_id);

COMMENT ON TABLE sv_viral_remake_log IS '爆款二创状态变迁日志';
COMMENT ON COLUMN sv_viral_remake_log.from_status IS '变更前 remake_status';
COMMENT ON COLUMN sv_viral_remake_log.to_status IS '变更后 remake_status';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_variables TEXT;
COMMENT ON COLUMN sv_viral_video.remake_variables IS '用户编辑后的二创变量表 JSON';

ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS source_viral_id BIGINT;
COMMENT ON COLUMN sv_project.source_viral_id IS '源自爆款库 sv_viral_video.id（viral_clone 等）';
CREATE INDEX IF NOT EXISTS idx_sv_project_source_viral ON sv_project(source_viral_id) WHERE source_viral_id IS NOT NULL;
