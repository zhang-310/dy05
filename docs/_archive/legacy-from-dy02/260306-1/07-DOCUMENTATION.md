# 07 文档与错误码同步分析

> 综合评分：50/100（C 级）
> 发现问题：12 项（P0: 2 / P1: 5 / P2: 5）

---

## P0 - 必须修复

### DOC-01: README.md 严重过时

**文件**: `README.md`

**问题**:
- 第一行声称 "Vue 3 + TypeScript"，实际使用 React 18
- 列出的 API 路由示例全部过期
- 未提及核心功能：话术版本管理、AI 脚本生成、效果评分
- 未提及 payment, monitoring, sms 等新模块

**修复**: 全面重写 README.md

---

### DOC-02: 错误码前后端不同步（缺 38 个）

**三处对比**:

| 源文件 | 错误码数量 | 同步状态 |
|-------|---------|--------|
| `ErrorCode.java` | 216 个常量 | 基准 |
| `error-codes.ts` | 178 个 | 缺 38 个 |
| `docs/04-错误码注册表.md` | 部分同步 | 落后 |

**缺失号段**:
- 4300 段（attribution 归因）
- 4400 段（payment 支付）
- 5100 段（sms 短信）
- 部分新增的 3800 段（system 监控）

**修复方案**:
1. 立即同步 error-codes.ts
2. 创建自动同步脚本（从 ErrorCode.java 生成 error-codes.ts）
3. CI 中添加同步检查门禁

---

## P1 - 应尽快修复

### DOC-03: CLAUDE.md 模块前缀表不完整

**缺失模块**:
- payment (表前缀 `pay_`, 包名 `module.payment`)
- monitoring (表前缀 `sys_monitor_`, 包名 `module.monitoring`)
- sms (表前缀 `sms_`, 包名 `module.sms`)
- attribution (表前缀 `attr_`, 包名 `module.attribution`)

### DOC-04: API 文档不覆盖全部端点

- 项目有 635 个 API 端点
- Swagger UI 仅显示基础描述
- 无独立的 API 参考文档覆盖所有端点
- @ApiResponse responseCode 标注错误（使用 "0" 而非 "200"）

### DOC-05: 测试文档与实际脱离

**文件**: `docs/testing/TEST_FILES_SUMMARY.md`
- 记录的测试文件已过时
- 无测试覆盖率目标
- 无性能基准

### DOC-06: 部署文档缺少环境变量清单

部署文档未列出所有必需的环境变量及其说明。

### DOC-07: 缺少 ADR（Architecture Decision Records）

关键技术决策无记录：
- 为什么全部用 POST？
- 为什么不用数据库外键？
- 为什么选择 Specification 而非 QueryDSL？
- 为什么选择 Zustand 而非 Redux？

---

## P2 - 优化项

### DOC-08: 无 API Changelog
API 变更无文档追踪，前端无法了解后端接口变化。

### DOC-09: 缺少故障排查手册 (Runbook)
告警触发后无标准处理流程文档。

### DOC-10: 缺少性能基准文档
无 QPS、响应时间、并发数等基准记录。

### DOC-11: 国际化无任何文档
后端 515 个文件包含硬编码中文，无 i18n 指导。

### DOC-12: 无 Onboarding 文档
新开发者无快速上手指南。
