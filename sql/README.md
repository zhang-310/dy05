# 数据库脚本说明（dy05 单一入口）

**增量 schema 演进（SSOT）**：[`douyin-operations-app/src/main/resources/db/migration`](../douyin-operations-app/src/main/resources/db/migration)（**Flyway**）。应用配置见 `douyin-operations-app/src/main/resources/application.yml`（默认 `spring.flyway.enabled: ${FLYWAY_ENABLED:false}`）与 `application-dev.yml`（开发默认 `enabled: true`，便于 `ddl-auto: validate` 对齐全表）。

**本文档替代**：原 `sql/MIGRATION.md`、`sql/MIGRATION_README.md`、`sql/DATABASE_SCRIPTS.md` 已合并至此或改为重定向；请以本文件为准。

---

## 1. 新环境该用哪条路径（二选一，勿混用）

| 方式 | 适用 | 说明 |
|------|------|------|
| **A. Flyway** | 与线上一致、本地/容器已能启动 Spring Boot | 空库或已 baseline 的库：由应用迁移；开发 profile 默认开启 Flyway。 |
| **B. `init.sql` / `init-all.sql` / `init-docker.sql`** | 纯 SQL 初始化、CI 或 DBA 习惯 psql | 一次性灌入基线；**之后**增量仍应交给 **Flyway**（或重建空库再走 Flyway），避免同一变更执行两次。 |

推荐：**日常开发** 用 **Flyway**（启动 `douyin-operations` 即可）；**全新空库 + 只跑脚本** 时可用 `init.sql`，再交给 Flyway 管理后续版本。

连接信息以 Docker/环境变量为准：默认主机 `localhost`、端口 **`5433`**、库名 `douyin_operations`（与 `application.yml` 一致）。

---

## 2. 目录结构（摘要）

```
sql/
├── README.md                 # 本说明（唯一入口）
├── init.sql                  # 全量初始化（主推荐）
├── init-all.sql              # 全量（历史上有「不含 shortvideo」等差异，以文件内注释为准）
├── init-docker.sql           # Compose 网络内 postgres执行用
├── auth/ … ai/ … live/ …    # 各域 schema.sql、resource-data.sql、migration-*.sql
├── performance/              # 索引等├── migrations/               # 仅 README：原脚本已迁至 _archive
└── _archive/
    └── legacy-manual-migrations/   # 原 sql/migrations/*（考古，勿对新库批量执行）
```

各模块 `schema.sql` / `resource-data.sql` 仍用于 **`init*.sql` 拼装** 或手工对照；**新增表/列** 请走 **Flyway 新脚本**。

---

## 3. 初始化命令示例

```bash
# 创建数据库后（端口按本机为准，常见为 5433）
psql -U postgres -h localhost -p 5433 -d douyin_operations -f sql/init.sql
```

或分步 `\i sql/auth/schema.sql` 等，顺序需满足依赖（认证优先）。

---

## 4. 手工与应急脚本（非 SSOT）

- **`sql/<module>/migration-*.sql`**：遗留环境补丁、或历史文档中引用的增量；**默认新环境不要逐条执行**。若 Flyway 已包含同等变更，以 Flyway 为准。
- **[`sql/_archive/legacy-manual-migrations/`](_archive/legacy-manual-migrations/)**：原 **`sql/migrations/`** 整体归档（含旧版 `V001`～`V011` 编号、`migrate-all.sql`、rollback 等）。与当前 Flyway 版本号可能不一致，**仅供排查历史问题**。

---

## 5. 版本与迁移（考古摘要）

以下为 **dy02 时代**按「产品版本」叙述的手工迁移线索；**dy05 起增量以 Flyway 版本号为准**。

- **v1.1.0 认证**：`sql/auth/migration-v2-to-v3.sql`、`migration-role-v3.sql`
- **v1.2.0 直播**：`sql/live/migration-persona.sql`、`migration-data-sync.sql`（若存在）
- **v1.3.0 话术模板**：`sql/script/migration-template-uservw.sql`
- **v1.4.0 短视频话术**：`sql/shortvideo/migration-script-template.sql`
- **v1.5.0 产品话术**：`sql/product/` 下相关脚本
- **v1.6.0 粉丝画像**：`sql/douyin/fan-profile-schema.sql` 等
- **v1.7.0 OAuth**：`sql/douyinapi/oauth-token-schema.sql` 等

**验证表/字段是否存在**（示例）：

```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'live_session' AND column_name = 'persona_id';

SELECT EXISTS (
  SELECT FROM information_schema.tables WHERE table_name = 'live_session_data'
);
```

**回滚**：优先 **从备份恢复**；单点 `DROP TABLE` / `ALTER` 回滚仅作应急，需自行评估数据。

---

## 6. 测试数据

各模块可能含 `sample-data.sql`：**仅开发环境**；生产勿加载。

---

## 7. 常见问题

**Q：Flyway 报 checksum 错误？**  
开发环境可临时 `validate-on-migrate: false`（见 `application-dev.yml`），或与生产对齐后对目标库执行 `flyway repair` / 重建空库后全量 migrate。

**Q：`sql/migrations/migrate-all.sql` 去哪了？**  
已迁至 **`sql/_archive/legacy-manual-migrations/migrate-all.sql`**，不再作为推荐入口。

**Q：文档里还写 5532 端口？**  
以 **`application.yml` 与 `docker-compose`** 为准（当前默认 **5433**）。

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-04-13 | 合并多份 SQL 说明；Flyway 为增量 SSOT；`sql/migrations` 迁至 `_archive` |
