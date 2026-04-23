-- 话术质量评分表（合规+流畅+吸引力三维评分）
CREATE TABLE IF NOT EXISTS live_script_quality_score (
    id                BIGSERIAL      PRIMARY KEY,
    script_id         BIGINT         NOT NULL,
    session_id        BIGINT,
    compliance_score  NUMERIC(5,2)   NOT NULL DEFAULT 0,
    fluency_score     NUMERIC(5,2)   NOT NULL DEFAULT 0,
    engagement_score  NUMERIC(5,2)   NOT NULL DEFAULT 0,
    total_quality_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    details_json      JSONB,
    owner_id          BIGINT,
    deleted           INTEGER        NOT NULL DEFAULT 0,
    create_time       TIMESTAMP      DEFAULT NOW(),
    update_time       TIMESTAMP      DEFAULT NOW()
);

COMMENT ON TABLE live_script_quality_score IS '话术质量评分（合规/流畅/吸引力）';
COMMENT ON COLUMN live_script_quality_score.compliance_score IS '合规评分 0-100';
COMMENT ON COLUMN live_script_quality_score.fluency_score IS '流畅度评分 0-100';
COMMENT ON COLUMN live_script_quality_score.engagement_score IS '吸引力评分 0-100';
COMMENT ON COLUMN live_script_quality_score.total_quality_score IS '综合质量评分 0-100';
COMMENT ON COLUMN live_script_quality_score.details_json IS '评分明细 JSON（各维度扣分项）';

CREATE INDEX IF NOT EXISTS idx_live_quality_score_script ON live_script_quality_score(script_id, deleted);
CREATE INDEX IF NOT EXISTS idx_live_quality_score_session ON live_script_quality_score(session_id, deleted);
CREATE INDEX IF NOT EXISTS idx_live_quality_score_owner ON live_script_quality_score(owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_live_quality_score_total ON live_script_quality_score(total_quality_score DESC);
