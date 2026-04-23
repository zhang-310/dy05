-- 直播聊天话题池 sys_config 配置
-- 运营可通过后台修改此配置，运行时生效，无需重启

INSERT INTO sys_config (config_key, config_value, description, value_type, deleted)
VALUES (
    'live.chat.topic_pool',
    '夫妻感情与相处的小技巧,婆媳关系怎样越处越顺,人间清醒与励志金句,歇后语与幽默段子（适度）,古诗词里的生活智慧,育儿与家长里短（轻松向）,职场心态与解压碎碎念',
    '直播聊天话题池，逗号分隔，系统按槽位序号轮换选取',
    'string',
    0
)
ON CONFLICT DO NOTHING;
