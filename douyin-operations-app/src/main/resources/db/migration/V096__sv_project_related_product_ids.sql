-- CONTENT-02：短视频项目关联商品 ID 列表（JSON 数组），供 AI 文案等注入产品上下文
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS related_product_ids JSONB;
COMMENT ON COLUMN sv_project.related_product_ids IS '关联 dy_product.id 列表（JSON 数组），owner 同款隔离';
