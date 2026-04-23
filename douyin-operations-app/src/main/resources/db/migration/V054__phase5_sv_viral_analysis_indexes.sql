-- V054: Phase 5 数据库补全
-- 1. sv_viral_video 添加 analysis_result 字段（如不存在）
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS analysis_result TEXT;
COMMENT ON COLUMN sv_viral_video.analysis_result IS '结构化爆款分析结果（JSON格式）';

-- 2. 补充缺失索引
CREATE INDEX IF NOT EXISTS idx_graph_edge_owner ON ai_graph_edge(owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_search_log_create ON ai_search_log(create_time);
CREATE INDEX IF NOT EXISTS idx_ab_test_owner ON live_script_ab_test(owner_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_ab_test_script ON live_script_ab_test(script_id, deleted);
CREATE INDEX IF NOT EXISTS idx_remake_owner ON sv_remake_template(owner_id, deleted);
