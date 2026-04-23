# dy02 文档归档索引（只读）

> **dy05 现行说明**：请先读仓库 **`docs/README.md`** → **`SSOT.md`** / **`BUILD.md`**。本目录为从 dy02 迁入的 **完整快照**，**不随 dy05 日常迭代更新**。
>
> **根目录已整理**：除本 `README` 与 `00～04` 核心五篇外，原散落根目录的升级/复盘/报告类 `.md` 已移至 **`_orphan-root-md/`**（仍可全文检索）。

---

# dy02 项目文档索引（原稿）

> 原更新日期：2026-03-27

本文档目录为 dy02（抖音运营 SaaS 平台）完整文档体系的导航入口（历史快照）。

---

## 核心文档

| 文档 | 说明 |
|------|------|
| [00-业务范围.md](00-业务范围.md) | 产品目标、业务边界、品类约束 |
| [01-产品需求总览.md](01-产品需求总览.md) | PRD：功能模块全景、用户角色、系统边界 |
| [02-技术架构文档.md](02-技术架构文档.md) | 后端/前端架构、技术选型、部署架构 |
| [03-开发规范.md](03-开发规范.md) | 编码规范、命名约定、9步开发流程 |
| [04-错误码注册表.md](04-错误码注册表.md) | 全量错误码（号段规则 + 三处同步说明）|

---

## API 文档

位于 `api/` 目录，共 **157 个 Controller、972 个接口、212 个 Entity**。

| 文件 | 模块 | Controller | API | Entity |
|------|------|-----------|-----|--------|
| [api/00-INDEX.md](api/00-INDEX.md) | 总索引 | 157 | 972 | 212 |
| [api/auth-module.md](api/auth-module.md) | 认证权限 | 7 | 48 | 10 |
| [api/live-module.md](api/live-module.md) | 直播 | 39 | 192 | 33 |
| [api/shortvideo-module.md](api/shortvideo-module.md) | 短视频 | 35 | 185 | 41 |
| [api/ai-module.md](api/ai-module.md) | AI 引擎 | 18 | 143 | 42 |
| [api/script-module.md](api/script-module.md) | 话术脚本 | 9 | 41 | 12 |
| [api/product-module.md](api/product-module.md) | 商品管理 | 6 | 64 | 13 |
| [api/copy-module.md](api/copy-module.md) | 文案管理 | 4 | 16 | 3 |
| [api/douyin-module.md](api/douyin-module.md) | 抖音账号 | 4 | 19 | 5 |
| [api/agent-module.md](api/agent-module.md) | 智能体 | — | — | — |
| [api/abtest-module.md](api/abtest-module.md) | A/B 测试 | — | — | — |
| [api/config-module.md](api/config-module.md) | 配置管理 | — | — | — |
| [api/storage-module.md](api/storage-module.md) | 存储管理 | — | — | — |
| [api/system-module.md](api/system-module.md) | 系统监控 | — | — | — |
| [api/log-module.md](api/log-module.md) | 日志管理 | — | — | — |
| [api/wecom-module.md](api/wecom-module.md) | 企业微信 | — | — | — |
| [api/payment-module.md](api/payment-module.md) | 支付 | — | — | — |
| [api/attribution-module.md](api/attribution-module.md) | 归因分析 | — | — | — |
| [api/dashboard-module.md](api/dashboard-module.md) | 驾驶舱 | — | — | — |

---

## 模块设计文档

位于 `modules/` 目录，每个模块含 9 篇设计文档（00-大纲 → 08-测试与验收）。

| 模块目录 | 表前缀 | 说明 |
|---------|--------|------|
| [modules/auth/](modules/auth/) | auth_ | 用户认证、角色权限、资源管理 |
| [modules/live/](modules/live/) | live_ | 直播场次、话术生成、数据分析 |
| [modules/script/](modules/script/) | sc_ | 话术库、违规检测、模板管理 |
| [modules/product/](modules/product/) | dy_ | 商品管理、效果评分 |
| [modules/shortvideo/](modules/shortvideo/) | sv_ | 短视频策划、AI 创作、爆款复刻 |
| [modules/ai/](modules/ai/) | ai_ | 知识库、RAG、自进化引擎 |
| [modules/copy/](modules/copy/) | cp_ | 文案库、版本管理、审核 |
| [modules/abtest/](modules/abtest/) | ab_ | A/B 实验、效果对比 |
| [modules/agent/](modules/agent/) | agent_ | 智能体对话、任务执行 |
| [modules/douyin/](modules/douyin/) | dy_ | 抖音账号绑定、OAuth、人设 |
| [modules/config/](modules/config/) | sys_config_ | 系统配置、行业分类 |
| [modules/storage/](modules/storage/) | sys_file_ | 文件上传、BOS/OSS |
| [modules/log/](modules/log/) | sys_log_ | 操作日志、审计 |
| [modules/system/](modules/system/) | sys_ | 系统监控、API 调用日志 |
| [modules/wecom/](modules/wecom/) | wecom_ | 企业微信推送、机器人 |
| [modules/payment/](modules/payment/) | pay_ | 支付订单、交易流水 |
| [modules/attribution/](modules/attribution/) | — | 归因分析 |

---

## 架构文档

| 文档 | 说明 |
|------|------|
| [architecture/00-系统总览.md](architecture/00-系统总览.md) | 系统整体架构图 |
| [architecture/01-后端架构.md](architecture/01-后端架构.md) | Spring Boot 分层架构 |
| [architecture/02-前端架构.md](architecture/02-前端架构.md) | React + Vite + MUI 架构 |
| [architecture/03-数据库设计.md](architecture/03-数据库设计.md) | 数据库表设计原则 |
| [architecture/04-基础设施.md](architecture/04-基础设施.md) | 缓存、消息队列、搜索引擎 |

---

## 开发指南

| 文档 | 说明 |
|------|------|
| [development/00-快速开始.md](development/00-快速开始.md) | 本地环境搭建 |
| [development/01-开发规范.md](development/01-开发规范.md) | 代码规范详细版 |
| [development/02-新模块开发.md](development/02-新模块开发.md) | 新模块 9 步流程 |
| [development/03-错误码注册表.md](development/03-错误码注册表.md) | 错误码管理 |
| [development/04-文档代码同步清单.md](development/04-文档代码同步清单.md) | 文档与代码同步规范 |

---

## 运维文档

| 文档 | 说明 |
|------|------|
| [deployment/00-Docker部署.md](deployment/00-Docker部署.md) | Docker Compose 部署 |
| [deployment/01-CI-CD.md](deployment/01-CI-CD.md) | CI/CD 管线说明 |
| [devops/DEPLOYMENT_GUIDE.md](devops/DEPLOYMENT_GUIDE.md) | 生产部署指南 |
| [devops/DOCKER_SETUP_GUIDE.md](devops/DOCKER_SETUP_GUIDE.md) | Docker 配置指南 |
| [devops/PROMETHEUS_GRAFANA_SETUP.md](devops/PROMETHEUS_GRAFANA_SETUP.md) | 监控配置 |

---

## 架构决策记录（ADR）

| 编号 | 标题 |
|------|------|
| [adr/001-统一POST接口.md](adr/001-统一POST接口.md) | 所有业务 API 统一用 POST |
| [adr/002-无数据库外键.md](adr/002-无数据库外键.md) | 不使用数据库外键 |
| [adr/003-Specification动态查询.md](adr/003-Specification动态查询.md) | JPA Specification 动态查询 |
| [adr/004-Zustand状态管理.md](adr/004-Zustand状态管理.md) | 前端 Zustand 状态管理 |
| [adr/005-API鉴权白名单单一来源.md](adr/005-API鉴权白名单单一来源.md) | API 鉴权白名单 SSOT |

---

## AI 模块专项

| 文档 | 说明 |
|------|------|
| [ai/00-火山方舟对接.md](ai/00-火山方舟对接.md) | 火山方舟 LLM API 接入指南 |

---

## 质量保障

| 文档 | 说明 |
|------|------|
| [quality/00-对外验收口径与三层100%.md](quality/00-对外验收口径与三层100%.md) | 验收标准与质量门槛 |
