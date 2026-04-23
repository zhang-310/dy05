-- Phase 4/5 · N-3 受众画像 CSV 归档 + G-2 图谱关系人工校验队列（无 DB 外键）
-- Entity: SvAudienceProfileImport, AiGraphRelationSuggestion

CREATE TABLE IF NOT EXISTS sv_audience_profile_import (
    id           BIGSERIAL PRIMARY KEY,
    owner_id     BIGINT         NOT NULL,
    data_source  VARCHAR(64)    NOT NULL DEFAULT 'third_party_csv',
    payload_json TEXT           NOT NULL,
    row_count    INTEGER        NOT NULL DEFAULT 0,
    create_time  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      INTEGER        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sv_audience_import_owner_time ON sv_audience_profile_import (owner_id, create_time DESC) WHERE deleted = 0;

COMMENT ON TABLE sv_audience_profile_import IS 'N-3：第三方/CSV 导入受众画像快照（JSON 行数组）';

CREATE TABLE IF NOT EXISTS ai_graph_relation_suggestion (
    id                BIGSERIAL PRIMARY KEY,
    owner_id          BIGINT         NOT NULL DEFAULT 0,
    source_entity_key VARCHAR(512)   NOT NULL,
    target_entity_key VARCHAR(512)   NOT NULL,
    relation_type     VARCHAR(64)    NOT NULL,
    confidence        DOUBLE PRECISION NOT NULL DEFAULT 0,
    status            VARCHAR(32)    NOT NULL DEFAULT 'pending',
    evidence_json     TEXT,
    create_time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ai_graph_rel_sug_owner_status ON ai_graph_relation_suggestion (owner_id, status) WHERE deleted = 0;

COMMENT ON TABLE ai_graph_relation_suggestion IS 'G-2：图谱关系建议 / 人工校验队列（弱监督/规则产出）';
