-- 主题池归属：支持 知识库 与 抖音账号 归类
-- kb_id NOT NULL, account_id NULL → 归属知识库
-- kb_id NULL, account_id NOT NULL → 归属抖音账号
-- kb_id NULL, account_id NULL → 全局主题
ALTER TABLE ai_evolve_topic ADD COLUMN IF NOT EXISTS account_id BIGINT;
COMMENT ON COLUMN ai_evolve_topic.account_id IS 'douyin_account.id, NULL=not assigned';
CREATE INDEX IF NOT EXISTS idx_ai_evolve_topic_account ON ai_evolve_topic (account_id, priority, status) WHERE deleted = 0;
