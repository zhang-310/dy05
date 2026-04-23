#!/bin/sh
# ============================================================
# Nginx 启动脚本
# 环境变量注入 + 健康检查
# ============================================================

set -e

# 创建健康检查路由
cat > /usr/share/nginx/conf.d/health.conf <<EOF
server {
    listen 80;
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type "text/plain";
    }
}
EOF

# 输出启动日志
echo "Starting Nginx frontend server..."
echo "Environment:"
echo "  NODE_ENV: ${NODE_ENV:-production}"
echo "  API_BASE: ${API_BASE:-/api/v1}"

# 前台运行 Nginx
exec nginx -g "daemon off;"
