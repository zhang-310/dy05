-- ============================================================
-- ai_call_log 全链路性能阶段耗时字段
-- 版本：1.2 | 更新日期：2026-03-01
-- 说明：用于性能分析，记录各阶段耗时（JSON）
-- ============================================================

ALTER TABLE ai_call_log ADD COLUMN IF NOT EXISTS stage_timings TEXT;

COMMENT ON COLUMN ai_call_log.stage_timings IS '各阶段耗时（JSON），如 {"cache_lookup_ms":2,"query_rewrite_ms":50,"embedding_ms":100,"vector_search_ms":80,"fulltext_search_ms":120,"fusion_ms":1,"reranker_ms":200,"cache_write_ms":3}';
