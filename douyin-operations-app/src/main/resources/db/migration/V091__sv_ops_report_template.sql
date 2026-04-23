-- N-6：运营报表模板（仪表盘摘要 + 后续 CSV 导出）
CREATE TABLE IF NOT EXISTS sv_ops_report_template (
    id             BIGSERIAL PRIMARY KEY,
    owner_id       BIGINT       NOT NULL,
    name           VARCHAR(256) NOT NULL,
    definition_json TEXT,
    deleted        INTEGER      NOT NULL DEFAULT 0,
    create_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_ops_report_owner ON sv_ops_report_template (owner_id, deleted);
