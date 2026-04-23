-- 直播话术完整流程：generate→iterate→violation_check→save
INSERT INTO workflow_definition (workflow_code, workflow_name, description, status, deleted, create_time, update_time)
SELECT 'live_script_full', '直播话术完整流程', '生成→迭代→质检→保存', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM workflow_definition WHERE workflow_code = 'live_script_full' AND deleted = 0);

-- 步骤
INSERT INTO workflow_step (definition_id, step_code, step_name, sequence_no, deleted, create_time, update_time)
SELECT d.id, 'generate', 'AI 生成话术', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workflow_definition d WHERE d.workflow_code = 'live_script_full' AND d.deleted = 0
AND NOT EXISTS (SELECT 1 FROM workflow_step s WHERE s.definition_id = d.id AND s.step_code = 'generate' AND s.deleted = 0);

INSERT INTO workflow_step (definition_id, step_code, step_name, sequence_no, deleted, create_time, update_time)
SELECT d.id, 'iterate', '话术迭代（可选）', 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workflow_definition d WHERE d.workflow_code = 'live_script_full' AND d.deleted = 0
AND NOT EXISTS (SELECT 1 FROM workflow_step s WHERE s.definition_id = d.id AND s.step_code = 'iterate' AND s.deleted = 0);

INSERT INTO workflow_step (definition_id, step_code, step_name, sequence_no, deleted, create_time, update_time)
SELECT d.id, 'violation_check', '违规质检', 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workflow_definition d WHERE d.workflow_code = 'live_script_full' AND d.deleted = 0
AND NOT EXISTS (SELECT 1 FROM workflow_step s WHERE s.definition_id = d.id AND s.step_code = 'violation_check' AND s.deleted = 0);

INSERT INTO workflow_step (definition_id, step_code, step_name, sequence_no, deleted, create_time, update_time)
SELECT d.id, 'save', '保存确认', 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workflow_definition d WHERE d.workflow_code = 'live_script_full' AND d.deleted = 0
AND NOT EXISTS (SELECT 1 FROM workflow_step s WHERE s.definition_id = d.id AND s.step_code = 'save' AND s.deleted = 0);
