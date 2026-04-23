-- P2 L-5：分段效果分（代理：按段落长度权重拆分 total_score，JSON 存于 effectiveness）
-- P2 L-7：总分较历史异常跳变标记
ALTER TABLE live_script_effectiveness ADD COLUMN IF NOT EXISTS segment_scores_json TEXT;
ALTER TABLE live_script_effectiveness ADD COLUMN IF NOT EXISTS score_anomaly_flag INTEGER NOT NULL DEFAULT 0;
ALTER TABLE live_script_effectiveness ADD COLUMN IF NOT EXISTS score_anomaly_detail VARCHAR(512);

COMMENT ON COLUMN live_script_effectiveness.segment_scores_json IS 'L-5：段落分 [{segmentIndex,chars,score}]';
COMMENT ON COLUMN live_script_effectiveness.score_anomaly_flag IS 'L-7：1=较上次异常跳变';
COMMENT ON COLUMN live_script_effectiveness.score_anomaly_detail IS 'L-7：异常说明';
