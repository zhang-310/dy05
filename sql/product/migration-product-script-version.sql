-- SQL Migration for Product Script Version Module
-- Module: product-script-version
-- Date: 2026-03-06
-- This migration creates the product script version management tables

-- Create product_script_version table
CREATE TABLE IF NOT EXISTS product_script_version (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL,
  script_id BIGINT,
  version_number INTEGER NOT NULL DEFAULT 1,
  content TEXT NOT NULL,
  style VARCHAR(64),
  effectiveness_score DECIMAL(5,2) DEFAULT 0,
  usage_count INTEGER DEFAULT 0,
  conversion_rate DECIMAL(5,2) DEFAULT 0,
  likes_count INTEGER DEFAULT 0,
  comments_count INTEGER DEFAULT 0,
  is_active BOOLEAN DEFAULT true,
  is_recommended BOOLEAN DEFAULT false,
  archived BOOLEAN DEFAULT false,
  owner_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  deleted INTEGER DEFAULT 0,

  UNIQUE (product_id, version_number, deleted),
  CONSTRAINT fk_product_script_version_owner FOREIGN KEY (owner_id) REFERENCES auth_user(id)
);

-- Create product_script_snapshot table
CREATE TABLE IF NOT EXISTS product_script_snapshot (
  id BIGSERIAL PRIMARY KEY,
  live_session_id BIGINT NOT NULL,
  product_script_version_id BIGINT NOT NULL,
  content_snapshot TEXT NOT NULL,
  referenced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  owner_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INTEGER DEFAULT 0,

  CONSTRAINT fk_product_script_snapshot_version FOREIGN KEY (product_script_version_id) REFERENCES product_script_version(id),
  CONSTRAINT fk_product_script_snapshot_owner FOREIGN KEY (owner_id) REFERENCES auth_user(id)
);

-- Create indexes for product_script_version table
CREATE INDEX IF NOT EXISTS idx_product_script_version_product_id
  ON product_script_version(product_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_product_script_version_owner_id
  ON product_script_version(owner_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_product_script_version_effectiveness_score
  ON product_script_version(effectiveness_score DESC) WHERE deleted = 0 AND is_active = true;

CREATE INDEX IF NOT EXISTS idx_product_script_version_usage_count
  ON product_script_version(usage_count DESC) WHERE deleted = 0 AND is_active = true;

CREATE INDEX IF NOT EXISTS idx_product_script_version_is_active
  ON product_script_version(is_active) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_product_script_version_is_recommended
  ON product_script_version(is_recommended) WHERE deleted = 0 AND is_active = true;

CREATE INDEX IF NOT EXISTS idx_product_script_version_created_at
  ON product_script_version(created_at DESC) WHERE deleted = 0;

-- Create indexes for product_script_snapshot table
CREATE INDEX IF NOT EXISTS idx_product_script_snapshot_live_session_id
  ON product_script_snapshot(live_session_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_product_script_snapshot_product_script_version_id
  ON product_script_snapshot(product_script_version_id) WHERE deleted = 0;

-- Add table comments
COMMENT ON TABLE product_script_version IS '商品话术版本表：存储产品的各个话术版本及其效果评分';
COMMENT ON TABLE product_script_snapshot IS '商品话术引用快照表：记录直播场次中引用的话术快照';

-- Add column comments
COMMENT ON COLUMN product_script_version.product_id IS '关联产品 ID';
COMMENT ON COLUMN product_script_version.script_id IS '关联话术 ID（如果来自话术库）';
COMMENT ON COLUMN product_script_version.version_number IS '版本号（自动递增）';
COMMENT ON COLUMN product_script_version.content IS '话术内容';
COMMENT ON COLUMN product_script_version.style IS '话术风格分类（激情/温柔/专业/幽默等）';
COMMENT ON COLUMN product_script_version.effectiveness_score IS '效果评分（0-100）';
COMMENT ON COLUMN product_script_version.usage_count IS '使用次数';
COMMENT ON COLUMN product_script_version.conversion_rate IS '转化率（百分比）';
COMMENT ON COLUMN product_script_version.likes_count IS '点赞数';
COMMENT ON COLUMN product_script_version.comments_count IS '评论数';
COMMENT ON COLUMN product_script_version.is_active IS '是否启用';
COMMENT ON COLUMN product_script_version.is_recommended IS '是否推荐';
COMMENT ON COLUMN product_script_version.archived IS '是否归档';
COMMENT ON COLUMN product_script_version.owner_id IS '数据隔离：所有者 ID';
COMMENT ON COLUMN product_script_version.created_at IS '创建时间';
COMMENT ON COLUMN product_script_version.updated_at IS '更新时间';
COMMENT ON COLUMN product_script_version.deleted_at IS '删除时间';
COMMENT ON COLUMN product_script_version.deleted IS '逻辑删除标记（0=未删除, 1=已删除）';

COMMENT ON COLUMN product_script_snapshot.live_session_id IS '直播场次 ID';
COMMENT ON COLUMN product_script_snapshot.product_script_version_id IS '话术版本 ID';
COMMENT ON COLUMN product_script_snapshot.content_snapshot IS '快照内容（引用时的内容）';
COMMENT ON COLUMN product_script_snapshot.referenced_at IS '引用时间';
COMMENT ON COLUMN product_script_snapshot.owner_id IS '数据隔离：所有者 ID';
COMMENT ON COLUMN product_script_snapshot.created_at IS '创建时间';
COMMENT ON COLUMN product_script_snapshot.deleted IS '逻辑删除标记（0=未删除, 1=已删除）';
