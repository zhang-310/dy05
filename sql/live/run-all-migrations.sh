#!/bin/bash
# live 模块 - 一键执行所有迁移（Linux/Mac）
# 用法：./sql/live/run-all-migrations.sh 或 bash sql/live/run-all-migrations.sh
# 需配置：PGHOST PGPORT PGUSER PGDATABASE（默认 douyin_operations）

cd "$(dirname "$0")"
PGDATABASE="${PGDATABASE:-douyin_operations}"
PGUSER="${PGUSER:-postgres}"

echo "=== live 模块迁移开始 ==="
for f in migration-persona.sql migration-fields.sql migration-data-sync.sql migration-ai-analysis.sql migration-script-template.sql migration-monitor-fields.sql migration-ai-review.sql migration-monitor-archive.sql; do
  echo "--- 执行 $f ---"
  psql -U "$PGUSER" -d "$PGDATABASE" -f "$f" || exit 1
done
echo "=== live 模块迁移完成 ==="
