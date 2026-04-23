-- 可灵等 CDN 返回的 image_url 常含签名参数，超过 512 字符
ALTER TABLE ai_image_generation ALTER COLUMN image_url TYPE TEXT;
