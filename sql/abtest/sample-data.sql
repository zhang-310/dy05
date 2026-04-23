-- ============================================================
-- abtest 模块 - 示例数据
-- ============================================================

INSERT INTO ab_experiment (owner_id, name, description, experiment_type, status, start_time, deleted, create_time, update_time)
VALUES
  (1, '商品标题A/B测试', '测试不同标题对点击率的影响', 'copy', 1, CURRENT_TIMESTAMP - INTERVAL '3 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '封面图对比实验', '测试不同封面图对播放量的影响', 'video', 0, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ab_variant (experiment_id, variant_name, variant_type, content, view_count, click_count, conversion_count, conversion_rate, is_winner, deleted, create_time, update_time)
VALUES
  (1, '标题A - 直接描述', 'A', '纯棉短袖T恤 夏季新款', 5200, 680, 120, 2.31, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '标题B - 情感驱动', 'B', '这件T恤让你清凉一夏！', 5100, 820, 165, 3.24, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '封面A - 产品特写', 'A', '产品正面特写图', 0, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '封面B - 场景图', 'B', '模特穿着场景图', 0, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ab_event (experiment_id, variant_id, event_type, user_fingerprint, session_id, create_time)
VALUES
  (1, 1, 'view', 'fp_user_001', 'sess_001', CURRENT_TIMESTAMP - INTERVAL '2 days'),
  (1, 1, 'click', 'fp_user_001', 'sess_001', CURRENT_TIMESTAMP - INTERVAL '2 days'),
  (1, 2, 'view', 'fp_user_002', 'sess_002', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (1, 2, 'click', 'fp_user_002', 'sess_002', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (1, 2, 'conversion', 'fp_user_002', 'sess_002', CURRENT_TIMESTAMP - INTERVAL '1 day');
