-- ============================================================
-- wecom 模块 - 示例数据
-- ============================================================

INSERT INTO wc_robot_config (owner_id, robot_name, webhook_url, robot_type, status, description, deleted, create_time, update_time)
VALUES
  (1, '运营通知机器人', 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=demo-key-001', 'notification', 1, '用于发送日常运营数据通知', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '告警机器人', 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=demo-key-002', 'alert', 1, '用于发送系统告警信息', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO wc_push_rule (owner_id, robot_id, rule_name, trigger_type, trigger_config, message_template, status, deleted, create_time, update_time)
VALUES
  (1, 1, '每日数据日报', 'schedule', '{"cron":"0 9 * * *"}', '【日报】昨日数据：播放量 {views}，点赞 {likes}，新增粉丝 {fans}', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, 2, '异常流量告警', 'event', '{"event":"traffic_anomaly","threshold":50}', '⚠️ 异常流量告警：账号 {account} 流量波动超过 {threshold}%，请及时关注', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO wc_message_log (owner_id, robot_id, rule_id, message_type, message_content, status, send_time, create_time)
VALUES
  (1, 1, 1, 'text', '【日报】昨日数据：播放量 125000，点赞 8600，新增粉丝 320', 1, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP),
  (1, 2, 2, 'text', '⚠️ 异常流量告警：账号 美食探店达人 流量波动超过 65%，请及时关注', 1, CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP);
