# 数据库审计报告

**审计时间**: 2026-05-12  
**数据库**: PostgreSQL 15, douyin_operations  
**表总数**: 246 张

---

## 1. 主键覆盖率

✅ **100% 覆盖** - 所有 246 张表均有主键

---

## 2. 逻辑删除字段覆盖率

✅ **81.3% 覆盖** (200/246 张表有 `deleted` 字段)

**缺少 `deleted` 字段的表** (46 张):
- 日志类表 (合理): `audit_log`, `auth_login_log`, `sys_api_call_log`, `sys_operation_log`, `sys_system_log`, `external_api_call_log`, `ai_call_log`, `ai_query_log`, `ai_search_log`, `model_training_log`, `payment_transaction_log`, `sc_compliance_check_log`, `sv_cross_module_log`, `sv_generation_log`, `sv_persona_check_log`, `wc_message_log`
- 关联表 (合理): `auth_role_resource`, `payment_order_item`, `sd_product_mapping`
- 历史/快照表 (合理): `live_monitor_archive`, `script_version_history`, `sys_config_version_history`, `product_script_snapshot`, `sv_competitor_snapshot`
- 系统表 (合理): `flyway_schema_history`
- 其他: `ab_event`, `ai_evolve_scheduler_lock`, `auth_verify_code`, `benchmark_prompt_usage_log`, `benchmark_script_similarity`, `benchmark_script_usage_effect`, `dy_product_sales_history`, `dy_product_script_usage`, `evolution_fitness_record`, `kb_feedback`, `live_approval_log`, `live_danmaku_intent_log`, `live_monitor`, `live_session_realtime_viewer_sample`, `payment_reconciliation_diff`, `payment_refund`, `product_similarity_matrix`, `sc_script_generation`, `sc_script_variant`, `sc_search_result`, `sc_search_suggestion`, `script_usage_log`, `sv_bgm_library`, `sv_cinematic_preset`, `sv_comment`, `sv_competitor_report`, `sv_effect_prediction`, `sv_material`, `sv_scene_camera_mapping`, `sv_video_data`, `sv_viral_favorite`, `sv_viral_remake_log`, `sv_webhook_dlq`, `sys_file`, `sys_sync_log`, `sys_upload_chunk`

**建议**: 大部分缺失是合理的（日志、关联表、历史表），少数业务表可考虑补充 `deleted` 字段以支持软删除。

---

## 3. 时间戳字段覆盖率

⚠️ **72.0% 覆盖** (177/246 张表同时有 `create_time` 和 `update_time`)

**缺少 `update_time` 的表** (69 张):
- 只读/追加表 (合理): 所有日志表、历史表、快照表
- 关联表 (合理): `auth_role_resource`, `payment_order_item`, `sd_product_mapping`
- 其他: `ab_event`, `agent_message`, `ai_agent_workflow_context`, `ai_evolve_pending_deepen`, `ai_evolve_report`, `ai_graph_edge`, `ai_inference_audit`, `ai_model_benchmark`, `ai_model_routing_log`, `auth_verify_code`, `benchmark_script_similarity`, `benchmark_script_usage_effect`, `dy_product_sales_history`, `dy_product_script_usage`, `evolution_fitness_record`, `kb_feedback`, `live_approval_log`, `live_danmaku_intent_log`, `live_monitor`, `live_session_realtime_viewer_sample`, `payment_reconciliation_diff`, `payment_refund`, `product_script_snapshot`, `product_similarity_matrix`, `sc_script_generation`, `sc_script_variant`, `sc_search_result`, `sc_search_suggestion`, `script_usage_log`, `sv_bgm_library`, `sv_cinematic_preset`, `sv_comment`, `sv_competitor_report`, `sv_competitor_snapshot`, `sv_effect_prediction`, `sv_generation_log`, `sv_material`, `sv_persona_check_log`, `sv_scene_camera_mapping`, `sv_video_data`, `sv_viral_favorite`, `sv_viral_remake_log`, `sv_webhook_dlq`, `sys_config_version_history`, `sys_file`, `sys_sync_log`, `sys_upload_chunk`

**缺少 `create_time` 的表** (5 张):
- `ai_evolve_scheduler_lock` (锁表，合理)
- `auth_login_log` (日志表，应有 `create_time`)
- `flyway_schema_history` (系统表，合理)
- `live_session_realtime_viewer_sample` (实时采样表，合理)
- `sc_script_generation` (生成任务表，应有 `create_time`)
- `sc_script_variant` (变体表，应有 `create_time`)
- `script_usage_log` (日志表，应有 `create_time`)

**建议**: 
- ⚠️ `auth_login_log` 应补充 `create_time` 字段
- ⚠️ `sc_script_generation`, `sc_script_variant`, `script_usage_log` 应补充 `create_time` 字段

---

## 4. 数据量分析

**TOP 10 数据量最大的表**:

| 表名 | 行数 | 表大小 | 索引大小 | 总大小 |
|------|------|--------|----------|--------|
| sys_api_call_log | - | 778 MB | 402 MB | 1181 MB |
| audit_log | - | 268 MB | 385 MB | 653 MB |
| ai_kb_document | - | 45 MB | 187 MB | 232 MB |
| sys_operation_log | - | 150 MB | 40 MB | 189 MB |
| ai_evolve_report | - | 5.6 MB | 96 MB | 101 MB |
| ai_call_log | - | 36 MB | 31 MB | 67 MB |
| copy_library | 88,342 | 25 MB | 4.2 MB | 29 MB |
| ai_evolve_topic | - | 11 MB | 2.7 MB | 13 MB |
| ai_evolve_task | - | 8.6 MB | 1.9 MB | 11 MB |
| auth_login_log | - | 3.6 MB | 4.1 MB | 7.9 MB |

**核心业务表数据量**:
- `live_script`: 2,483 行, 768 KB (表) + 904 KB (索引) = 1.7 MB
- `live_session`: 225 行, 280 KB
- `ai_knowledge_base`: 184 KB
- `auth_user`: 8 行, 152 KB
- `douyin_account`: 7 行, 64 KB
- `oauth_token`: 0 行, 40 KB (加密存储)

---

## 5. 索引覆盖率分析

**索引覆盖良好的表**:
- `live_session`: 13 个索引 ✅
- `live_script`: 11 个索引 ✅
- `agent`: 5 个索引 ✅
- `oauth_token`: 4 个索引 ✅
- `copy_library`: 4 个索引 ✅
- `douyin_account`: 3 个索引 ✅

**缺失索引分析**:
1. ⚠️ `live_script.ai_call_log_id`: 仅 4/2,483 行 (0.16%) 有值 - **低优先级**
2. ✅ `live_session.template_id`: 0/225 行 (0%) 有值 - **无需索引**

---

## 6. 表空间与索引占比分析

**索引占比过高的表** (索引 > 50%):

| 表名 | 总大小 | 表占比 | 索引占比 |
|------|--------|--------|----------|
| ai_evolve_report | 101 MB | 5.6% | 94.4% |
| ai_kb_document | 232 MB | 19.4% | 80.6% |
| audit_log | 653 MB | 41.0% | 59.0% |
| ai_index_queue | 1.5 MB | 4.1% | 95.9% |

**说明**: 
- `ai_evolve_report`, `ai_kb_document`, `ai_index_queue` 索引占比极高 (>80%)，可能是多字段联合索引或全文索引导致
- `audit_log` 索引占比 59%，符合审计日志表的查询需求（按时间、用户、操作类型等多维度查询）

---

## 7. 表膨胀与 Vacuum 状态

✅ **无表膨胀问题**

**统计结果**:
- 总表数: 247 张
- 死元组 > 1000 的表: 0 张
- 总死元组数: 0

**说明**: 数据库 AUTOVACUUM 工作正常，所有表的死元组已被清理。

---

## 8. 外键约束检查

根据 ADR-002 (无数据库外键)，本项目使用应用层校验代替数据库外键。

✅ **符合架构决策** - 无需检查外键约束

---

## 9. 安全性检查

### 9.1 敏感数据加密

✅ **OAuth Token 加密**: `oauth_token` 表的 `access_token` 和 `refresh_token` 字段使用 AES-256-GCM 加密存储
- 加密器: `TokenEncryptionConverter.java`
- 算法: AES/GCM/NoPadding
- IV: 12 字节随机
- Tag: 128 位
- 密钥来源: 环境变量 `OAUTH_TOKEN_ENCRYPTION_KEY`

### 9.2 密码存储

✅ **BCrypt 哈希**: `auth_user` 表的 `password` 字段使用 BCrypt 哈希存储 (Spring Security 默认)

---

## 10. 性能优化建议

### 10.1 高优先级

无

### 10.2 中优先级

1. **补充时间戳字段**:
   - `auth_login_log` 补充 `create_time`
   - `sc_script_generation`, `sc_script_variant`, `script_usage_log` 补充 `create_time`

2. **监控表膨胀**:
   - 定期检查 `sys_api_call_log`, `audit_log`, `ai_kb_document` 的死元组占比
   - 配置自动 VACUUM 策略

### 10.3 低优先级

1. **考虑补充软删除字段**:
   - 部分业务表 (如 `sv_comment`, `sv_material`) 可考虑补充 `deleted` 字段

2. **索引优化 - 清理未使用的大索引**:
   - `audit_log`: 6 个未使用索引 (总计 229 MB)
     - `idx_username_created` (100 MB) - 重复索引
     - `idx_audit_log_username_created` (100 MB) - 重复索引
     - `audit_log_pkey` (41 MB) - 主键未使用（可能是查询模式问题）
     - `idx_audit_log_create_time` (40 MB) - 重复索引
     - `idx_create_time` (40 MB) - 重复索引
     - `idx_audit_log_user_id` (16 MB) - 重复索引
     - `idx_user_id` (16 MB) - 重复索引
     - `idx_action` (16 MB) - 重复索引
     - `idx_audit_log_action` (16 MB) - 重复索引
   - `sys_api_call_log`: 4 个未使用索引 (总计 258 MB)
     - `idx_api_log_status_time` (92 MB)
     - `idx_api_log_module_time` (92 MB)
     - `sys_api_call_log_pkey` (37 MB) - 主键未使用
     - `idx_api_log_create_time` (37 MB)
   - `sys_operation_log`: 2 个未使用索引 (总计 25 MB)
     - `idx_sys_operation_log_module_time` (13 MB)
     - `idx_sys_operation_log_user_time` (12 MB)
   - `ai_call_log`: 5 个未使用索引 (总计 34 MB)
   - `ai_kb_document`: 1 个未使用索引 (`idx_kb_doc_fingerprint`, 8.5 MB)

   **建议**: 
   - 分析查询模式，确认这些索引是否真的不需要
   - 如果确认不需要，删除重复索引可释放约 350 MB 空间
   - 特别关注主键未使用的情况（可能是查询未使用主键，而是使用其他索引）

3. **监控索引使用情况**:
   - 定期检查 `pg_stat_user_indexes.idx_scan = 0` 的索引
   - 评估 `ai_evolve_report`, `ai_kb_document` 的索引使用情况

---

## 11. 总体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 主键覆盖 | ✅ 100% | 所有表均有主键 |
| 逻辑删除 | ✅ 81.3% | 大部分缺失是合理的（日志、关联表） |
| 时间戳字段 | ⚠️ 72.0% | 少数表缺少 `create_time` |
| 索引覆盖 | ✅ 良好 | 核心表索引充足 |
| 数据加密 | ✅ 良好 | OAuth Token 使用 AES-256-GCM |
| 表膨胀 | ✅ 优秀 | 无死元组，AUTOVACUUM 工作正常 |
| 索引使用 | ⚠️ 待优化 | 发现 350+ MB 未使用索引 |
| 架构一致性 | ✅ 良好 | 符合 ADR-002 (无外键) |
| 数据库配置 | ✅ 良好 | AUTOVACUUM 已启用，配置合理 |
| NULL 值处理 | ⚠️ 待优化 | 部分高 NULL 列有低效索引 |

**总体结论**: 
- ✅ 数据库设计健康，无阻塞级问题
- ✅ AUTOVACUUM 工作正常，无表膨胀
- ⚠️ 发现 350+ MB 未使用索引，建议清理以节省空间
- ⚠️ 建议补充 4 张表的 `create_time` 字段
- ✅ 敏感数据加密机制完善（OAuth Token 使用 AES-256-GCM）

---

## 12. 数据库配置检查

**当前配置**:

| 参数 | 值 | 说明 |
|------|-----|------|
| shared_buffers | 256 MB | 共享缓冲区 |
| effective_cache_size | 512 MB | 有效缓存大小 |
| maintenance_work_mem | 64 MB | 维护操作内存 |
| work_mem | 4 MB | 查询操作内存 |
| max_connections | 100 | 最大连接数 |
| autovacuum | on | 自动清理（已启用）✅ |
| random_page_cost | 4 | 随机页面成本 |

**配置评估**:
- ✅ `autovacuum = on` - 自动清理已启用，表膨胀控制良好
- ✅ `shared_buffers = 256 MB` - 对于开发环境合理
- ⚠️ `effective_cache_size = 512 MB` - 可考虑提升至 1-2 GB（如果物理内存充足）
- ⚠️ `random_page_cost = 4` - 如果使用 SSD，建议降至 1.1-1.5 以优化查询计划

---

## 13. 高 NULL 比例列分析

**live_script 表高 NULL 比例列** (NULL 比例 > 50%):

| 列名 | NULL 比例 | 说明 |
|------|-----------|------|
| ab_experiment_id | 100% | A/B 实验 ID（可选字段）|
| ab_variant_id | 100% | A/B 变体 ID（可选字段）|
| conversion_delta | 100% | 转化率增量（可选字段）|
| effectiveness_score | 100% | 有效性评分（可选字段）|
| execution_time | 100% | 执行时间（可选字段）|
| interaction_delta | 100% | 互动增量（可选字段）|
| product_id | 99.96% | 商品 ID（可选字段）|
| prompt_template_id | 99.96% | 提示模板 ID（可选字段）|
| referenced_script_id | 99.96% | 引用话术 ID（可选字段）|
| referenced_script_snapshot | 99.96% | 引用话术快照（可选字段）|
| requirement | 99.96% | 需求描述（可选字段）|
| user_id | 99.96% | 用户 ID（可选字段）|
| viewer_delta | 99.96% | 观众增量（可选字段）|
| ai_call_log_id | 99.84% | AI 调用日志 ID（可选字段）|
| generation_prompt_hash | 99.84% | 生成提示哈希（可选字段）|
| actual_execution_time | 99.60% | 实际执行时间（可选字段）|
| ai_suggestion | 99.40% | AI 建议（可选字段）|
| script_type | 98.79% | 话术类型（可选字段）|
| style | 98.79% | 风格（可选字段）|

**live_session 表高 NULL 比例列** (NULL 比例 > 50%):

| 列名 | NULL 比例 | 说明 |
|------|-----------|------|
| ab_experiment_id | 100% | A/B 实验 ID（可选字段）|
| ab_variant_id | 100% | A/B 变体 ID（可选字段）|
| actual_end_time | 100% | 实际结束时间（可选字段）|
| actual_start_time | 100% | 实际开始时间（可选字段）|
| approval_status | 100% | 审批状态（可选字段）|
| live_format | 100% | 直播格式（可选字段）|
| persona_id | 100% | 人设 ID（可选字段）|
| script_style | 100% | 话术风格（可选字段）|
| session_type | 100% | 场次类型（可选字段）|
| template_id | 100% | 模板 ID（可选字段）|
| platform_id | 99.56% | 平台 ID（可选字段）|

**影响分析**:
- ⚠️ 高 NULL 比例列会影响索引效率（B-tree 索引不存储 NULL 值）
- ⚠️ 部分列（如 `live_script.ai_call_log_id`）已有索引但 NULL 比例 99.84%，索引效率极低
- ✅ 大部分高 NULL 列是可选业务字段，符合业务逻辑

**建议**:
- 对于高 NULL 比例且需要查询的列，考虑使用部分索引（`WHERE column IS NOT NULL`）
- 评估 `live_script.ai_call_log_id` 索引的必要性（仅 0.16% 行有值）
