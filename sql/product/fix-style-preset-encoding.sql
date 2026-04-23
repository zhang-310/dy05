-- ============================================================
-- 风格预设表中文乱码修复脚本（与前端 api/product.ts STYLE_CODE_* 一致）
-- 若 style_preset 的 preset_name、description 出现乱码，执行本脚本
-- 执行前确保客户端编码为 UTF-8：psql 中执行 SET client_encoding TO 'UTF8';
-- Windows: chcp 65001 后再执行 psql
-- 若缺少扩展风格行，先执行 migration-style-preset-extended.sql
-- ============================================================

-- 基础 5 个
UPDATE style_preset SET preset_name = '专业版', description = '专业严谨，逻辑清晰' WHERE preset_code = 'professional' AND deleted = 0;
UPDATE style_preset SET preset_name = '亲切版', description = '温暖亲切，像朋友推荐' WHERE preset_code = 'friendly' AND deleted = 0;
UPDATE style_preset SET preset_name = '激情版', description = '热情洋溢，感染力强' WHERE preset_code = 'passionate' AND deleted = 0;
UPDATE style_preset SET preset_name = '种草版', description = '真实体验，强调感受' WHERE preset_code = 'seeding' AND deleted = 0;
UPDATE style_preset SET preset_name = '促销版', description = '突出优惠，营造紧迫感' WHERE preset_code = 'promotion' AND deleted = 0;

-- 扩展：情绪/别具一格（与 migration-style-preset-extended 一致）
UPDATE style_preset SET preset_name = '高鸡汤', description = '励志治愈，心理共鸣' WHERE preset_code = 'chicken_soup' AND deleted = 0;
UPDATE style_preset SET preset_name = '高歇后语', description = '俗语智慧，幽默接地气' WHERE preset_code = 'proverb' AND deleted = 0;
UPDATE style_preset SET preset_name = '高扎心', description = '直击痛点，现实共鸣' WHERE preset_code = 'heart_piercing' AND deleted = 0;
UPDATE style_preset SET preset_name = '搞笑幽默', description = '轻松搞笑，拉近距离' WHERE preset_code = 'humorous' AND deleted = 0;
UPDATE style_preset SET preset_name = '人设特色', description = '强化人设，独树一帜' WHERE preset_code = 'persona_flavor' AND deleted = 0;
UPDATE style_preset SET preset_name = '地方特色', description = '方言俚语，地域共鸣' WHERE preset_code = 'local_flavor' AND deleted = 0;
UPDATE style_preset SET preset_name = '高创意', description = '出其不意，记忆点强' WHERE preset_code = 'creative' AND deleted = 0;
