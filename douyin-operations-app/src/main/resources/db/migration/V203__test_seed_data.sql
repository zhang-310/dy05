-- V203: 集成测试种子 — 测试数据
INSERT INTO live_session (id, user_id, account_id, live_title, status, scheduled_time, deleted)
VALUES (-1, 1, 1, '[TEST] 测试场次-GMV千万', 2, now() - interval '1 day', 0)
ON CONFLICT DO NOTHING;

INSERT INTO live_script (id, session_id, script_content, script_type, sequence_no, user_id, deleted)
VALUES (-1, -1, '[TEST] 欢迎来到直播间!', 'opening', 1, 1, 0)
ON CONFLICT DO NOTHING;
