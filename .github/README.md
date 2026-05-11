# CI/CD 配置文件清单

本目录包含完整的 CI/CD 流水线配置文件，用于自动化测试、构建和部署。

---

## 文件列表

### GitHub Actions Workflows

| 文件 | 说明 | 触发条件 |
|------|------|----------|
| `.github/workflows/pr-check.yml` | PR 质量检查 | PR 到 main/sprint-* 分支 |
| `.github/workflows/sprint-automation.yml` | Sprint 任务自动化 | 每日 9:00 AM UTC + 手动触发 |
| `.github/workflows/deploy.yml` | 生产部署 | Tag 推送 + 手动触发 |

### Docker 配置

| 文件 | 说明 |
|------|------|
| `docker/Dockerfile.backend` | 后端 Docker 镜像（多阶段构建）|
| `docker/Dockerfile.frontend` | 前端 Docker 镜像（多阶段构建）|
| `docker/nginx.conf` | Nginx 配置（反向代理 + SPA 路由）|

### 代码质量

| 文件 | 说明 |
|------|------|
| `sonar-project.properties` | SonarQube 配置 |

### 文档

| 文件 | 说明 |
|------|------|
| `docs/CI-CD-GUIDE.md` | CI/CD 完整使用指南 |

---

## 快速开始

### 1. 配置 GitHub Secrets

在 GitHub 仓库的 Settings → Secrets and variables → Actions 中添加：

```
SONAR_TOKEN=<your-sonarqube-token>
SONAR_HOST_URL=<your-sonarqube-url>
DOCKER_USERNAME=<your-docker-username>
DOCKER_PASSWORD=<your-docker-password>
DOCKER_REGISTRY=<your-docker-registry>
API_BASE_URL=<your-api-base-url>
```

### 2. 创建 GitHub Environments

在 Settings → Environments 中创建：
- **staging**: 测试环境
- **production**: 生产环境（需要审批）

### 3. 启用 Workflows

推送代码到 GitHub 后，Workflows 会自动启用。

---

## 使用示例

### 运行 PR 检查

```bash
# 创建功能分支
git checkout -b feature/B1.1-livescript-vo

# 提交代码
git add .
git commit -m "fix(live): 补全 LiveScriptVO 字段"

# 推送并创建 PR
git push origin feature/B1.1-livescript-vo
# 在 GitHub 上创建 PR，CI 会自动运行
```

### 手动触发 Sprint 任务

```bash
# 运行所有任务
gh workflow run sprint-automation.yml -f sprint=1 -f task_id=all

# 运行单个任务
gh workflow run sprint-automation.yml -f sprint=1 -f task_id=B1.1
```

### 部署到生产

```bash
# 打 Tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# 或手动触发
gh workflow run deploy.yml -f environment=production
```

---

## 本地测试

### 测试后端构建

```bash
# 构建 Docker 镜像
docker build -t dy05-backend:test -f docker/Dockerfile.backend .

# 运行容器
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5433/dy05_dev \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  dy05-backend:test

# 健康检查
curl http://localhost:8080/actuator/health
```

### 测试前端构建

```bash
# 构建 Docker 镜像
docker build -t dy05-frontend:test -f docker/Dockerfile.frontend ./front

# 运行容器
docker run -p 3000:80 dy05-frontend:test

# 访问
open http://localhost:3000
```

### 测试 SonarQube 扫描

```bash
# 后端
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=dy05 \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<your-token>

# 前端
cd front
npm run test:coverage
sonar-scanner
```

---

## 工作流程图

```
┌─────────────────┐
│  开发者提交代码  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   PR Check      │ ← 自动触发
│  - 编译检查      │
│  - 单元测试      │
│  - 类型检查      │
│  - 代码质量      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   代码审查       │ ← 人工审查
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  合并到 Sprint   │
│     分支        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Sprint 任务自动化│ ← 每日 9:00 AM
│  - 任务验证      │
│  - 进度报告      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  合并到 main     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 部署到 Staging   │ ← 自动部署
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   打 Tag        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 部署到 Production│ ← 需要审批
│  - 冒烟测试      │
│  - 健康检查      │
└─────────────────┘
```

---

## 监控与告警

### 流水线状态

- 所有 Workflow 运行状态：https://github.com/<org>/<repo>/actions
- PR 检查状态：在 PR 页面查看
- 部署状态：在 Environments 页面查看

### 通知渠道

- GitHub 通知（默认）
- Slack（需配置 `SLACK_WEBHOOK_URL`）
- 企业微信（需配置 `WECOM_WEBHOOK_URL`）
- 邮件（GitHub 自带）

---

## 故障排查

### 常见问题

1. **Docker 构建失败**
   - 检查 Dockerfile 语法
   - 确保所有依赖文件存在
   - 查看构建日志：`docker build --progress=plain`

2. **测试失败**
   - 本地运行测试：`mvn test` / `npm test`
   - 检查数据库连接
   - 查看详细错误日志

3. **部署失败**
   - 检查 Secrets 配置
   - 验证 Docker Registry 访问权限
   - 查看健康检查日志

### 获取帮助

- 查看详细文档：`docs/CI-CD-GUIDE.md`
- 查看 Workflow 日志：GitHub Actions 页面
- 联系 DevOps 团队

---

**创建时间**: 2026-05-11  
**维护者**: 项目团队
