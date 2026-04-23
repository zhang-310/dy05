# 历史手工迁移脚本（已归档）

本目录由原 **`sql/migrations/`** 迁入，**不再作为新环境或日常增量的执行入口**。

## 为何归档

- **增量 schema 演进** 的单一事实来源为 **Flyway**：[`douyin-operations-app/src/main/resources/db/migration`](../../../douyin-operations-app/src/main/resources/db/migration)（应用启动时按 `spring.flyway.*` 执行，见该模块下 `application.yml` / `application-dev.yml`）。
- 其中 **`V001`～`V006`** 与 Flyway 同名文件 **内容一致**；**`V007`～`V011`** 与当前 Flyway **版本号已错序/合并**（例如旧 `V011__live_alert_*` 对应 Flyway `V017__live_alert_*`），继续放在 `sql/migrations/` 易与 Flyway 混淆。
- `migrate-all.sql`、`rollback-v*.sql`、`add-evolve-*.sql`、`upgrade-analysis-2026.sql` 等仅作 **考古或极端手工场景** 参考；**新库请用 Flyway 或 `sql/init.sql` 二选一，勿混跑**。

## 使用注意

- **勿**对新环境批量 `\i` 本目录脚本。
- 若文档或注释仍写 `sql/migrations/...`，以 **`sql/README.md`** 现行说明为准。
