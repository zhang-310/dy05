-- 直播实时辅助面板数据表迁移脚本
-- 作成日期: 2026-03-06
-- 版本: 1.0

-- ========== 直播话术段落表 ==========
-- 记录直播场次中的话术段落，支持话术导航和倒计时
CREATE TABLE IF NOT EXISTS live_session_script_slot (
  id BIGSERIAL PRIMARY KEY COMMENT '主键',
  live_session_id BIGINT NOT NULL COMMENT '直播场次 ID',
  slot_index INTEGER NOT NULL COMMENT '段落序号（从 0 开始）',
  script_version_id BIGINT COMMENT '话术版本 ID（可关联 ProductScriptVersion）',
  content TEXT NOT NULL COMMENT '话术内容',
  duration_seconds INTEGER DEFAULT 120 COMMENT '建议讲解时长（秒）',
  script_type VARCHAR(32) COMMENT '类型（opening/product/discount/closing/emotional）',
  style VARCHAR(64) COMMENT '风格（enthusiastic/professional/gentle/humorous）',
  is_current BOOLEAN DEFAULT false COMMENT '是否当前段落',
  is_completed BOOLEAN DEFAULT false COMMENT '是否已讲解完成',
  started_at TIMESTAMP COMMENT '开始讲解时间',
  completed_at TIMESTAMP COMMENT '完成讲解时间',
  owner_id BIGINT NOT NULL COMMENT '所有者 ID（数据隔离）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted INTEGER DEFAULT 0 COMMENT '逻辑删除标记（0=有效，1=已删除）'
);

-- 创建索引以优化常用查询
CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_session_id
  ON live_session_script_slot(live_session_id);

CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_session_slot
  ON live_session_script_slot(live_session_id, slot_index);

CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_is_current
  ON live_session_script_slot(live_session_id, is_current);

CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_owner_id
  ON live_session_script_slot(owner_id);

-- 表注释
COMMENT ON TABLE live_session_script_slot IS '直播话术段落表 - 记录直播场次中的话术段落信息，支持话术导航、倒计时和完成状态跟踪';

-- ========== 直播实时数据表 ==========
-- 存储直播场次的实时数据（观众、点赞、评论、销售等）
CREATE TABLE IF NOT EXISTS live_session_realtime_data (
  id BIGSERIAL PRIMARY KEY COMMENT '主键',
  live_session_id BIGINT NOT NULL COMMENT '直播场次 ID',
  watched_count INTEGER DEFAULT 0 COMMENT '累计观看人数',
  viewer_count INTEGER DEFAULT 0 COMMENT '在线观众数',
  like_count INTEGER DEFAULT 0 COMMENT '点赞总数',
  comment_count INTEGER DEFAULT 0 COMMENT '评论总数',
  share_count INTEGER DEFAULT 0 COMMENT '分享总数',
  follow_count INTEGER DEFAULT 0 COMMENT '新增关注数',
  gift_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '礼物金额总额（元）',
  product_click_count INTEGER DEFAULT 0 COMMENT '产品点击数',
  product_purchase_count INTEGER DEFAULT 0 COMMENT '商品购买数',
  product_purchase_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '商品购买总金额（元）',
  current_slot_index INTEGER COMMENT '当前话术段落序号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted INTEGER DEFAULT 0 COMMENT '逻辑删除标记（0=有效，1=已删除）'
);

-- 创建索引以优化查询
CREATE INDEX IF NOT EXISTS idx_live_session_realtime_data_session_id
  ON live_session_realtime_data(live_session_id);

-- 表注释
COMMENT ON TABLE live_session_realtime_data IS '直播实时数据表 - 存储直播场次的实时统计数据，包括观众、点赞、评论和商品转化数据';

-- ========== 触发器：自动更新 updated_at ==========
-- live_session_script_slot 表的 updated_at 更新触发器
CREATE OR REPLACE FUNCTION update_live_session_script_slot_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_live_session_script_slot_updated_at ON live_session_script_slot;
CREATE TRIGGER trigger_live_session_script_slot_updated_at
  BEFORE UPDATE ON live_session_script_slot
  FOR EACH ROW
  EXECUTE FUNCTION update_live_session_script_slot_updated_at();

-- live_session_realtime_data 表的 updated_at 更新触发器
CREATE OR REPLACE FUNCTION update_live_session_realtime_data_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_live_session_realtime_data_updated_at ON live_session_realtime_data;
CREATE TRIGGER trigger_live_session_realtime_data_updated_at
  BEFORE UPDATE ON live_session_realtime_data
  FOR EACH ROW
  EXECUTE FUNCTION update_live_session_realtime_data_updated_at();

-- 版本号和生成时间
-- Version: 1.0
-- Generated: 2026-03-06
