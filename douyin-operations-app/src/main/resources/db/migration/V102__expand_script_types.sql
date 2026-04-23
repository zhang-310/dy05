-- LF-02：扩展 live_script / sv_script.script_type 列注释（VARCHAR 无需 ALTER）

COMMENT ON COLUMN live_script.script_type IS
  '话术类型：opening=开场, product=产品介绍, transition=转场, closing=收尾,
   chat=聊家常, interaction=互动引导, welfare=福利话术, closing_deal=逼单促单,
   hold_back=憋单蓄水, emotional=情绪价值, rapid_intro=快速过品(仓播),
   deep_sell=深度单品, pain_point=痛点放大, testimony=用户证言, custom=自定义';

COMMENT ON COLUMN sv_script.script_type IS
  '脚本类型：viral_clone=爆款复刻, daily=日更, soft_ad=软植入, review=测评,
   tutorial=教程, unboxing=开箱, seeding=种草, skit=段子/剧情, vlog=日常Vlog,
   before_after=前后对比, live_preview=直播预热, trending=热点借势';
