-- 修复 copy_processing 任务名称乱码
UPDATE ai_task_model_config
SET task_name = '文案处理', update_time = CURRENT_TIMESTAMP
WHERE task_code = 'copy_processing' AND deleted = 0;
