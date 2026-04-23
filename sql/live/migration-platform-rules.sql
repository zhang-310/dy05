-- 多平台规则引擎：平台配置 + 平台级违禁词规则
-- Phase 4.3

-- 1. 平台配置表
CREATE TABLE IF NOT EXISTS live_platform (
    id              BIGSERIAL PRIMARY KEY,
    platform_code   VARCHAR(32)  NOT NULL UNIQUE,
    platform_name   VARCHAR(64)  NOT NULL,
    icon_url        VARCHAR(512),
    prompt_template TEXT,
    max_script_length INTEGER DEFAULT 0,
    forbidden_topics TEXT,
    active          INTEGER      NOT NULL DEFAULT 1,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  live_platform IS '直播平台配置';
COMMENT ON COLUMN live_platform.platform_code IS '平台编码: douyin/kuaishou/taobao/jd/xiaohongshu';
COMMENT ON COLUMN live_platform.prompt_template IS '平台级 prompt 模板片段，注入到 system prompt 中';
COMMENT ON COLUMN live_platform.max_script_length IS '平台话术字数上限，0=不限';
COMMENT ON COLUMN live_platform.forbidden_topics IS '平台禁止话题（JSON 数组）';

-- 种子数据
INSERT INTO live_platform (platform_code, platform_name, prompt_template, max_script_length, forbidden_topics) VALUES
('douyin',      '抖音',   '你正在为抖音直播生成话术。抖音用户偏好短平快、节奏感强的内容，注意避免引导站外交易。', 2000, '["站外引流","私下交易"]'),
('kuaishou',    '快手',   '你正在为快手直播生成话术。快手用户偏好真实接地气的风格，注重老铁文化和信任感。', 2000, '["虚假宣传"]'),
('taobao',      '淘宝直播', '你正在为淘宝直播生成话术。淘宝用户注重性价比和商品详情，可适当使用促销话术。', 3000, '[]'),
('xiaohongshu', '小红书',  '你正在为小红书直播生成话术。小红书用户偏好种草风格，注重真实体验分享，避免硬广。', 1500, '["硬广","夸大功效"]'),
('jd',          '京东直播', '你正在为京东直播生成话术。京东用户注重品质和正品保障，可强调品牌和售后服务。', 3000, '[]')
ON CONFLICT (platform_code) DO NOTHING;

-- 2. 平台级违禁词规则表
CREATE TABLE IF NOT EXISTS live_violation_rule (
    id              BIGSERIAL PRIMARY KEY,
    platform_id     BIGINT       NOT NULL REFERENCES live_platform(id),
    word            VARCHAR(128) NOT NULL,
    level           VARCHAR(16)  NOT NULL DEFAULT 'warning',
    reason          VARCHAR(256),
    replacement     VARCHAR(256),
    category        VARCHAR(64),
    active          INTEGER      NOT NULL DEFAULT 1,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_violation_rule_platform ON live_violation_rule(platform_id) WHERE deleted = 0 AND active = 1;
CREATE INDEX IF NOT EXISTS idx_violation_rule_word ON live_violation_rule(word) WHERE deleted = 0;

COMMENT ON TABLE  live_violation_rule IS '平台级违禁词规则';
COMMENT ON COLUMN live_violation_rule.level IS '严重级别: warning/error/ban';
COMMENT ON COLUMN live_violation_rule.category IS '分类: 引流/虚假宣传/敏感词/医疗/绝对化';

-- 抖音特有违禁词示例
INSERT INTO live_violation_rule (platform_id, word, level, reason, replacement, category) VALUES
((SELECT id FROM live_platform WHERE platform_code = 'douyin'), '加微信', 'ban', '抖音禁止站外引流', '关注直播间', '引流'),
((SELECT id FROM live_platform WHERE platform_code = 'douyin'), '私聊我', 'error', '抖音禁止私域引导', '点击购物车', '引流'),
((SELECT id FROM live_platform WHERE platform_code = 'douyin'), '最便宜', 'warning', '绝对化用语', '超值优惠', '绝对化'),
((SELECT id FROM live_platform WHERE platform_code = 'kuaishou'), '点击链接', 'error', '快手限制外链引导', '点击小黄车', '引流'),
((SELECT id FROM live_platform WHERE platform_code = 'xiaohongshu'), '买它', 'warning', '小红书限制硬广话术', '值得入手', '硬广')
ON CONFLICT DO NOTHING;
