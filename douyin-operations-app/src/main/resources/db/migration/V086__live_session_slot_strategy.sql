-- P2 M-3/M-4/M-6：场次级槽位策略与模板实验标记（编排由应用层读取；占位数据面）
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS slot_rotation_mode VARCHAR(32) DEFAULT 'none';
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS heat_reorder_enabled BOOLEAN DEFAULT false;
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS template_ab_tag VARCHAR(64);

COMMENT ON COLUMN live_session.slot_rotation_mode IS 'none|hourly_block|manual — 时段商品轮换策略';
COMMENT ON COLUMN live_session.heat_reorder_enabled IS '是否启用峰值时段槽位重排（与监控联动时由任务执行）';
COMMENT ON COLUMN live_session.template_ab_tag IS '场次模板/结构实验标签';
