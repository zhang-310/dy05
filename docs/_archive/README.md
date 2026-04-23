# 归档说明

## `legacy-from-dy02/`

自 **dy02** 复制仓库时，原 **`docs/` 整树** 迁入此目录（模块设计、API 说明、`upgrade-plan-*`、`analysis`、历史报告等）。

**性质**：**只读、考古**；不随 dy05 日常迭代同步更新。与现行代码或 **`docs/SSOT.md` / `docs/BUILD.md` / `docs/dy05-升级交接说明.md`** 冲突时，**以代码与根文档为准**。

**目录整理**：

- 归档 **根目录** 仅保留 **`README.md`**（原 dy02 文档总索引）与 **`00～04` 核心五篇**。
- 原根目录其余升级/复盘/报告类 `.md` 已移至 **`legacy-from-dy02/_orphan-root-md/`**（根目录散落稿集中区；**非现行规范**）。

---

## 按场景跳转（均在 `legacy-from-dy02/` 下）

| 场景 | 路径 |
|------|------|
| 业务范围 | `00-业务范围.md` |
| 技术架构 | `02-技术架构文档.md`、`architecture/00-系统总览.md` |
| API 全索引与各模块 API | `api/00-INDEX.md`、`api/*-module.md` |
| 各业务模块设计范文 | `modules/<模块>/` |
| ADR（历史副本） | `adr/`（**现行**见仓库 `docs/adr/`） |
| 错误码注册表（考古） | `04-错误码注册表.md`（与代码冲突以 **`ErrorCode.java` + 现行约定** 为准） |
| 历史升级计划 / 战役 /风险分析 | `upgrade-plan-*`、`analysis/` |
| 根目录散落稿（会话纪要、报告、旧总索引） | **`_orphan-root-md/`**（见下） |

### `_orphan-root-md/` 是什么从原 dy02 **`docs/` 根目录** 迁入的零散 `.md`（升级进度、实现报告、会话摘要、重复的快速开始等）。**查找技术事实请优先 `SSOT`/`BUILD`/`CLAUDE.md`**；此处仅作历史检索。

---

## 归档删减准则（dy05 维护时可执行）

仅当文件**至少满足一条**且快速核对后**无不可替代信息**时，可从本归档删除（建议单 commit、便于 `git` 恢复）：

1. 与现行 **`docs/SSOT.md`、`docs/BUILD.md`、`docs/dy05-升级交接说明.md`** 重复或已被其完全替代的「总方案 / SSOT 草案」。
2. **纯会话纪要**、或与 `upgrade-plan-*` / `analysis/` **主题重复**的 summary、report。
3. **明显过时**且引用已不存在路径/功能的说明（可用抽样 `grep` 验证）。

**不建议删除**：`api/00-INDEX.md`、`modules/` 下模块设计、`04-错误码注册表.md` 等仍常被代码或 `CLAUDE.md` 引用的考古入口。

---

## 今后约定

- **不要**向 `legacy-from-dy02` **追加新正文**（避免第二套「活跃」文档）。
- 新内容：**`docs/modules/`**、**`docs/adr/`**、或更新 **`docs/SSOT.md` / `docs/BUILD.md` / `docs/README.md`**。
