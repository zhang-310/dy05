-- CONTENT-02：与 Flyway V096 一致
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS related_product_ids JSONB;
COMMENT ON COLUMN sv_project.related_product_ids IS '关联 dy_product.id 列表（JSON 数组）';
