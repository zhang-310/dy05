-- 五位主播人设配置表
CREATE TABLE IF NOT EXISTS ai_host_persona (
    id              BIGSERIAL       PRIMARY KEY,
    host_code       VARCHAR(32)     NOT NULL,
    host_name       VARCHAR(64)     NOT NULL,
    age             INTEGER,
    orientation     VARCHAR(16)     NOT NULL DEFAULT 'C',
    positioning     VARCHAR(256),
    content_matrix  TEXT,
    ai_priorities   TEXT,
    style_vector    TEXT,
    bayes_factors   TEXT,
    flow_phase      INTEGER         DEFAULT 0,
    sort_order      INTEGER         DEFAULT 0,
    status          INTEGER         NOT NULL DEFAULT 1,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_host_persona_code ON ai_host_persona(host_code) WHERE deleted = 0;
