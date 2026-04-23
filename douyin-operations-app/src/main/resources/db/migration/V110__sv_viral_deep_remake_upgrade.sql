-- P0–P2：爆款深度分析/二创：确认类型独立列、轻量/深度分析分离、步骤摘要
-- 启发式拆分历史 analysis_result：含 viralHypotheses 或 remakeVariableTable → deep，否则 → light

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS confirmed_remake_type VARCHAR(64);
COMMENT ON COLUMN sv_viral_video.confirmed_remake_type IS '运营确认的二创类型，与 remake_suggestions JSON 分离';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS light_analysis_result TEXT;
COMMENT ON COLUMN sv_viral_video.light_analysis_result IS '轻量爆款分析 JSON（元数据 LLM）';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analysis_result TEXT;
COMMENT ON COLUMN sv_viral_video.deep_analysis_result IS 'LF-05 深度分析完整 JSON';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_steps TEXT;
COMMENT ON COLUMN sv_viral_video.deep_analyze_steps IS '深度分析步骤摘要 JSON（排障）';

UPDATE sv_viral_video v
SET deep_analysis_result = v.analysis_result
WHERE v.analysis_result IS NOT NULL
  AND (v.analysis_result LIKE '%viralHypotheses%' OR v.analysis_result LIKE '%remakeVariableTable%');

UPDATE sv_viral_video v
SET light_analysis_result = v.analysis_result
WHERE v.analysis_result IS NOT NULL
  AND v.deep_analysis_result IS NULL;
