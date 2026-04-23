-- ============================================================
-- ai_task_model_config 任务级模型配置迁移
-- 版本：1.0 | 更新日期：2026-02-28
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_task_model_config (
    id                      BIGSERIAL       PRIMARY KEY,
    task_code               VARCHAR(64)     NOT NULL,                           -- 任务编码：knowledge_evolve, evolve_expand 等
    task_name              VARCHAR(128)    NOT NULL,                           -- 任务名称
    task_group             VARCHAR(32)     NOT NULL DEFAULT 'evolve',          -- 分组：text_generate/embed/rerank/media/evolve
    primary_model_id        BIGINT,                                             -- 主模型 ID（关联 ai_model.id）
    fallback_model_id       BIGINT,                                             -- 备用模型 1 ID
    fallback2_model_id      BIGINT,                                             -- 备用模型 2 ID
    timeout_seconds         INTEGER,                                            -- 任务级超时（秒）
    max_retries            INTEGER         NOT NULL DEFAULT 1,                 -- 每层最大重试次数
    sort_order             INTEGER         NOT NULL DEFAULT 0,                 -- 排序
    status                  INTEGER         NOT NULL DEFAULT 1,                 -- 1=启用 0=禁用
    deleted                 INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_task_model_config_task ON ai_task_model_config (task_code) WHERE deleted = 0;

COMMENT ON TABLE ai_task_model_config IS '任务-模型映射：每个 AI 任务绑定主/备模型';
COMMENT ON COLUMN ai_task_model_config.task_code IS '任务编码：knowledge_evolve, evolve_expand';
COMMENT ON COLUMN ai_task_model_config.primary_model_id IS '主模型 ID';
COMMENT ON COLUMN ai_task_model_config.fallback_model_id IS '备用模型 1 ID';
COMMENT ON COLUMN ai_task_model_config.fallback2_model_id IS '备用模型 2 ID';
COMMENT ON COLUMN ai_task_model_config.timeout_seconds IS '任务级超时（秒），进化建议 360';

-- 默认任务配置（primary/fallback 需在管理端配置或手动更新 ai_model.id）
-- query_rewrite：检索查询改写，优先使用 task 配置，否则回退到任意可用模型
INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, timeout_seconds, max_retries, sort_order, status, deleted, create_time, update_time)
SELECT 'query_rewrite', '检索查询改写', 'text_generate',
  (SELECT id FROM ai_model WHERE status=1 AND deleted=0 ORDER BY id LIMIT 1),
  NULL, NULL,
  15, 1, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_task_model_config WHERE task_code='query_rewrite' AND deleted=0);

-- short_video_script：短视频脚本/文案/分镜/标题生成
INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, timeout_seconds, max_retries, sort_order, status, deleted, create_time, update_time)
SELECT 'short_video_script', '短视频脚本生成', 'shortvideo',
  (SELECT id FROM ai_model WHERE status=1 AND deleted=0 ORDER BY id LIMIT 1),
  NULL, NULL,
  60, 1, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_task_model_config WHERE task_code='short_video_script' AND deleted=0)
  AND EXISTS (SELECT 1 FROM ai_model WHERE status=1 AND deleted=0);

-- copy_processing：文案处理（文案库 AI 生成、创意工坊等）
INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, timeout_seconds, max_retries, sort_order, status, deleted, create_time, update_time)
SELECT 'copy_processing', '文案处理', 'copy',
  (SELECT id FROM ai_model WHERE status=1 AND deleted=0 ORDER BY id LIMIT 1),
  NULL, NULL,
  60, 1, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_task_model_config WHERE task_code='copy_processing' AND deleted=0)
  AND EXISTS (SELECT 1 FROM ai_model WHERE status=1 AND deleted=0);
