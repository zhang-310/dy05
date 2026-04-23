-- ================================================
-- 直播槽位类型配置表（#18 槽位类型扩展）
-- ================================================
CREATE TABLE IF NOT EXISTS live_slot_type (
    id BIGSERIAL PRIMARY KEY,
    slot_code VARCHAR(32) NOT NULL UNIQUE,
    slot_label VARCHAR(64) NOT NULL,
    default_requirement VARCHAR(128),
    sort_order INTEGER DEFAULT 0,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_slot_type_sort ON live_slot_type(sort_order);

COMMENT ON TABLE live_slot_type IS '直播槽位类型配置表';
COMMENT ON COLUMN live_slot_type.slot_code IS '槽位标识：opening/product/transition/closing/interaction/promotion';
COMMENT ON COLUMN live_slot_type.slot_label IS '展示名称';
COMMENT ON COLUMN live_slot_type.default_requirement IS '默认需求描述';

-- 初始化默认槽位
INSERT INTO live_slot_type (slot_code, slot_label, default_requirement, sort_order, is_enabled, created_by)
VALUES
('opening', '开场', '开场白', 1, TRUE, 0),
('product', '产品介绍', '产品介绍', 2, TRUE, 0),
('transition', '转场', '转场', 3, TRUE, 0),
('closing', '收尾', '收尾', 4, TRUE, 0),
('interaction', '互动引导', '互动引导', 5, TRUE, 0),
('promotion', '促单加赛', '促单加赛', 6, TRUE, 0)
ON CONFLICT (slot_code) DO NOTHING;
