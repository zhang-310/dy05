-- dy_persona 增加地方特色、人设记忆点（高停留话术扩展）
ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS local_flavor VARCHAR(64);
ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS persona_traits VARCHAR(256);

COMMENT ON COLUMN dy_persona.local_flavor IS '地方特色：广西(老表直播自带幽默感)/东北/四川/上海/广东等，话术生成时融入方言或地域梗';
COMMENT ON COLUMN dy_persona.persona_traits IS '人设记忆点：口头禅、标签、特色表达，逗号分隔';
