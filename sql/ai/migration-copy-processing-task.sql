-- ============================================================
-- 新增「文案处理」任务配置，供文案模块 AI 生成使用
-- 执行：已有数据库执行此脚本，新库由 task-model-config-migration 已包含
-- ============================================================

INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, timeout_seconds, max_retries, sort_order, status, deleted, create_time, update_time)
SELECT 'copy_processing', '文案处理', 'copy',
  (SELECT id FROM ai_model WHERE status=1 AND deleted=0 ORDER BY id LIMIT 1),
  NULL, NULL,
  60, 1, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_task_model_config WHERE task_code='copy_processing' AND deleted=0)
  AND EXISTS (SELECT 1 FROM ai_model WHERE status=1 AND deleted=0);
