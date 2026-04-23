-- V130: 话术归因精细化 -- 单段话术执行时间戳
-- 新增 started_at / ended_at 用于精确关联 live_monitor 秒级数据，
-- 替代当前场次级均摊归因算法（live_script_effectiveness 的 totalScore 粒度问题）
ALTER TABLE live_script
    ADD COLUMN IF NOT EXISTS started_at  TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS ended_at    TIMESTAMP WITH TIME ZONE DEFAULT NULL;

COMMENT ON COLUMN live_script.started_at IS '话术实际开始播出时间（由实时面板「下一段」操作写入）';
COMMENT ON COLUMN live_script.ended_at   IS '话术实际结束播出时间（由实时面板「标记完成」操作写入）';

-- 为归因查询创建索引：按场次+时间段查 GMV 增量
CREATE INDEX IF NOT EXISTS idx_live_script_session_time
    ON live_script(session_id, started_at, ended_at)
    WHERE deleted = 0 AND started_at IS NOT NULL;
