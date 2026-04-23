-- Migration: 风格预设迁移到数据库
-- Phase 2.3: 消除前后端硬编码同步问题

CREATE TABLE IF NOT EXISTS live_style_preset (
    id              BIGSERIAL PRIMARY KEY,
    style_key       VARCHAR(50)   NOT NULL UNIQUE,
    label           VARCHAR(50)   NOT NULL,
    group_name      VARCHAR(30)   NOT NULL,
    prompt_template TEXT          NOT NULL,
    sort_order      INTEGER       NOT NULL DEFAULT 0,
    active          INTEGER       NOT NULL DEFAULT 1,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE live_style_preset IS '直播话术风格预设';
COMMENT ON COLUMN live_style_preset.style_key IS '风格标识（唯一）';
COMMENT ON COLUMN live_style_preset.label IS '前端显示名称';
COMMENT ON COLUMN live_style_preset.group_name IS '分组名称';
COMMENT ON COLUMN live_style_preset.prompt_template IS 'Prompt 模板文本';
COMMENT ON COLUMN live_style_preset.active IS '是否启用 1=启用 0=禁用';

CREATE INDEX IF NOT EXISTS idx_live_style_preset_active ON live_style_preset(active, sort_order);

-- 种子数据：基础组
INSERT INTO live_style_preset (style_key, label, group_name, prompt_template, sort_order) VALUES
('professional', '专业', '基础', '话术风格：专业、有条理、可信度高', 100),
('friendly', '亲切', '基础', '话术风格：亲切、像朋友聊天、拉近距离', 101),
('passionate', '热情', '基础', '话术风格：热情、有感染力、情绪饱满', 102),
('gentle', '温和', '基础', '话术风格：温和、柔和、不施压，像闺蜜推荐，自然舒服', 103),
('warm', '温暖', '基础', '话术风格：温暖亲切、有关怀感，像家人般体贴，暖心', 104),
('casual', '轻松', '基础', '话术风格：轻松随意、聊天式表达，不刻意推销，降低防备', 105)
ON CONFLICT (style_key) DO NOTHING;

-- 种子数据：带货组
INSERT INTO live_style_preset (style_key, label, group_name, prompt_template, sort_order) VALUES
('seeding', '种草', '带货', '话术风格：种草向、突出使用场景和体验感', 200),
('promotion', '促销', '带货', '话术风格：促销向、限时抢购、库存紧张、制造紧迫', 201),
('comparison', '对比型', '带货', '话术风格：对比型。语言特点：通过前后对比、竞品对比突出优势，数据说话，真实可信', 202),
('scenario', '场景代入', '带货', '话术风格：场景代入。语言特点：描绘具体使用场景，让观众想象自己使用的画面，增强购买欲', 203)
ON CONFLICT (style_key) DO NOTHING;

-- 种子数据：情感组
INSERT INTO live_style_preset (style_key, label, group_name, prompt_template, sort_order) VALUES
('emotional', '共情', '情感', '话术风格：高情感、情感饱满、故事感强。语言特点：共情表达、情绪共鸣、像在讲自己的经历，让人产生「这说的就是我」的认同感，拉停留', 300),
('heart_piercing', '扎心', '情感', '话术风格：高扎心。语言特点：人生真相、成长代价、现实共鸣类金句，句式简短有力有冲击感，扎到点子上，可与「对自己好一点」等情感价值自然结合', 301),
('chicken_soup', '鸡汤', '情感', '话术风格：高鸡汤、正能量、情感饱满。语言特点：多用感叹句和反问句增强感染力，融入人生感悟和励志金句，强调「你值得」「对自己好一点」「投资自己」，产品介绍与情感价值绑定，适当使用排比句和类比', 302),
('lyrical', '抒情', '情感', '话术风格：高抒情、诗意优美。语言特点：排比、意境、比喻、金句，有审美感，格调高但不装，自然融入产品', 303),
('positive', '正能量', '情感', '话术风格：正能量。语言特点：励志、向上、希望、治愈，让人感觉被鼓励、被温暖，情绪正向', 304),
('love', '爱情', '情感', '话术风格：爱情类。语言特点：夫妻关系、恋爱观、婚姻感悟，情感共鸣，高互动', 305),
('family', '家庭', '情感', '话术风格：家庭类。语言特点：亲子、夫妻、婆媳、家庭责任等场景共鸣，贴近生活，女性受众易共鸣', 306),
('healing', '治愈系', '情感', '话术风格：治愈系。语言特点：轻柔治愈、减压放松，像深夜电台般舒适，适合高压人群，传递「慢下来也没关系」的温暖', 307),
('pain_resonance', '痛点共鸣', '情感', '话术风格：痛点共鸣。语言特点：直击生活痛点、制造缺失感，让观众产生「这说的就是我」的代入感，再自然引出解决方案', 308)
ON CONFLICT (style_key) DO NOTHING;

-- 种子数据：表达组
INSERT INTO live_style_preset (style_key, label, group_name, prompt_template, sort_order) VALUES
('humorous', '幽默', '表达', '话术风格：搞笑幽默。语言特点：段子、梗、包袱、轻松调侃，让人愉悦，易留存、复播，但不低俗', 400),
('storytelling', '故事型', '表达', '话术风格：故事型。语言特点：用真实或虚构故事引入，有起承转合，代入感强，拉停留', 401),
('suspense', '悬念型', '表达', '话术风格：悬念型。语言特点：先抛悬念/问题，制造好奇心，再揭晓答案，引导看到最后', 402),
('proverb', '歇后语', '表达', '话术风格：高歇后语、俗语、接地气、有梗。语言特点：适当融入歇后语、俗语、民间智慧，幽默不油腻，让人会心一笑，拉近距离，有记忆点', 403),
('creative', '高创意', '表达', '话术风格：高创意。语言特点：反套路、金句、类比、反转，新奇感强，传播力高，让人眼前一亮', 404),
('interactive', '互动型', '表达', '话术风格：互动型。语言特点：多用提问、投票、口令、抽奖等互动引导，活跃气氛，提升参与度', 405),
('self_mockery', '自嘲幽默', '表达', '话术风格：自嘲幽默。语言特点：先承认缺点再转折亮点，拉低姿态增强亲和力，让人觉得真实接地气，「我也是普通人」的代入感', 406),
('rhyme_jingle', '顺口溜', '表达', '话术风格：顺口溜/押韵。语言特点：节奏感强、朗朗上口、易记易传播，适合口播金句和记忆点打造，高情商回复也适用', 407)
ON CONFLICT (style_key) DO NOTHING;

-- 种子数据：节奏组
INSERT INTO live_style_preset (style_key, label, group_name, prompt_template, sort_order) VALUES
('fast', '快节奏', '节奏', '话术风格：快节奏。语言特点：信息密度高、语速快、节奏紧凑，适合限时秒杀和促单冲刺', 500),
('slow', '慢节奏', '节奏', '话术风格：慢节奏。语言特点：语速舒缓、娓娓道来，适合高客单和需要信任感的产品', 501)
ON CONFLICT (style_key) DO NOTHING;

-- 种子数据：特色组
INSERT INTO live_style_preset (style_key, label, group_name, prompt_template, sort_order) VALUES
('local_flavor', '地方特色', '特色', '话术风格：地方特色。语言特点：可融入方言、地域梗、地方习俗，亲切感强，圈层认同（若人设或场次有地域信息请体现）', 600),
('persona_flavor', '人设特色', '特色', '话术风格：人设特色。语言特点：体现人设标签、语气、记忆点，人设鲜明，粉丝粘性强', 601)
ON CONFLICT (style_key) DO NOTHING;

-- 种子数据：娱乐组
INSERT INTO live_style_preset (style_key, label, group_name, prompt_template, sort_order) VALUES
('caihongpi', '彩虹屁', '娱乐', '话术风格：彩虹屁。语言特点：花式夸赞、甜言蜜语、极致赞美，真诚不做作，让人心花怒放、如沐春风，适合暖场和拉近距离', 700),
('poison_soup', '毒鸡汤', '娱乐', '话术风格：毒鸡汤。语言特点：反讽、夸张、反转金句，表面正能量实则扎心，幽默中带现实感，如「努力不一定成功，但不努力一定很舒服」，接地气如绝绝子', 701),
('worker_life', '打工人', '娱乐', '话术风格：打工人/职场。语言特点：职场共鸣、摸鱼日常、加班心酸、老板语录，引发打工人强烈认同，评论区炸裂型话题', 702),
('growth_comeback', '成长逆袭', '娱乐', '话术风格：成长逆袭。语言特点：时间跨度对比、强反差、逆袭故事，从低谷到蜕变，激励感强，容易引发评论区分享', 703)
ON CONFLICT (style_key) DO NOTHING;
