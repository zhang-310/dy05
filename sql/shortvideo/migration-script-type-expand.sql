-- LF-02：与 Flyway V102 一致
COMMENT ON COLUMN sv_script.script_type IS
  '脚本类型：viral_clone=爆款复刻, daily=日更, soft_ad=软植入, review=测评,
   tutorial=教程, unboxing=开箱, seeding=种草, skit=段子/剧情, vlog=日常Vlog,
   before_after=前后对比, live_preview=直播预热, trending=热点借势';
