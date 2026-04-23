#!/bin/bash
# PostgreSQL 备份恢复脚本
# 用法: ./restore.sh <backup_file>

set -euo pipefail

if [ $# -eq 0 ]; then
    echo "用法: $0 <backup_file>"
    echo "可用备份:"
    ls -lh /var/backups/postgres/*.dump 2>/dev/null || echo "  无可用备份"
    exit 1
fi

BACKUP_FILE="$1"
DB_HOST="${PGHOST:-postgres}"
DB_PORT="${PGPORT:-5433}"
DB_NAME="${PGDATABASE:-douyin_operations}"
DB_USER="${PGUSER:-postgres}"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "错误: 备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

echo "[$(date)] 开始恢复: ${BACKUP_FILE} -> ${DB_NAME}"
echo "警告: 这将覆盖当前数据库！5秒后开始..."
sleep 5

PGPASSWORD="${PGPASSWORD:-postgresql}" pg_restore \
  -h "${DB_HOST}" \
  -p "${DB_PORT}" \
  -U "${DB_USER}" \
  -d "${DB_NAME}" \
  --clean \
  --if-exists \
  --verbose \
  "${BACKUP_FILE}" 2>&1

echo "[$(date)] 恢复完成"
