-- 账号采集智能输入：记录输入类型和原始输入
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS input_type VARCHAR(32);
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS original_input VARCHAR(1024);
