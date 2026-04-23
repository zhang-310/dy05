-- 添加场景字段到产品话术表
-- 参考 dy01 的场景化生成能力

ALTER TABLE dy_product_script ADD COLUMN IF NOT EXISTS scene VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_product_script_scene ON dy_product_script(scene);

COMMENT ON COLUMN dy_product_script.scene IS '应用场景：short_video(短视频带货)/guopin(过品带货)/cangbo(仓播带货)/danpin(单品直播间)/yubo(娱播穿插)';

-- 场景说明：
-- short_video: 短视频带货 - 15-60秒，快节奏，强调视觉冲击
-- guopin: 过品带货 - 快速介绍，突出核心亮点，适合多品轮播
-- cangbo: 仓播带货 - 强调库存充足、优惠力度、限时抢购
-- danpin: 单品直播间 - 深度讲解，反复强调卖点，适合高客单价
-- yubo: 娱播穿插 - 轻松自然，融入娱乐内容，不生硬推销
