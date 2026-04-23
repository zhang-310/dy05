# 抖音运营平台 - 快速上线指南

## 一键部署（推荐）

### Windows 系统

```cmd
cd C:\claude\dy01\docker
deploy.bat
```

### Linux/Mac 系统

```bash
cd /path/to/dy01/docker
./deploy.sh
```

## 手动部署步骤

### 1. 准备环境配置

```bash
cd docker
cp .env.example .env
```

编辑 `.env` 文件，修改以下关键配置：

```bash
# 数据库密码（必须修改）
POSTGRES_PASSWORD=your_secure_password

# JWT 密钥（必须修改，至少 32 位）
APP_TOKEN_SECRET=your_jwt_secret_key_min_32_characters

# RabbitMQ 密码（建议修改）
RABBITMQ_PASS=your_rabbitmq_password
```

### 2. 启动服务

```bash
# 构建镜像
docker compose build

# 启动所有服务
docker compose up -d

# 查看启动状态
docker compose ps
```

### 3. 初始化数据库

```bash
# Windows
docker run --rm -v "%cd%\..\sql:/sql" --network docker_dy-net -e PGPASSWORD=your_secure_password postgres:15-alpine psql -h dy-postgres -U postgres -d douyin_operations -f /sql/init.sql

# Linux/Mac
docker run --rm -v "$(pwd)/../sql:/sql" --network docker_dy-net -e PGPASSWORD=your_secure_password postgres:15-alpine psql -h dy-postgres -U postgres -d douyin_operations -f /sql/init.sql
```

### 4. 验证部署

```bash
# 运行测试脚本
# Windows
test.bat

# Linux/Mac
./test.sh
```

## 移动端 / 局域网访问（本地开发）

手机与电脑在同一 WiFi 下时，可用手机浏览器访问：

```bash
# 1. 启动后端
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. 启动前端（已配置 host: 0.0.0.0，支持局域网）
cd frontend && npm run dev
```

在手机浏览器输入：`http://<电脑局域网IP>:3000`，例如 `http://192.168.1.100:3000`。

查看本机 IP：
- Windows: `ipconfig`，找 IPv4 地址
- Mac/Linux: `ifconfig` 或 `ip addr`

---

## 访问系统

部署成功后，通过以下地址访问：

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| 前端应用 | http://localhost:8888 | admin / admin123 |
| 后端 API | http://localhost:8189 | - |
| API 文档 | http://localhost:8189/swagger-ui.html | - |
| RabbitMQ 管理 | http://localhost:15672 | guest / guest |

## 常用命令

### 查看日志

```bash
# 查看所有日志
docker compose logs

# 查看应用日志
docker compose logs -f app

# 查看最近 100 行
docker compose logs --tail=100 app
```

### 重启服务

```bash
# 重启所有服务
docker compose restart

# 重启应用
docker compose restart app
```

### 停止服务

```bash
# 停止所有服务
docker compose down

# 停止并删除数据（谨慎使用）
docker compose down -v
```

### 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建
docker compose build --no-cache

# 重启服务
docker compose up -d
```

## 故障排查

### 应用无法启动

```bash
# 查看应用日志
docker compose logs app

# 检查端口占用
netstat -ano | findstr "8189"

# 重启应用
docker compose restart app
```

### 数据库连接失败

```bash
# 检查数据库状态
docker compose exec postgres pg_isready -U postgres

# 查看数据库日志
docker compose logs postgres

# 重启数据库
docker compose restart postgres
```

### 前端无法访问

```bash
# 检查 Nginx 状态
docker compose ps nginx

# 查看 Nginx 日志
docker compose logs nginx

# 重启 Nginx
docker compose restart nginx
```

## 性能优化建议

### 1. 调整 JVM 参数

编辑 `docker/Dockerfile`，修改 `JAVA_OPTS`：

```dockerfile
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
```

### 2. 配置数据库连接池

编辑 `src/main/resources/application-prod.yml`：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### 3. 启用 Redis 缓存

确保 Redis 配置正确：

```yaml
spring:
  redis:
    host: redis
    port: 6379
```

## 安全建议

1. **修改默认密码**
   - 数据库密码
   - RabbitMQ 密码
   - JWT 密钥

2. **配置防火墙**
   - 仅开放必要端口（8888、8189）
   - 限制数据库端口外部访问

3. **启用 HTTPS**
   - 配置 SSL 证书
   - 修改 Nginx 配置

4. **定期备份**
   - 每日备份数据库
   - 保留最近 7 天备份

## 监控和维护

### 查看资源使用

```bash
# 查看容器资源
docker stats

# 查看磁盘使用
docker system df
```

### 清理资源

```bash
# 清理未使用镜像
docker image prune -a

# 清理未使用容器
docker container prune

# 清理未使用卷
docker volume prune
```

### 备份数据库

```bash
# 备份
docker compose exec postgres pg_dump -U postgres douyin_operations > backup_$(date +%Y%m%d).sql

# 恢复
docker compose exec -T postgres psql -U postgres douyin_operations < backup_20260227.sql
```

## 技术支持

遇到问题请查看：

1. 部署文档：`docs/DEPLOYMENT_GUIDE.md`
2. 数据库文档：`sql/MIGRATION.md`
3. API 文档：`http://localhost:8189/swagger-ui.html`
4. 项目日志：`docker compose logs`

## 检查清单

部署前检查：

- [ ] Docker 和 Docker Compose 已安装
- [ ] 端口 8888、8189、5433、6380 未被占用
- [ ] 至少 4GB 可用内存
- [ ] 至少 20GB 可用磁盘空间
- [ ] 已修改 .env 文件中的密码配置

部署后验证：

- [ ] 所有容器正常运行（docker compose ps）
- [ ] 后端健康检查通过（http://localhost:8189/actuator/health）
- [ ] 前端页面可访问（http://localhost:8888）
- [ ] 数据库连接正常
- [ ] API 文档可访问（http://localhost:8189/swagger-ui.html）
- [ ] 可以正常登录系统（admin / admin123）

## 下一步

系统部署成功后，建议：

1. 修改管理员密码
2. 配置抖音 API 密钥
3. 配置 AI 服务（Ollama/OpenAI/DeepSeek）
4. 导入测试数据
5. 配置定时任务
6. 设置监控告警
