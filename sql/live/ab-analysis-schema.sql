-- A/B 测试闭环反馈结果表
-- Task Q4-3: A/B test analysis results

CREATE TABLE IF NOT EXISTS live_ab_test_result (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT        NOT NULL,
    experiment_key      VARCHAR(100)  NOT NULL,
    variant             VARCHAR(50),
    style               VARCHAR(50),
    effectiveness_score DECIMAL(5,2),
    conversion_rate     DECIMAL(5,4),
    interaction_rate    DECIMAL(5,4),
    sample_size         INTEGER       DEFAULT 0,
    confidence          DECIMAL(5,4),
    owner_id            BIGINT        NOT NULL,
    deleted             INTEGER       NOT NULL DEFAULT 0,
    create_time         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  live_ab_test_result IS 'A/B 测试结果记录';
COMMENT ON COLUMN live_ab_test_result.experiment_key IS '实验唯一标识';
COMMENT ON COLUMN live_ab_test_result.confidence IS '统计置信度';

CREATE INDEX IF NOT EXISTS idx_ab_result_experiment ON live_ab_test_result(experiment_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ab_result_owner      ON live_ab_test_result(owner_id)       WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ab_result_session     ON live_ab_test_result(session_id)     WHERE deleted = 0;
