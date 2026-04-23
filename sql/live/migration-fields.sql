-- ============================================================
-- live 模块 - 补齐字段（对照文档 02-数据库设计）
-- 执行：psql -U postgres -d douyin_operations -f sql/live/migration-fields.sql
-- ============================================================

-- live_session 补齐
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS session_cover VARCHAR(512);
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS script_style VARCHAR(64) DEFAULT 'professional';
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS readiness_check VARCHAR(512);
COMMENT ON COLUMN live_session.session_cover IS '直播封面图 URL';
COMMENT ON COLUMN live_session.script_style IS '话术风格：professional/friendly/passionate/seeding/promotion';
COMMENT ON COLUMN live_session.readiness_check IS '开播准备清单结果（JSON）';

-- live_product 补齐
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS script_source VARCHAR(32) DEFAULT 'session';
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS product_script_id BIGINT;
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN live_product.script_source IS '话术来源：product/session';
COMMENT ON COLUMN live_product.product_script_id IS '引用的产品话术 ID（dy_product_script.id）';
COMMENT ON COLUMN live_product.deleted IS '逻辑删除：0=正常 1=已删除';

-- live_script 补齐（migration-persona 已加 script_type/style/ai_generated）
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS product_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ai_call_log_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS generation_status VARCHAR(16) DEFAULT 'success';
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS violation_checked INTEGER DEFAULT 0;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS violation_result TEXT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS viewer_delta INTEGER;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS interaction_delta INTEGER;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS effectiveness_score DECIMAL(5,2);
COMMENT ON COLUMN live_script.product_id IS '关联产品 ID（产品话术时填写）';
COMMENT ON COLUMN live_script.ai_call_log_id IS '关联 AI 调用日志 ID（效果归因）';
COMMENT ON COLUMN live_script.generation_status IS '生成状态：success/failed/pending';
COMMENT ON COLUMN live_script.violation_checked IS '是否已做违规检测：0=否 1=是';
COMMENT ON COLUMN live_script.violation_result IS '违规检测结果（JSON）';
COMMENT ON COLUMN live_script.viewer_delta IS '执行后 30s 观众变化（效果归因）';
COMMENT ON COLUMN live_script.interaction_delta IS '执行后 30s 互动变化（效果归因）';
COMMENT ON COLUMN live_script.effectiveness_score IS '效果评分';

CREATE INDEX IF NOT EXISTS idx_live_script_product_id ON live_script(product_id) WHERE product_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_live_script_effectiveness ON live_script(effectiveness_score DESC) WHERE effectiveness_score IS NOT NULL;
