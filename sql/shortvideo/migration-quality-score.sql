-- 成片质量评分
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS quality_score DOUBLE PRECISION;
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS quality_label VARCHAR(20);
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS quality_report_json TEXT;
