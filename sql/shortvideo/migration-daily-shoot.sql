-- ============================================================
-- 每日拍摄脚本功能 - 迁移脚本
-- 版本: v1.0
-- 日期: 2026-03-02
-- 说明: sv_project 新增 schedule_date、shoot_status、persona_id；
--       sv_shot 新增 review_status、reviewer_note（分镜审核）
-- ============================================================

-- 1. sv_project 新增每日拍摄字段
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS schedule_date DATE;
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS shoot_status VARCHAR(32);
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS persona_id BIGINT;

COMMENT ON COLUMN sv_project.schedule_date IS '计划拍摄日期（daily 类型使用）';
COMMENT ON COLUMN sv_project.shoot_status IS '拍摄状态：not_started/ready/shooting/shot_done（仅 daily 类型）';
COMMENT ON COLUMN sv_project.persona_id IS '关联人设 ID（daily 类型）';

CREATE INDEX IF NOT EXISTS idx_sv_project_schedule ON sv_project (owner_id, schedule_date) WHERE schedule_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sv_project_persona ON sv_project (persona_id) WHERE persona_id IS NOT NULL;

-- 2. sv_shot 新增分镜审核字段
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS review_status VARCHAR(16) DEFAULT 'pending';
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS reviewer_note TEXT;

COMMENT ON COLUMN sv_shot.review_status IS '审核状态：pending/approved/needs_revision（仅 daily 类型分镜）';
COMMENT ON COLUMN sv_shot.reviewer_note IS '审核/修改备注';
