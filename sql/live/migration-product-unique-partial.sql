-- 将 live_product 唯一约束改为部分索引（仅 deleted=0）
-- 允许软删除后重新添加同一产品，避免 batch-add 报 duplicate key
DROP INDEX IF EXISTS uk_live_product_session_product;
CREATE UNIQUE INDEX uk_live_product_session_product
    ON live_product(session_id, product_id) WHERE deleted = 0;
