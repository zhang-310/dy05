-- V-5：分镜剪辑时间轴（逐段 trim + 可选属性 JSON）→ auto-compose / FFmpeg clipTrims
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS trim_start_sec DOUBLE PRECISION;
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS trim_end_sec DOUBLE PRECISION;
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS timeline_json TEXT;

COMMENT ON COLUMN sv_shot.trim_start_sec IS '成片合并前该片源内裁剪起点（秒），null 表示从 0';
COMMENT ON COLUMN sv_shot.trim_end_sec IS '成片合并前该片源内裁剪结束（秒），null 表示不裁末端';
COMMENT ON COLUMN sv_shot.timeline_json IS '可选：属性关键帧等扩展 JSON（预留）';
