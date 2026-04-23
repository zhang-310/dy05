# DY01 项目执行启动包

欢迎使用 DY01 项目的完整启动包！本文档是整个启动包的入口指南。

---

## 🎯 快速开始（3 步）

### 1. 阅读项目概览
**文档**：`CLAUDE.md` (项目规范和架构)
**时间**：15 分钟

### 2. 阅读本周计划
**文档**：`EXECUTION_STARTUP_CHECKLIST.md` (启动清单) 或 `WEEK1_KICKOFF_AGENDA.md` (周一议程)
**时间**：30 分钟

### 3. 开始编码
**文档**：`W01_CODE_FRAMEWORK_GUIDE.md` (代码框架使用指南)
**代码框架**：按照 TODO 注释补全
**时间**：4 天完成 W-01 全部工作

---

## 📚 文档导航

### 执行文档（4 个）

| 文档 | 用途 | 受众 | 阅读时间 |
|------|------|------|---------|
| [EXECUTION_STARTUP_CHECKLIST.md](EXECUTION_STARTUP_CHECKLIST.md) | 启动检查表、环境配置、工作项分配 | PM / Tech Lead / 全体开发 | 1h |
| [WEEK1_KICKOFF_AGENDA.md](WEEK1_KICKOFF_AGENDA.md) | 周一 2h 会议详细议程（含 API/测试设计） | PM / Tech Lead / 全体开发 | 1h |
| [DAILY_EXECUTION_TEMPLATE.md](DAILY_EXECUTION_TEMPLATE.md) | Daily Standup / 周度回顾 / 日报模板 | 全体开发 | 30min |
| [W01_CODE_FRAMEWORK_GUIDE.md](W01_CODE_FRAMEWORK_GUIDE.md) | 代码框架位置、使用说明、TODO 清单 | 后端 / 前端开发 | 1h |

### 启动总结（2 个）

| 文档 | 用途 | 受众 |
|------|------|------|
| [STARTUP_COMPLETE_SUMMARY.md](STARTUP_COMPLETE_SUMMARY.md) | 启动包完整概览、快速启动步骤、交付清单 | 所有人（特别是管理层） |
| [STARTUP_DELIVERABLES.txt](STARTUP_DELIVERABLES.txt) | 详细的交付物清单（文件列表 + 工作量）| 项目管理 / 技术负责人 |

### 项目规范

| 文档 | 用途 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | 项目架构、技术栈、规范、错误码体系 |

---

## 💻 代码框架

### 数据库脚本
- `sql/live/migration-script-version-schema.sql` - live_script_version 表建表脚本

### 后端代码框架（Java）
**位置**：`src/main/java/cn/gaifan/douyinOperations/module/live/`

| 文件 | 状态 | TODO 数 |
|------|------|--------|
| entity/LiveScriptVersion.java | ✅ 完成 | 0 |
| repository/LiveScriptVersionRepository.java | ✅ 完成 | 0 |
| service/LiveScriptVersionService.java | ✅ 完成 | 0 |
| service/impl/LiveScriptVersionServiceImpl.java | 🟡 部分 | 5 (S001-S005, ~13h) |
| controller/LiveScriptVersionController.java | 🟡 部分 | 4 (C001-C004, ~8h) |
| vo/LiveScriptVersion*.java | 🟡 部分 | 5 VO 类补充字段 |
| test/LiveScriptVersionServiceTest.java | 🟡 部分 | 5 (T001-T005, ~5h) |

**总工作量**：~31 人小时

### 前端代码框架（TypeScript/React）
**位置**：`frontend-react/src/`

| 文件 | 状态 | TODO 数 |
|------|------|--------|
| api/live.versions.ts | ✅ 完成 | 0 |
| hooks/useScriptVersions.ts | 🟡 部分 | 5 (H001-H005, ~6h) |
| components/live/VersionSelector.tsx | 🟡 部分 | 13 (Comp001-Comp013, ~10h) |
| types/live.ts | 🟡 部分 | 5 个类型定义 |
| __tests__/useScriptVersions.test.ts | 🔴 需建 | 5 (T001-T005, ~4h) |

**总工作量**：~25 人小时

---

## 🚀 执行日程

### 周四（3 月 6 日）
- **09:00-10:30**：Kickoff 会议（见 WEEK1_KICKOFF_AGENDA.md）
- **14:00-17:00**：环境配置 + 代码框架讲解
- **16:00**：Daily Standup（首次）

### 周五（3 月 7 日）
- **09:00-17:00**：持续开发（W1-001 到 W1-007）
- **16:00**：Daily Standup + 周度回顾
- **18:00**：交付截止

**详细时间表**：见 EXECUTION_STARTUP_CHECKLIST.md 或 STARTUP_COMPLETE_SUMMARY.md

---

## ✅ 成功标准

**W-01 成功的 5 个标志**：

1. ✅ **代码编译**：`mvn compile` 和 `npm run type-check` 都通过
2. ✅ **测试覆盖**：后端和前端的单元测试覆盖率 >= 80%
3. ✅ **代码审查**：所有 PR 都有 2 个 +1（无阻塞）
4. ✅ **API 可用**：4 个 API 端点可在 Postman/curl 中调用成功
5. ✅ **交付通过**：PM 和 Tech Lead 签署周五交付确认

---

## 📊 工作量统计

| 环节 | 后端 | 前端 | 合计 |
|------|------|------|------|
| 需实现的 TODO | 5 + 5 = 10 个 | 5 + 13 + 5 = 23 个 | 33 个 |
| 预估工作量 | 31 小时 | 25 小时 | **56 小时** |
| 人员配置 | 1 人 | 1 人 | 2 人 |
| 预期工期 | 4 天 | 3 天 | **4 天** |

---

## 🛠 环境要求

### 硬件
- RAM：至少 8GB（推荐 16GB）
- 磁盘：至少 200GB

### 软件
- JDK 21 (LTS)
- Maven 3.8.1+
- Node.js 18.17+
- npm 9.6+
- Git 2.40+
- Docker 20+（用于启动 PostgreSQL/Redis 等）

**验证步骤**：见 EXECUTION_STARTUP_CHECKLIST.md 的"第四部分：开发环境检查清单"

---

## 🔗 关键链接

### 项目规范
- [CLAUDE.md](CLAUDE.md) - 项目架构和规范（必读）
- [docs/04-错误码注册表.md](docs/04-错误码注册表.md) - 错误码体系

### 代码示例（参考现有实现）
- [src/main/java/.../live/entity/LiveScript.java](src/main/java/cn/gaifan/douyinOperations/module/live/entity/LiveScript.java) - Entity 示例
- [src/main/java/.../live/service/impl/LiveScriptServiceImpl.java](src/main/java/cn/gaifan/douyinOperations/module/live/service/impl/LiveScriptServiceImpl.java) - Service 实现示例
- [frontend-react/src/hooks/usePageLoading.ts](frontend-react/src/hooks/usePageLoading.ts) - Hook 示例
- [frontend-react/src/components/FormDialog.tsx](frontend-react/src/components/FormDialog.tsx) - Component 示例

---

## 🆘 常见问题

### Q: 我不知道从哪里开始？
**A**：按以下顺序：
1. 读 CLAUDE.md（项目规范）
2. 读 WEEK1_KICKOFF_AGENDA.md（项目和 M1 详情）
3. 读 W01_CODE_FRAMEWORK_GUIDE.md（代码框架指南）
4. 打开对应的代码文件，按 TODO 注释补全代码

### Q: 我该贡献什么代码？
**A**：按照 W01_CODE_FRAMEWORK_GUIDE.md 中的清单：
- 后端：补全 5 个 TODO（Service 实现 + Controller + VO + 测试）
- 前端：补全 23 个 TODO（Hook + Component + 类型 + 测试）

### Q: 我如何知道工作是否完成？
**A**：检查以下清单：
- [ ] 所有 TODO 注释已完成（通过搜索 "TODO-" 验证）
- [ ] `mvn compile` / `npm run type-check` 通过
- [ ] 单元测试通过，覆盖率 >= 80%
- [ ] PR 代码审查通过（2 个 +1）
- [ ] API 在 Postman 中可调用成功

### Q: 遇到错误怎么办？
**A**：
1. 查看 DAILY_EXECUTION_TEMPLATE.md 中的 Blocked 处理流程
2. 在 #dy01-tech Slack 频道提问
3. 或联系 Tech Lead 或对应模块的负责人

---

## 📞 支持与反馈

- **Slack**：#dy01-tech （全体开发者）/ #dy01-frontend （前端） / #dy01-backend （后端）
- **每日**：16:00 Daily Standup
- **每周**：周五 14:00 周度回顾

---

## 📝 后续阅读

- **下一周**：W-02 Kickoff（违规词库扩展），预计周二启动
- **长期**：Milestone 2（M2）讲解见 WEEK1_KICKOFF_AGENDA.md 的"段落 3"

---

**祝你编码愉快！** 🚀

如有任何疑问或建议，欢迎在 Slack 中提问。

---

**文档版本**：1.0
**最后更新**：2026-03-05 18:00
**维护者**：Tech Lead
