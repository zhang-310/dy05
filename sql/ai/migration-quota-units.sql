-- AI 配额细粒度计费：used_units 支持小数（引用=0，模板=0.5，AI=1）
ALTER TABLE ai_call_quota ADD COLUMN IF NOT EXISTS used_units DECIMAL(10,2) DEFAULT 0;
COMMENT ON COLUMN ai_call_quota.used_units IS '已用额度（支持小数：引用0/模板0.5/AI生成1）';
