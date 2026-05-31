-- TianAPI material import observability.
-- Records each daily/manual run and every category's call usage / stop reason.
CREATE TABLE IF NOT EXISTS tianapi_import_run (
    id                                  BIGSERIAL PRIMARY KEY,
    trigger_type                        VARCHAR(32),
    status                              VARCHAR(32) NOT NULL DEFAULT 'running',
    mode                                VARCHAR(64),
    configured_calls_per_category       INTEGER,
    configured_api_calls_per_category   INTEGER,
    estimated_max_http_calls            INTEGER,
    total_imported                      INTEGER NOT NULL DEFAULT 0,
    total_skipped                       INTEGER NOT NULL DEFAULT 0,
    total_calls                         INTEGER NOT NULL DEFAULT 0,
    total_kb_imported                   INTEGER NOT NULL DEFAULT 0,
    skip_reason                         TEXT,
    error_message                       TEXT,
    started_at                          TIMESTAMP,
    finished_at                         TIMESTAMP,
    create_time                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tianapi_import_run_created
    ON tianapi_import_run(create_time DESC);

CREATE TABLE IF NOT EXISTS tianapi_import_category_stat (
    id                  BIGSERIAL PRIMARY KEY,
    run_id              BIGINT NOT NULL REFERENCES tianapi_import_run(id) ON DELETE CASCADE,
    category            VARCHAR(64) NOT NULL,
    display_name        VARCHAR(128),
    planned_calls       INTEGER NOT NULL DEFAULT 0,
    calls               INTEGER NOT NULL DEFAULT 0,
    imported            INTEGER NOT NULL DEFAULT 0,
    skipped             INTEGER NOT NULL DEFAULT 0,
    empty_responses     INTEGER NOT NULL DEFAULT 0,
    failed_calls        INTEGER NOT NULL DEFAULT 0,
    quota_exhausted     BOOLEAN NOT NULL DEFAULT FALSE,
    stop_reason         TEXT,
    last_error          TEXT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tianapi_import_category_run
    ON tianapi_import_category_stat(run_id, id);
