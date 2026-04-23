# live 模块 SQL 迁移

## 迁移脚本执行顺序

1. `migration-persona.sql` — persona_id、script_type、style、ai_generated
2. `migration-fields.sql` — session_cover、script_style、readiness_check、script_source、product_id、ai_call_log_id 等
3. `migration-data-sync.sql` — live_session_data、live_product_data 表
4. `migration-ai-review.sql` — live_session_data.ai_review_id（关联 AI 复盘报告）
5. `migration-ai-analysis.sql` — ai_analysis 字段
6. `migration-script-template.sql` — live_script_template 表
7. `migration-monitor-fields.sql` — total_viewers、new_followers、online_count、gmv、orders
8. `migration-monitor-archive.sql` — live_monitor_archive 归档表（供 LiveMonitorArchiveScheduler 使用）

## 一键执行

**Windows（需安装 PostgreSQL 客户端）：**
```bat
cd sql\live
set PGDATABASE=douyin_operations
set PGUSER=postgres
set PGPASSWORD=postgresql
set PGPORT=5433
run-all-migrations.bat
```

**Linux/Mac：**
```bash
cd sql/live
export PGDATABASE=douyin_operations
export PGPORT=5433  # 若使用 Docker 映射端口
./run-all-migrations.sh
```

**Docker 内执行：**
```bash
docker exec -i dy-postgres psql -U postgres -d douyin_operations < sql/live/migration-persona.sql
# 或逐个执行上述 6 个 migration-*.sql
```
