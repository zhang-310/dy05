-- 话术分析查询优化索引
-- 补充缺失的组合索引和过滤索引

-- 场次+审核状态 组合索引（按场次筛选审核状态）
CREATE INDEX IF NOT EXISTS idx_live_script_session_approval
    ON live_script(session_id, approval_status) WHERE deleted = 0;

-- 违规检测标记索引（批量扫描未检测话术）
CREATE INDEX IF NOT EXISTS idx_live_script_violation_checked
    ON live_script(violation_checked) WHERE deleted = 0 AND violation_checked = 0;

-- 直播形式+状态 组合索引（按格式筛选场次列表）
CREATE INDEX IF NOT EXISTS idx_live_session_format_status
    ON live_session(live_format, status) WHERE deleted = 0;
