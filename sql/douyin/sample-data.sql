-- ============================================================
-- douyin 模块 - 示例数据
-- ============================================================

INSERT INTO douyin_account (user_id, account_name, account_id, follow_count, fan_count, video_count, total_likes, description, status, bind_time, deleted, create_time, update_time)
VALUES
  (1, '美食探店达人', 'dy_food_001', 120, 58000, 86, 320000, '专注美食探店，分享各地美食', 1, CURRENT_TIMESTAMP - INTERVAL '30 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '穿搭种草官', 'dy_fashion_002', 85, 32000, 52, 180000, '每日穿搭分享，时尚不迷路', 1, CURRENT_TIMESTAMP - INTERVAL '20 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO douyin_video (account_id, video_id, title, description, view_count, like_count, share_count, comment_count, download_count, video_type, publish_time, deleted, create_time, update_time)
VALUES
  (1, 'v_001', '探店｜人均50的宝藏日料', '隐藏在巷子里的日料小店，性价比超高', 125000, 8600, 1200, 560, 320, 'normal', CURRENT_TIMESTAMP - INTERVAL '5 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, 'v_002', '这家火锅真的绝了', '牛油锅底配上鲜切毛肚，太香了', 89000, 5200, 800, 380, 210, 'normal', CURRENT_TIMESTAMP - INTERVAL '3 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'v_003', '秋冬穿搭｜小个子显高秘诀', '155也能穿出170的感觉', 67000, 4100, 650, 290, 180, 'normal', CURRENT_TIMESTAMP - INTERVAL '2 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
VALUES
  (1, 1, '吃货小姐姐', 'lifestyle', '热爱美食的90后女生', 'casual', '18-35岁女性', '探店vlog', '美食,探店,种草', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, 2, '时尚博主', 'commerce', '简约风穿搭博主', 'professional', '20-30岁女性', '穿搭教程', '穿搭,时尚,显高', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
