-- LF-05：爆款深度分析（ASR + 抽帧 + LLM）持久化字段

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS transcript TEXT;
COMMENT ON COLUMN sv_viral_video.transcript IS 'ASR 转写的完整口播文案';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS scene_descriptions TEXT;
COMMENT ON COLUMN sv_viral_video.scene_descriptions IS '抽帧视觉分析的场景描述（每帧一行）';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_variable_table TEXT;
COMMENT ON COLUMN sv_viral_video.remake_variable_table IS '可替换变量表 JSON：{mustKeep:[], replaceable:[{variable,original,suggestion}]}';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyzed_at TIMESTAMP;
COMMENT ON COLUMN sv_viral_video.deep_analyzed_at IS '深度分析完成时间';

COMMENT ON TABLE sv_viral_video IS '爆款视频库：含垂类采集(vertical_industry)、热搜采集(hot_topic_scheduler)、阈值采集(douyin_video_threshold)、AI深度分析(ASR+抽帧+LLM)、人设融合二创';

CREATE INDEX IF NOT EXISTS idx_sv_viral_deep_analyzed ON sv_viral_video(deep_analyzed_at) WHERE deleted = 0 AND deep_analyzed_at IS NULL;
