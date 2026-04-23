-- HyDE：任务模型 kb_hyde（见 HydeExpansionServiceImpl、app.ai.search.hyde）
INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, timeout_seconds, max_retries, sort_order, status, deleted, create_time, update_time)
SELECT 'kb_hyde', '知识库 HyDE 假设文档', 'text_generate',
  (SELECT id FROM ai_model WHERE status = 1 AND deleted = 0 ORDER BY id LIMIT 1),
  NULL, NULL,
  20, 1, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_task_model_config WHERE task_code = 'kb_hyde' AND deleted = 0)
  AND EXISTS (SELECT 1 FROM ai_model WHERE status = 1 AND deleted = 0);
