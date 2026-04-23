-- LF-02：与 Flyway V102 一致，手工执行时可单独跑本文件
COMMENT ON COLUMN live_script.script_type IS
  '话术类型：opening=开场, product=产品介绍, transition=转场, closing=收尾,
   chat=聊家常, interaction=互动引导, welfare=福利话术, closing_deal=逼单促单,
   hold_back=憋单蓄水, emotional=情绪价值, rapid_intro=快速过品(仓播),
   deep_sell=深度单品, pain_point=痛点放大, testimony=用户证言, custom=自定义';
