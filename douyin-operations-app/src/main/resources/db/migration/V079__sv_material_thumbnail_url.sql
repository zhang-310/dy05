-- A-5：素材视频预览缩略图 CDN URL
ALTER TABLE sv_material ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);
COMMENT ON COLUMN sv_material.thumbnail_url IS '视频首帧等预览图 BOS CDN URL';
