# 07 升级行动计划

## Phase 1: 紧急修复（破坏性问题）

| # | 行动项 | 文件 |
|---|--------|------|
| 1 | ScriptGenerationController userId 硬编码修复 | script/controller/ScriptGenerationController.java |
| 2 | OrderServiceImpl.getUserIdFromContext() 修复 | payment/service/impl/OrderServiceImpl.java |
| 3 | LiveMonitor SQL 迁移：补充 6 个缺失字段 | V004 迁移脚本 |
| 4 | LiveProduct SQL 迁移：补充 deleted + update_time | V005 迁移脚本 |
| 5 | live 三表补充 user_id 字段 | V006 迁移脚本 |

## Phase 2: 高优先级修复

| # | 行动项 |
|---|--------|
| 6 | 6 处 findAll() 全表扫描改为分页/条件查询 |
| 7 | 2 处循环 save 改为 saveAll |
| 8 | LiveProductServiceImpl.batchSort() N+1 修复 |
| 9 | 13 处 RuntimeException 替换为 BusinessException |
| 10 | 前端 console.error 替换为 toast（45+处） |
| 11 | LazyRoute 页面添加 ErrorBoundary |
| 12 | LiveScriptBuilderPage 拆分（1043行） |
| 13 | HikariCP/batch_size/fetch_size 配置优化 |
| 14 | 异步线程池扩容 |

## Phase 3: 质量提升

| # | 行动项 |
|---|--------|
| 15 | useEffect cleanup 修复（QuickGeneratePage 等） |
| 16 | Elasticsearch 启用 xpack.security |
| 17 | Minio 移除默认密钥 |
| 18 | Flyway 整合分散迁移脚本（V007） |

## Phase 4: 中期改进

| # | 行动项 |
|---|--------|
| 19 | 补充 live 模块索引 |
| 20 | 前端 API 响应类型定义（替换 Record<string, unknown>） |
