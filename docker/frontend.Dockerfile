# ============================================================
# 前端容器：React + MUI SPA，由 Nginx 提供静态资源
# 构建产物输出到 dist/，base 为 /
# ============================================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app

# 复制依赖文件
COPY frontend-react/package.json frontend-react/package-lock.json* ./
RUN npm ci --prefer-offline --no-audit --no-fund

# 复制源码并构建（输出到 dist，根路径部署）
COPY frontend-react/ .
RUN npm run build:docker

# ============================================================
# Nginx 运行时
# ============================================================
FROM nginx:1.25-alpine
LABEL maintainer="gaifan" description="Douyin Operations Frontend"

RUN apk add --no-cache curl

# 复制构建产物到 Nginx 静态目录（frontend-react 输出到 dist）
COPY --from=frontend-builder /app/dist /usr/share/nginx/html

# 复制前端 nginx 配置（仅静态托管，API 由入口 nginx 代理）
COPY docker/frontend-nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
