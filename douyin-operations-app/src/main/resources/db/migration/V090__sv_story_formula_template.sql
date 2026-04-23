-- D-5：剧情公式模板（租户自定义 + 与预置 API 合并展示）
CREATE TABLE IF NOT EXISTS sv_story_formula_template (
    id            BIGSERIAL PRIMARY KEY,
    owner_id      BIGINT       NOT NULL,
    code          VARCHAR(64)  NOT NULL,
    name          VARCHAR(256) NOT NULL,
    description   TEXT,
    formula_json  TEXT,
    deleted       INTEGER      NOT NULL DEFAULT 0,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_story_formula_owner ON sv_story_formula_template (owner_id, deleted);

COMMENT ON TABLE sv_story_formula_template IS '短剧剧情公式模板（D-5）';
