# DY01 项目第二轮深度迭代分析

> 分析时间：2026-03-06 | 基于 Phase 1-4 升级后的最新代码

## 文档索引

| 编号 | 文件 | 内容 | 发现数 |
|------|------|------|--------|
| 01 | [后端安全与架构](./01-BACKEND-SECURITY-ARCHITECTURE.md) | 安全漏洞、架构缺陷、代码质量 | 38 |
| 02 | [前端质量分析](./02-FRONTEND-QUALITY.md) | 类型安全、组件质量、状态管理 | 42 |
| 03 | [数据库与Schema](./03-DATABASE-SCHEMA.md) | Entity-Schema不对齐、索引、迁移 | 29 |
| 04 | [DevOps与运维](./04-DEVOPS-INFRA.md) | Docker、CI/CD、Nginx、监控 | 22 |
| 05 | [测试覆盖分析](./05-TEST-COVERAGE.md) | 后端/前端测试缺口、E2E | 35 |
| 06 | [性能优化](./06-PERFORMANCE.md) | N+1查询、全表扫描、缓存、并发 | 22 |
| 07 | [升级行动计划](./07-ACTION-PLAN.md) | 分阶段可执行修复计划 | 50项 |

## 本轮分析重点

相比第一轮分析（260306-1），本轮重点关注：
1. Phase 1-4 升级后遗留的深层问题
2. Entity 与 SQL Schema 不对齐（破坏性问题）
3. N+1 查询与全表扫描（性能瓶颈）
4. 92个 ServiceImpl 缺失单元测试
5. 前端 console.error 替代 toast、LazyRoute 缺 ErrorBoundary
6. live 模块数据隔离缺陷（缺 user_id 字段）

## 关键发现摘要

### CRITICAL（必须立即修复）
- ScriptGenerationController 硬编码 `userId = 1L`
- OrderServiceImpl.getUserIdFromContext() 虚假实现
- LiveMonitor Entity 有 6 个字段在 SQL 表中不存在
- LiveProduct Entity 有 3 个字段在 SQL 表中不存在

### HIGH（优先修复）
- live_script/live_monitor/live_product 缺少 user_id 数据隔离
- 13 个文件直接抛 RuntimeException
- 6 处 findAll() 全表扫描
- 前端 45+ 处 console.error 应改为 toast
- 1043 行的 LiveScriptBuilderPage 未拆分

### 统计概览
- 后端 Java 文件: 1140 个
- 前端 TS/TSX 文件: 302 个
- 后端测试覆盖率: ~23%（27/119 ServiceImpl）
- 前端测试覆盖率: ~5%（11 个测试文件）
