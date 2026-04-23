# dy05 文档索引（非碎片化入口）

本仓库 **`docs/` 根下** 只维护 **本索引 + `SSOT.md` + `BUILD.md`** 三份现行说明；另有 **`adr/`**（架构决策）、**`modules/`**（新模块设计占位）。自 dy02 迁入的 **500+ 篇** 历史材料均在 **`_archive/legacy-from-dy02/`**，**只读**；其中归档根目录已 **去掉与现行 `SSOT`/`BUILD` 重复的草案**，散落的升级/复盘类文件集中在 **`_archive/legacy-from-dy02/_orphan-root-md/`**。

---

## 现行文档（优先阅读）

| 文件 | 内容 |
|------|------|
| **[SSOT.md](./SSOT.md)** | 为何重建、文档治理规则、dy02 事实摘要、业务包与 Maven 目标对应 |
| **[BUILD.md](./BUILD.md)** | 模块化单体：Maven 目录、mvn 命令、迁移阶段、环境与前端构建 |
| **[README.md](./README.md)** | 本索引 |
| **[dy05-升级交接说明.md](./dy05-升级交接说明.md)** | 切换到本仓库后下一轮 **Maven/模块化升级** 的交接清单（P0～P4） |

**开发与命令速查**：仓库根 **`CLAUDE.md`**、贡献约定 **`AGENTS.md`**。

---

## 归档里有什么（按需打开）

路径：**`docs/_archive/legacy-from-dy02/`**（自 dy02 原样迁入，**不保证与最新代码同步**）。

| 需要的内容 | 归档内路径（示例） |
|------------|-------------------|
| 业务范围 | `_archive/legacy-from-dy02/00-业务范围.md` |
| 技术架构 | `_archive/legacy-from-dy02/02-技术架构文档.md`、`architecture/00-系统总览.md` |
| API 索引与模块 API | `_archive/legacy-from-dy02/api/00-INDEX.md`、`api/*-module.md` |
| 各业务模块设计 | `_archive/legacy-from-dy02/modules/<模块>/` |
| ADR（历史副本） | `_archive/legacy-from-dy02/adr/`；**现行以 `docs/adr/` 为准** |
| 错误码注册表 | `_archive/legacy-from-dy02/04-错误码注册表.md` |
| 历史升级/战役/复盘 | `_archive/legacy-from-dy02/upgrade-plan-*`、`analysis/`、**`_orphan-root-md/`**（根目录散落稿） |

完整说明见 **`_archive/README.md`**。

---

## 今后新增文档写在哪

1. **重建 / 模块化 / 构建**：只改 **`docs/SSOT.md`** 或 **`docs/BUILD.md`**，或更新本 **`README.md`**。
2. **新业务模块**：只在 **`docs/modules/<模块>/`** 下新增（从 `00-大纲.md` 起），**不要**在 `docs/` 根目录再堆零散 md。
3. **架构决策**：优先 **`docs/adr/`**（若需与归档区分，可新建 `docs/adr/`，与 `_archive/.../adr` 并存；**现行 ADR 以根下 `docs/adr/` 为准**，必要时从归档抄入修订版）。

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-04-13 | dy05 初始化：根文档收敛为 README + SSOT + BUILD，旧 docs 迁入 `_archive/legacy-from-dy02` |
| 2026-04-13 | 归档整理：删除与现行重复的 SSOT/build 草案；根目录散落 md 迁入 `_orphan-root-md/` |
| 2026-04-13 | 归档精简：强化 `_archive/README.md` 索引与删减准则；删除 `_orphan-root-md` 内7 篇会话/重复进度类稿；SQL 与 Flyway 收敛见 `sql/README.md` |
| 2026-04-13 | Maven P0：`douyin-operations-app` 子模块；见 `docs/BUILD.md` |
| 2026-04-13 | `douyin-operations-common` + 根目录垃圾 md/log 清理；见 `docs/BUILD.md` |
| 2026-04-13 | 文档对齐：多模块 Maven 清单、**SSOT §4** 表、**BUILD** 依赖图与 App glue、交接说明与根 **README/AGENTS/CLAUDE** 一致 |
