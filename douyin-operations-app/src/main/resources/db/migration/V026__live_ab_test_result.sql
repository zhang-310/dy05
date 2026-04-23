-- V026: live_ab_test_result A/B 测试结果表（与 Entity 对齐）
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

CREATE INDEX IF NOT EXISTS idx_ab_result_experiment ON live_ab_test_result(experiment_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ab_result_owner ON live_ab_test_result(owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ab_result_session ON live_ab_test_result(session_id) WHERE deleted = 0;
