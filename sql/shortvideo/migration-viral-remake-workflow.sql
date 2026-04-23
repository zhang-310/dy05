-- LF-03：与 Flyway V103 一致（爆款二创闭环字段）
-- 见 src/main/resources/db/migration/V103__viral_remake_workflow.sql

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_status INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN sv_viral_video.remake_status IS
  '二创状态：0=未处理, 1=已推荐(AI推荐), 2=运营已确认, 3=脚本已生成, 4=已进拍摄/任务, 5=已完成';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_suggestions TEXT;
COMMENT ON COLUMN sv_viral_video.remake_suggestions IS 'AI 二创建议 JSON：[{remakeType, angle, brief, matchScore}]';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS matched_persona_ids TEXT;
COMMENT ON COLUMN sv_viral_video.matched_persona_ids IS '匹配的人设 ID JSON 数组，如 [1,3,5]';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS industry_tags TEXT;
COMMENT ON COLUMN sv_viral_video.industry_tags IS '行业标签 JSON 数组，如 ["护肤","彩妆","美妆工具"]';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_script_id BIGINT;
COMMENT ON COLUMN sv_viral_video.remake_script_id IS '二创生成的脚本 sv_script.id';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_task_id BIGINT;
COMMENT ON COLUMN sv_viral_video.remake_task_id IS '关联拍摄任务 sv_shooting_task.id';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS confirmed_by BIGINT;
COMMENT ON COLUMN sv_viral_video.confirmed_by IS '运营确认人 auth_user.id';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;
COMMENT ON COLUMN sv_viral_video.confirmed_at IS '运营确认时间';

CREATE INDEX IF NOT EXISTS idx_sv_viral_remake_status ON sv_viral_video(remake_status, viral_score DESC) WHERE deleted = 0;
