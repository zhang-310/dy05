-- ============================================================
-- 迁移：补充资源管理中缺失的 API 和按钮
-- 用法：docker cp sql/auth/migration-add-all-apis.sql dy-postgres:/tmp/
--       docker exec dy-postgres psql -U postgres -d douyin_operations -f /tmp/migration-add-all-apis.sql
-- ============================================================

-- 修正：Log API 路径（旧 list 改为 page）
UPDATE auth_resource SET resource_code = '/api/v1/log/operation/page' WHERE resource_code = '/api/v1/log/operation/list' AND deleted = 0;
UPDATE auth_resource SET resource_code = '/api/v1/log/system/page' WHERE resource_code = '/api/v1/log/system/list' AND deleted = 0;

-- Auth 补充
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/user/online', '在线用户', 'auth', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/auth/user/online' AND deleted = 0);

-- Organization -> auth
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'auth', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/organization/my','我的机构','GET'),
       ('/api/v1/organization/create','创建机构','POST'),
       ('/api/v1/organization/update','更新机构','POST'),
       ('/api/v1/organization/members','机构成员','GET'),
       ('/api/v1/organization/invite','邀请达人','POST'),
       ('/api/v1/organization/remove','移除成员','POST'),
       ('/api/v1/organization/invitations','待处理邀请','GET'),
       ('/api/v1/organization/invitation/accept','接受邀请','POST'),
       ('/api/v1/organization/invitation/reject','拒绝邀请','POST'),
       ('/api/v1/organization/search-talents','搜索达人','GET')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Log 补充（修正路径：后端为 operation/page、system/page，非 list）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/operation/page', '操作日志列表', 'log', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/log/operation/page' AND deleted = 0);
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/system/page', '系统日志列表', 'log', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/log/system/page' AND deleted = 0);
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/operation/export', '导出操作日志', 'log', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/log/operation/export' AND deleted = 0);
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/system/export', '导出系统日志', 'log', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/log/system/export' AND deleted = 0);

-- Douyin 补充
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'douyin', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/douyin/account/delete','删除账号','POST'),
       ('/api/v1/douyin/account/statistics','账号统计','POST'),
       ('/api/v1/douyin/video/get','视频详情','POST'),
       ('/api/v1/douyin/video/save','保存视频','POST'),
       ('/api/v1/douyin/video/sync','同步视频','POST'),
       ('/api/v1/douyin/persona/get','人设详情','POST'),
       ('/api/v1/douyin/persona/delete','删除人设','POST'),
       ('/api/v1/douyin/persona/set-default','设默认人设','POST'),
       ('/api/v1/douyin/persona/get-default','获取默认人设','POST'),
       ('/api/v1/douyin/persona/templates','人设模板','POST'),
       ('/api/v1/douyin/fan-profile','粉丝画像','GET'),
       ('/api/v1/douyin/oauth/authorize-url','授权URL','GET'),
       ('/api/v1/douyin/oauth/callback','OAuth回调','GET'),
       ('/api/v1/douyin/oauth/refresh-token','刷新Token','POST')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Copy 补充
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'copy', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/copy' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/copy/library/get','文案详情'),
       ('/api/v1/copy/library/save','保存文案'),
       ('/api/v1/copy/library/delete','删除文案'),
       ('/api/v1/copy/library/update-status','更新状态'),
       ('/api/v1/copy/library/increment-use-count','递增使用'),
       ('/api/v1/copy/approval/search','审批搜索'),
       ('/api/v1/copy/approval/get','审批详情'),
       ('/api/v1/copy/approval/save','保存审批'),
       ('/api/v1/copy/template/search','模板搜索'),
       ('/api/v1/copy/template/get','模板详情'),
       ('/api/v1/copy/template/save','保存模板'),
       ('/api/v1/copy/template/delete','删除模板')
     ) AS r(c,n)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Script 补充 -> /live
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'script', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/script/get','话术详情','POST'),
       ('/api/v1/script/delete','删除话术','POST'),
       ('/api/v1/script/use-count','使用次数','POST'),
       ('/api/v1/script/violation/check','违规检测','POST'),
       ('/api/v1/script/violation/public/list','公共违规词','POST'),
       ('/api/v1/script/violation/suggest-replacement','替换建议','POST'),
       ('/api/v1/script/template/search','模板搜索','POST'),
       ('/api/v1/script/template/get','模板详情','POST'),
       ('/api/v1/script/template/save','保存模板','POST'),
       ('/api/v1/script/template/delete','删除模板','POST'),
       ('/api/v1/script/template/use-count','模板使用','POST'),
       ('/api/v1/script/template/by-scene','按场景模板','GET'),
       ('/api/v1/script/user-violation/search','用户违规词','POST'),
       ('/api/v1/script/user-violation/get','违规词详情','POST'),
       ('/api/v1/script/user-violation/save','保存违规词','POST'),
       ('/api/v1/script/user-violation/delete','删除违规词','POST'),
       ('/api/v1/script/user-violation/active','有效违规词','GET'),
       ('/api/v1/script/admin/violation/list','违规词列表','POST'),
       ('/api/v1/script/admin/violation/save','保存违规词','POST'),
       ('/api/v1/script/admin/violation/delete','删除违规词','POST'),
       ('/api/v1/script/admin/violation/active','启用违规词','GET')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Live 补充
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'live', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/live/session/get','场次详情','POST'),
       ('/api/v1/live/session/delete','删除场次','POST'),
       ('/api/v1/live/session/status','场次状态','POST'),
       ('/api/v1/live/session/viewers','观看人数','POST'),
       ('/api/v1/live/session/likes','点赞数','POST'),
       ('/api/v1/live/session/trend','趋势分析','POST'),
       ('/api/v1/live/product/search','商品搜索','POST'),
       ('/api/v1/live/product/get','商品详情','POST'),
       ('/api/v1/live/product/save','保存商品','POST'),
       ('/api/v1/live/product/delete','删除商品','POST'),
       ('/api/v1/live/product/by-session','场次商品','POST'),
       ('/api/v1/live/script/search','话术搜索','POST'),
       ('/api/v1/live/script/get','话术详情','POST'),
       ('/api/v1/live/script/save','保存话术','POST'),
       ('/api/v1/live/script/delete','删除话术','POST'),
       ('/api/v1/live/script/by-session','场次话术','POST'),
       ('/api/v1/live/script/executed','执行状态','POST'),
       ('/api/v1/live/monitor/search','监控搜索','POST'),
       ('/api/v1/live/monitor/save','保存监控','POST'),
       ('/api/v1/live/monitor/by-session','场次监控','POST'),
       ('/api/v1/live/monitor/push','推送监控','POST'),
       ('/api/v1/live/ai/generate-opening','开场话术','POST'),
       ('/api/v1/live/ai/generate-product','商品话术','POST'),
       ('/api/v1/live/ai/generate-transition','过渡话术','POST'),
       ('/api/v1/live/ai/generate-closing','收尾话术','POST'),
       ('/api/v1/live/ai/generate-full','完整话术','POST'),
       ('/api/v1/live/ai/check-violation','违规检测','POST'),
       ('/api/v1/live/ai/generate-product-script','产品话术','POST'),
       ('/api/v1/live/data/session','场次数据','GET'),
       ('/api/v1/live/data/session/save','保存场次数据','POST'),
       ('/api/v1/live/data/session/sync','同步场次','POST'),
       ('/api/v1/live/data/product','商品数据','GET'),
       ('/api/v1/live/data/product/save','保存商品数据','POST')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Product 补充
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'product', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/product' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/product/get','商品详情','POST'),
       ('/api/v1/product/delete','删除商品','POST'),
       ('/api/v1/product/update-inventory','更新库存','POST'),
       ('/api/v1/product/publish','上架','POST'),
       ('/api/v1/product/unpublish','下架','POST'),
       ('/api/v1/product/set-featured','设推荐','POST'),
       ('/api/v1/product/sales-history/search','销售历史','POST'),
       ('/api/v1/product/sales-history/get','销售详情','POST'),
       ('/api/v1/product/sales-history/save','保存销售','POST'),
       ('/api/v1/product/sales-history/total-sales-amount','累计金额','GET'),
       ('/api/v1/product/sales-history/total-sales-quantity','累计销量','GET'),
       ('/api/v1/product/script/save','保存话术','POST')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Shortvideo 补充 -> /shortvideo
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'shortvideo', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/shortvideo/content/search','视频搜索'),
       ('/api/v1/shortvideo/content/get','视频详情'),
       ('/api/v1/shortvideo/content/save','保存视频'),
       ('/api/v1/shortvideo/content/delete','删除视频'),
       ('/api/v1/shortvideo/content/increment-view-count','播放量'),
       ('/api/v1/shortvideo/category/list','分类列表'),
       ('/api/v1/shortvideo/category/get','分类详情'),
       ('/api/v1/shortvideo/category/save','保存分类'),
       ('/api/v1/shortvideo/category/delete','删除分类'),
       ('/api/v1/shortvideo/comment/search','评论搜索'),
       ('/api/v1/shortvideo/comment/get','评论详情'),
       ('/api/v1/shortvideo/comment/save','保存评论'),
       ('/api/v1/shortvideo/comment/delete','删除评论'),
       ('/api/v1/shortvideo/comment/increment-like-count','评论点赞'),
       ('/api/v1/shortvideo/script-template/search','脚本模板搜索'),
       ('/api/v1/shortvideo/script-template/get','脚本模板详情'),
       ('/api/v1/shortvideo/script-template/save','保存脚本模板'),
       ('/api/v1/shortvideo/script-template/delete','删除脚本模板'),
       ('/api/v1/shortvideo/script-template/use-count','模板使用'),
       ('/api/v1/shortvideo/script-template/by-scene','按场景模板'),
       ('/api/v1/shortvideo/viral/list','爆款列表'),
       ('/api/v1/shortvideo/viral/collect','收藏爆款'),
       ('/api/v1/shortvideo/viral/get','爆款详情'),
       ('/api/v1/shortvideo/viral/delete','删除收藏'),
       ('/api/v1/shortvideo/viral/analyze','爆款分析'),
       ('/api/v1/shortvideo/viral/replicate','爆款复刻'),
       ('/api/v1/shortvideo/viral/recommended','推荐爆款'),
       ('/api/v1/shortvideo/ai/generate-copy','AI文案'),
       ('/api/v1/shortvideo/ai/generate-script','AI脚本'),
       ('/api/v1/shortvideo/ai/generate-title','AI标题'),
       ('/api/v1/shortvideo/ai/generate-plan','AI方案')
     ) AS r(c,n)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- AI 补充
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'ai', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/ai/model/get','模型详情','POST'),
       ('/api/v1/ai/model/delete','删除模型','POST'),
       ('/api/v1/ai/task/get','任务详情','POST'),
       ('/api/v1/ai/task/complete','完成任务','POST'),
       ('/api/v1/ai/prompt/save','保存Prompt','POST'),
       ('/api/v1/ai/prompt/delete','删除Prompt','POST'),
       ('/api/v1/ai/knowledge/get','知识详情','POST'),
       ('/api/v1/ai/knowledge/delete','删除知识','POST'),
       ('/api/v1/ai/knowledge/status','知识状态','POST'),
       ('/api/v1/ai/knowledge-base/create','创建知识库','POST'),
       ('/api/v1/ai/knowledge-base/list','知识库列表','GET'),
       ('/api/v1/ai/media/image/text2img','文生图','POST'),
       ('/api/v1/ai/media/image/img2img','图生图','POST'),
       ('/api/v1/ai/media/image/edit','图像编辑','POST'),
       ('/api/v1/ai/media/image/history','图像历史','GET'),
       ('/api/v1/ai/media/tts/generate','TTS生成','POST'),
       ('/api/v1/ai/media/tts/voices','音色列表','GET'),
       ('/api/v1/ai/media/tts/history','TTS历史','GET'),
       ('/api/v1/ai/media/video/trim','视频剪辑','POST'),
       ('/api/v1/ai/media/video/merge','视频合并','POST'),
       ('/api/v1/ai/media/video/subtitle','添加字幕','POST'),
       ('/api/v1/ai/media/video/music','添加音乐','POST'),
       ('/api/v1/ai/media/video/transcode','视频转码','POST'),
       ('/api/v1/ai/media/video/auto-compose','自动成片','POST'),
       ('/api/v1/ai/evolution/viral/list','爆款拆解列表','POST'),
       ('/api/v1/ai/evolution/viral/get','爆款拆解详情','POST'),
       ('/api/v1/ai/evolution/viral/trigger','触发拆解','POST'),
       ('/api/v1/ai/evolution/viral/complete','完成拆解','POST'),
       ('/api/v1/ai/evolution/viral/delete','删除拆解','POST'),
       ('/api/v1/ai/evolution/live-review/list','直播复盘列表','POST'),
       ('/api/v1/ai/evolution/live-review/get','直播复盘详情','POST'),
       ('/api/v1/ai/evolution/live-review/trigger','触发复盘','POST'),
       ('/api/v1/ai/evolution/live-review/complete','完成复盘','POST'),
       ('/api/v1/ai/evolution/live-review/delete','删除复盘','POST'),
       ('/api/v1/ai/evolution/video/compare','视频对比','POST'),
       ('/api/v1/ai/evolution/stats','进化统计','POST')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Abtest 补充 -> /abtest
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'abtest', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/abtest' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/abtest/experiment/list','实验列表'),
       ('/api/v1/abtest/experiment/get','实验详情'),
       ('/api/v1/abtest/experiment/save','保存实验'),
       ('/api/v1/abtest/experiment/delete','删除实验'),
       ('/api/v1/abtest/experiment/update-status','实验状态'),
       ('/api/v1/abtest/experiment/set-winner','设获胜'),
       ('/api/v1/abtest/variant/save','保存变体'),
       ('/api/v1/abtest/variant/delete','删除变体'),
       ('/api/v1/abtest/event/record','记录事件')
     ) AS r(c,n)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Agent 补充 -> /ai
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'agent', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/agent/list','智能体列表'),
       ('/api/v1/agent/get','智能体详情'),
       ('/api/v1/agent/save','保存智能体'),
       ('/api/v1/agent/delete','删除智能体'),
       ('/api/v1/agent/update-status','更新状态'),
       ('/api/v1/agent/conversation/create','创建对话'),
       ('/api/v1/agent/conversation/list','对话列表'),
       ('/api/v1/agent/conversation/delete','删除对话'),
       ('/api/v1/agent/message/send','发送消息'),
       ('/api/v1/agent/message/list','消息列表')
     ) AS r(c,n)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- System 补充 -> /log
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'system', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/system/api-log/list','API日志','POST'),
       ('/api/v1/system/api-log/stats','API统计','POST'),
       ('/api/v1/system/sync-log/list','同步日志','POST'),
       ('/api/v1/system/health','健康检查','POST'),
       ('/api/v1/system/info','系统信息','POST')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Wecom 补充 -> /config
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'wecom', 'POST', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/wecom/robot/list','机器人列表'),
       ('/api/v1/wecom/robot/get','机器人详情'),
       ('/api/v1/wecom/robot/save','保存机器人'),
       ('/api/v1/wecom/robot/delete','删除机器人'),
       ('/api/v1/wecom/robot/update-status','机器人状态'),
       ('/api/v1/wecom/rule/list','规则列表'),
       ('/api/v1/wecom/rule/get','规则详情'),
       ('/api/v1/wecom/rule/save','保存规则'),
       ('/api/v1/wecom/rule/delete','删除规则'),
       ('/api/v1/wecom/rule/update-status','规则状态'),
       ('/api/v1/wecom/log/list','消息日志'),
       ('/api/v1/wecom/push','推送消息')
     ) AS r(c,n)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- Attribution 补充 -> /live（路径变量用前缀，如 session 匹配 /session/123）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'attribution', r.m, m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m,
     (VALUES
       ('/api/v1/attribution/trigger','触发归因','POST'),
       ('/api/v1/attribution/session','场次归因','GET'),
       ('/api/v1/attribution/summary','归因汇总','GET'),
       ('/api/v1/attribution','归因详情','GET')
     ) AS r(c,n,m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- 常用按钮
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'button', r.c, r.n, r.mod, NULL, m.id, 0
FROM (VALUES
  ('user:add','新增用户','auth'), ('user:edit','编辑用户','auth'), ('user:delete','删除用户','auth'), ('user:ban','封禁用户','auth'),
  ('role:add','新增角色','auth'), ('role:edit','编辑角色','auth'), ('role:delete','删除角色','auth'), ('role:auth','角色授权','auth'),
  ('resource:add','新增资源','auth'), ('resource:edit','编辑资源','auth'), ('resource:delete','删除资源','auth'),
  ('copy:add','新增文案','copy'), ('copy:edit','编辑文案','copy'), ('copy:delete','删除文案','copy'), ('copy:approve','审批','copy'),
  ('product:add','新增商品','product'), ('product:edit','编辑商品','product'), ('product:delete','删除商品','product'),
  ('script:add','新增话术','script'), ('script:edit','编辑话术','script'), ('script:delete','删除话术','script'),
  ('live:add','新增场次','live'), ('live:edit','编辑场次','live'), ('live:delete','删除场次','live')
) AS r(c,n,mod)
JOIN auth_resource m ON m.resource_code = CASE r.mod WHEN 'auth' THEN '/auth' WHEN 'copy' THEN '/copy' WHEN 'product' THEN '/product' WHEN 'script' THEN '/live' WHEN 'live' THEN '/live' END
  AND m.resource_type = 'menu' AND m.deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND resource_type = 'button' AND deleted = 0);

-- admin 绑定所有新资源
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r
CROSS JOIN auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0
  AND res.deleted = 0 AND res.resource_type IN ('api','button')
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource WHERE role_id = r.id AND resource_id = res.id);
