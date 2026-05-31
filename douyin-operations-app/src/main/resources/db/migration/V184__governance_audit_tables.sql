-- ============================================
-- Sprint 3: 治理审计基础设施表
-- ============================================

-- 审计事件表
CREATE TABLE IF NOT EXISTS sys_audit_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    action VARCHAR(64) NOT NULL,
    resource VARCHAR(256) NOT NULL DEFAULT '',
    detail TEXT,
    success BOOLEAN NOT NULL DEFAULT true,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_tenant ON sys_audit_event(tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action ON sys_audit_event(action, create_time DESC);

-- 用量台账表
CREATE TABLE IF NOT EXISTS sys_usage_ledger (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    feature_code VARCHAR(64) NOT NULL,
    model VARCHAR(64) DEFAULT 'mock',
    tokens BIGINT NOT NULL DEFAULT 0,
    cost_credits BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_usage_tenant ON sys_usage_ledger(tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_usage_feature ON sys_usage_ledger(feature_code);

-- 产品表（DB 驱动产品目录）
CREATE TABLE IF NOT EXISTS sys_product (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    stage VARCHAR(16) NOT NULL DEFAULT 'preview',
    enabled BOOLEAN NOT NULL DEFAULT true,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);

-- 功能表
CREATE TABLE IF NOT EXISTS sys_feature (
    code VARCHAR(128) PRIMARY KEY,
    product_code VARCHAR(64) NOT NULL REFERENCES sys_product(code),
    name VARCHAR(128) NOT NULL,
    quota_unit VARCHAR(32) DEFAULT 'call',
    monthly_limit BIGINT NOT NULL DEFAULT 1000,
    enabled BOOLEAN NOT NULL DEFAULT true,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);

-- 种子数据：产品目录
INSERT INTO sys_product (code, name, stage) VALUES
    ('douyin-ops', '抖音运营', 'launched'),
    ('video-insight', '短视频拆解分析', 'launched'),
    ('knowledge-base', 'AI 知识库', 'launched'),
    ('digital-human', 'AI 数字人', 'preview'),
    ('drama-ai', '短剧 AI 制作', 'preview'),
    ('shortvideo-maker', '短视频成片创作', 'preview')
ON CONFLICT (code) DO NOTHING;

-- 种子数据：功能定义
INSERT INTO sys_feature (code, product_code, name, quota_unit, monthly_limit) VALUES
    ('douyin-ops.account-mgmt', 'douyin-ops', '账号管理', 'account', 100),
    ('douyin-ops.video-analysis', 'douyin-ops', '视频分析', 'analysis', 1000),
    ('douyin-ops.live-script', 'douyin-ops', '直播话术', 'script', 200),
    ('video-insight.breakdown', 'video-insight', '视频拆解', 'analysis', 500),
    ('knowledge-base.rag', 'knowledge-base', 'RAG 查询', 'query', 5000),
    ('ai.chat', 'douyin-ops', 'AI 对话', 'call', 5000),
    ('ai.generation', 'douyin-ops', 'AI 生成', 'call', 1000)
ON CONFLICT (code) DO NOTHING;
