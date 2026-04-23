# 03 数据库与 Schema 分析

## 1. CRITICAL: Entity-Schema 不对齐

### LiveMonitor（严重不对齐）

SQL 表字段：`session_id, timestamp, viewers, likes, comments, shares, product_impressions, create_time`

Entity 多出（SQL中不存在）：
- `total_viewers`, `new_followers`, `online_count`, `gmv`, `orders`, `deleted`

Entity 有 `@SQLRestriction("deleted = 0")`，但表无 deleted 列 → **运行时异常**

### LiveProduct（不对齐）

SQL 表字段：`session_id, product_id, product_name, sale_quantity, revenue, position, create_time`

Entity 多出：`scriptSource`, `productScriptId`, `productType`, `updateTime`, `deleted`

SQL 缺少：`deleted`, `update_time`

## 2. HIGH: 数据隔离缺陷

以下表缺少 user_id/owner_id，必须通过 session_id 级联验证：

| 表 | 当前隔离方案 | 风险 |
|---|------------|------|
| live_script | session_id → live_session.user_id | 易遗漏 |
| live_monitor | session_id → live_session.user_id | 易遗漏 |
| live_product | session_id → live_session.user_id | 易遗漏 |
| script_check | script_id → script_library.user_id | 易遗漏 |

## 3. 索引覆盖不足

| 表 | 缺失索引 |
|---|---------|
| live_script | (user_id, session_id, deleted), (session_id, sequence_no) |
| live_monitor | (user_id) |
| script_check | (script_id, check_time DESC) |
| copy_approval | (copy_id, approval_status) |
| agent_conversation | (user_id, create_time DESC) |

## 4. 字段类型问题

| 表.字段 | 当前 | 建议 |
|---------|------|------|
| live_product.sale_quantity | INTEGER | BIGINT |
| live_session.viewers | INTEGER | BIGINT |
| sv_comment.like_count | INTEGER | BIGINT |
| sv_comment.reply_count | INTEGER | BIGINT |

## 5. Flyway 迁移未纳入管理

分散的迁移脚本未版本化：
- `sql/live/migration-script-version.sql`
- `sql/live/migration-effectiveness-schema.sql`
- `sql/live/migration-realtime-panel.sql`
- `sql/product/migration-product-script-version.sql`
- `sql/script/migration-compliance-word.sql`
- `sql/migrations/upgrade-analysis-2026.sql`（关键：live_monitor.deleted）
