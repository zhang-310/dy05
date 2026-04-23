-- 产品话术表
CREATE TABLE IF NOT EXISTS dy_product_script (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    script_type VARCHAR(32) NOT NULL,
    script_content TEXT NOT NULL,
    persona_id BIGINT,
    style VARCHAR(32),
    version INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT false,
    duration INTEGER,
    token_usage INTEGER,
    created_by BIGINT,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_script_product_id ON dy_product_script(product_id);
CREATE INDEX idx_product_script_type ON dy_product_script(script_type);
CREATE INDEX idx_product_script_active ON dy_product_script(is_active);
CREATE INDEX idx_product_script_deleted ON dy_product_script(deleted);

COMMENT ON TABLE dy_product_script IS '产品话术表';
COMMENT ON COLUMN dy_product_script.product_id IS '产品ID';
COMMENT ON COLUMN dy_product_script.script_type IS '话术类型：seed(种草)/promotion(促销)/formal(正式)';
COMMENT ON COLUMN dy_product_script.script_content IS '话术内容';
COMMENT ON COLUMN dy_product_script.persona_id IS '人设ID';
COMMENT ON COLUMN dy_product_script.style IS '话术风格';
COMMENT ON COLUMN dy_product_script.version IS '版本号';
COMMENT ON COLUMN dy_product_script.is_active IS '是否启用';
COMMENT ON COLUMN dy_product_script.duration IS '时长（秒）';
COMMENT ON COLUMN dy_product_script.token_usage IS 'Token使用量';
COMMENT ON COLUMN dy_product_script.created_by IS '创建人';
COMMENT ON COLUMN dy_product_script.deleted IS '软删除标记';
