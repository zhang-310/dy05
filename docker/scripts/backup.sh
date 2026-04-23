#!/bin/bash
# PostgreSQL 每日全量备份脚本
# 用法: ./backup.sh
# 配置: 环境变量 PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/postgres}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
DB_HOST="${PGHOST:-postgres}"
DB_PORT="${PGPORT:-5433}"
DB_NAME="${PGDATABASE:-douyin_operations}"
DB_USER="${PGUSER:-postgres}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.dump"

echo "[$(date)] 开始备份: ${DB_NAME}"

# 确保备份目录存在
mkdir -p "${BACKUP_DIR}"

# 执行备份
PGPASSWORD="${PGPASSWORD:-postgresql}" pg_dump \
  -h "${DB_HOST}" \
  -p "${DB_PORT}" \
  -U "${DB_USER}" \
  -d "${DB_NAME}" \
  --format=custom \
  --compress=9 \
  --verbose \
  --file="${BACKUP_FILE}" 2>&1

echo "[$(date)] 备份完成: ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"

# 清理过期备份
echo "[$(date)] 清理 ${RETENTION_DAYS} 天前的备份..."
find "${BACKUP_DIR}" -name "${DB_NAME}_*.dump" -mtime +${RETENTION_DAYS} -delete
echo "[$(date)] 清理完成"

# 列出当前备份
echo "[$(date)] 当前备份列表:"
ls -lh "${BACKUP_DIR}/"
