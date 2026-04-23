-- ============================================================
-- shortvideo 模块 - 示例数据
-- ============================================================

INSERT INTO sv_category (owner_id, name, description, sort_order, deleted, create_time, update_time)
VALUES
  (1, '美食探店', '美食相关短视频', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '穿搭分享', '穿搭教程和种草', 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '好物推荐', '产品测评和推荐', 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sv_video (owner_id, account_id, title, description, duration, tags, category_id, view_count, like_count, comment_count, share_count, favorite_count, sync_status, deleted, create_time, update_time)
VALUES
  (1, 1, '探店｜人均50的宝藏日料', '隐藏在巷子里的日料小店', 65, '美食,日料,探店', 1, 125000, 8600, 560, 1200, 3200, 1, 0, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP),
  (1, 1, '这家火锅真的绝了', '牛油锅底配上鲜切毛肚', 48, '火锅,美食,重庆', 1, 89000, 5200, 380, 800, 2100, 1, 0, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),
  (1, 2, '秋冬穿搭｜小个子显高秘诀', '155也能穿出170的感觉', 72, '穿搭,显高,秋冬', 2, 67000, 4100, 290, 650, 1800, 1, 0, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP);

INSERT INTO sv_video_data (video_id, snapshot_date, view_count, like_count, comment_count, share_count, favorite_count, new_followers, view_delta, like_delta, create_time)
VALUES
  (1, CURRENT_DATE - 2, 100000, 7000, 450, 1000, 2800, 320, 25000, 1600, CURRENT_TIMESTAMP),
  (1, CURRENT_DATE - 1, 120000, 8200, 530, 1150, 3100, 180, 20000, 1200, CURRENT_TIMESTAMP),
  (1, CURRENT_DATE, 125000, 8600, 560, 1200, 3200, 85, 5000, 400, CURRENT_TIMESTAMP);

INSERT INTO sv_hot_topic (source, title, description, heat_score, category, related_tags, status, expiry_time, create_time, update_time)
VALUES
  ('douyin', '秋天的第一杯奶茶', '秋季奶茶话题持续火爆', 9500, '美食', '奶茶,秋天,打卡', 1, CURRENT_TIMESTAMP + INTERVAL '7 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('douyin', '显瘦穿搭公式', '小个子穿搭技巧分享', 8200, '时尚', '穿搭,显瘦,小个子', 1, CURRENT_TIMESTAMP + INTERVAL '5 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
