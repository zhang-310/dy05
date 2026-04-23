-- ============================================================
-- live 模块 - 示例数据
-- ============================================================

INSERT INTO live_session (user_id, account_id, live_title, live_description, scheduled_time, start_time, end_time, viewers, likes, status, deleted, create_time, update_time)
VALUES
  (1, 1, '美食好物专场', '精选各地美食好物，超低价直播间专享', CURRENT_TIMESTAMP + INTERVAL '1 day', NULL, NULL, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, 1, '周末零食大赏', '各种零食试吃测评，边吃边聊', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '3 hours', 12500, 8600, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO live_product (session_id, product_id, product_name, sale_quantity, revenue, position, create_time)
VALUES
  (2, 1, '纯棉短袖T恤', 120, 9588.00, 1, CURRENT_TIMESTAMP),
  (2, 2, '保湿面膜套装', 200, 11980.00, 2, CURRENT_TIMESTAMP),
  (2, 3, '无线蓝牙耳机', 50, 6450.00, 3, CURRENT_TIMESTAMP);

INSERT INTO live_script (session_id, script_content, sequence_no, execution_time, executed, deleted, create_time, update_time)
VALUES
  (2, '大家好，欢迎来到直播间！今天给大家带来超多零食好物~', 1, '00:00:00', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '第一款产品来了！这款T恤真的太舒服了，纯棉面料，夏天穿特别透气', 2, '00:15:00', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '感谢大家的支持，今天的直播就到这里，记得关注我们下次再见！', 3, '02:50:00', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO live_monitor (session_id, timestamp, viewers, likes, comments, shares, product_impressions, create_time)
VALUES
  (2, CURRENT_TIMESTAMP - INTERVAL '2 days', 3200, 1500, 280, 45, 1200, CURRENT_TIMESTAMP),
  (2, CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 hour', 8500, 4200, 650, 120, 3500, CURRENT_TIMESTAMP),
  (2, CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours', 12500, 8600, 980, 210, 5800, CURRENT_TIMESTAMP);
