-- 分镜尾帧：每个镜头支持首帧+尾帧（6镜=12张图），首帧运镜到尾帧成片
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS end_frame_url VARCHAR(500);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS end_frame_bos_key VARCHAR(500);
COMMENT ON COLUMN sv_shot.end_frame_url   IS '尾帧图片 URL（BOS CDN），与 keyframe_url 首帧组成运镜';
COMMENT ON COLUMN sv_shot.end_frame_bos_key IS '尾帧 BOS Key（用于删除）';
