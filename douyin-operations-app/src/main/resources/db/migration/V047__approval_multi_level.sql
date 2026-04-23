-- V047: 多级审批支持（Phase 2.8）
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS approval_level INTEGER DEFAULT 1;
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS max_level INTEGER DEFAULT 1;
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS current_approver_id BIGINT;
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS auto_approve_rule VARCHAR(64);
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS owner_id BIGINT;
