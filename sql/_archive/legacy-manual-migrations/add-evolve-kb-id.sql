-- 方案 A：主题池按知识库隔离，支持每 KB 独立进化
-- 执行：psql -d douyin_operations -f sql/_archive/legacy-manual-migrations/add-evolve-kb-id.sql

-- ai_evolve_topic 增加 kb_id（NULL=全局主题，可被任意 KB 使用）
ALTER TABLE ai_evolve_topic ADD COLUMN IF NOT EXISTS kb_id BIGINT;
COMMENT ON COLUMN ai_evolve_topic.kb_id IS '知识库ID，NULL=全局主题';

CREATE INDEX IF NOT EXISTS idx_ai_evolve_topic_kb_priority 
ON ai_evolve_topic (kb_id, priority, status) WHERE deleted = 0;

-- ai_evolve_task 增加 kb_id（标识本轮进化所属知识库）
ALTER TABLE ai_evolve_task ADD COLUMN IF NOT EXISTS kb_id BIGINT;
COMMENT ON COLUMN ai_evolve_task.kb_id IS '进化所属知识库ID';

-- ai_index_queue 增加 target_kb_id（evolved 类型时指定入库目标 KB）
ALTER TABLE ai_index_queue ADD COLUMN IF NOT EXISTS target_kb_id BIGINT;
COMMENT ON COLUMN ai_index_queue.target_kb_id IS 'evolved 类型时：目标知识库ID';
