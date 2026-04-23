-- 话术生成记录表
CREATE TABLE IF NOT EXISTS sc_script_generation (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_price DECIMAL(10, 2) NOT NULL,
    key_features TEXT NOT NULL,
    duration INTEGER NOT NULL,
    style VARCHAR(50) NOT NULL,
    variant_count INTEGER DEFAULT 3,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 话术版本表
CREATE TABLE IF NOT EXISTS sc_script_variant (
    id BIGSERIAL PRIMARY KEY,
    generation_id BIGINT NOT NULL REFERENCES sc_script_generation(id),
    variant_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    score DECIMAL(3, 1) NOT NULL,
    key_points TEXT,
    likes INTEGER DEFAULT 0,
    uses INTEGER DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sc_generation_owner ON sc_script_generation(owner_id);
CREATE INDEX IF NOT EXISTS idx_sc_variant_generation ON sc_script_variant(generation_id);
