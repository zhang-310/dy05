# CI/CD 流水线配置说明

**创建时间**: 2026-05-11  
**版本**: v1.0

---

## 概述

本项目使用 GitHub Actions 实现 CI/CD 自动化，包含三条主要流水线：

1. **PR Quality Check** - 代码质量检查（每次 PR 自动触发）
2. **Sprint Task Automation** - Sprint 任务自动化（每日定时 + 手动触发）
3. **Deploy to Production** - 生产部署（Tag 触发 + 手动触发）

---

## 流水线详情

### 1. PR Quality Check (`.github/workflows/pr-check.yml`)

**触发条件**:
- Pull Request 到 `main` 或 `sprint-*` 分支
- Push 到 `main` 或 `sprint-*` 分支

**执行内容**:

#### Backend Check
- 启动 PostgreSQL 15 + Redis 7 服务
- 编译后端代码 (`mvn compile`)
- 运行单元测试 (`mvn test`)
- 检查测试覆盖率 (`mvn verify`)
- 上传覆盖率报告到 Codecov

#### Frontend Check
- 安装依赖 (`npm ci`)
- TypeScript 类型检查 (`npm run type-check`)
- 运行前端测试 (`npm run test:coverage`)
- 构建生产版本 (`npm run build`)
- 上传覆盖率报告到 Codecov

#### Code Quality
- SonarQube 代码扫描
- Quality Gate 检查

**验收标准**:
- 所有测试通过
- 测试覆盖率 ≥ 80%
- TypeScript 无类型错误
- SonarQube Quality Gate 通过

---

### 2. Sprint Task Automation (`.github/workflows/sprint-automation.yml`)

**触发条件**:
- 每日 9:00 AM UTC (北京时间 5:00 PM) 自动运行
- 手动触发（可指定 Sprint 和任务 ID）

**执行内容**:

#### Check Sprint Status
- 读取当前 Sprint 编号
- 统计待完成任务数量

#### Run Backend Tasks
- **Task B1.1**: 验证 LiveScriptVO 包含 30 个字段
- **Task B1.2**: 验证 LiveSessionSaveVO 包含必需字段
- **Task B1.3**: 验证缓存配置正确
- 运行所有后端测试

#### Run Frontend Tasks
- **Task F1.1**: 验证 LiveScript 类型定义
- **Task F1.2**: 验证 LiveSession 类型定义
- 运行类型检查和测试
- 构建前端

#### Daily Report
- 生成每日进度报告
- 统计任务状态（TODO/IN PROGRESS/DONE）
- 失败时发送通知

**手动触发示例**:
```bash
# 运行 Sprint 1 的所有任务
gh workflow run sprint-automation.yml -f sprint=1 -f task_id=all

# 运行 Sprint 1 的单个任务
gh workflow run sprint-automation.yml -f sprint=1 -f task_id=B1.1
```

---

### 3. Deploy to Production (`.github/workflows/deploy.yml`)

**触发条件**:
- 推送 Tag (`v*.*.*` 格式，如 `v1.0.0`)
- 手动触发（可选择 staging 或 production）

**执行内容**:

#### Build Backend
- 打包后端 JAR (`mvn package -DskipTests`)
- 构建 Docker 镜像
- 推送到 Docker Registry
- 上传 JAR 文件为 artifact

#### Build Frontend
- 构建前端生产版本 (`npm run build`)
- 构建 Docker 镜像
- 推送到 Docker Registry
- 上传 dist 文件为 artifact

#### Deploy to Staging
- 部署到 Staging 环境
- 运行冒烟测试（健康检查 + 基础 API 测试）

#### Deploy to Production
- 部署到 Production 环境
- 运行冒烟测试
- 成功时发送通知
- 失败时自动回滚

**手动触发示例**:
```bash
# 部署到 Staging
gh workflow run deploy.yml -f environment=staging

# 部署到 Production
gh workflow run deploy.yml -f environment=production
```

---

## 环境变量配置

### GitHub Secrets 配置

在 GitHub 仓库的 Settings → Secrets and variables → Actions 中配置：

| Secret | 说明 | 示例 |
|--------|------|------|
| `SONAR_TOKEN` | SonarQube 访问令牌 | `sqp_xxxxx` |
| `SONAR_HOST_URL` | SonarQube 服务器地址 | `https://sonarqube.example.com` |
| `DOCKER_USERNAME` | Docker Registry 用户名 | `dy05-ci` |
| `DOCKER_PASSWORD` | Docker Registry 密码 | `xxxxx` |
| `DOCKER_REGISTRY` | Docker Registry 地址 | `registry.example.com/dy05` |
| `API_BASE_URL` | 前端 API 基础 URL | `https://api.dy05.example.com` |

### 环境配置

在 GitHub 仓库的 Settings → Environments 中创建：

#### Staging Environment
- **URL**: `https://staging.dy05.example.com`
- **Protection rules**: 无需审批

#### Production Environment
- **URL**: `https://dy05.example.com`
- **Protection rules**: 
  - Required reviewers: 至少 1 人审批
  - Wait timer: 5 分钟

---

## 本地测试

### 测试 PR Check 流水线

```bash
# 后端
docker compose -f docker/docker-compose.yml up -d postgres redis
mvn compile
mvn test
mvn verify

# 前端
cd front
npm ci
npm run type-check
npm run test:coverage
npm run build
```

### 测试 Sprint Automation

```bash
# 检查 Sprint 状态
grep "Sprint" docs/SPRINT-1-TASKS.md
grep -c "⚠️ TODO" docs/SPRINT-1-TASKS.md

# 验证后端任务
grep -c "private" douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/vo/LiveScriptVO.java
mvn compile -pl douyin-operations-live -am

# 验证前端任务
grep "scriptType" front/src/types/live.ts
cd front && npm run type-check
```

### 测试部署流水线

```bash
# 构建后端 Docker 镜像
docker build -t dy05-backend:test -f docker/Dockerfile.backend .

# 构建前端 Docker 镜像
docker build -t dy05-frontend:test -f docker/Dockerfile.frontend ./front

# 测试健康检查
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/auth/health
```

---

## 故障排查

### PR Check 失败

**问题**: 后端测试失败
```bash
# 检查数据库连接
docker compose -f docker/docker-compose.yml logs postgres

# 本地运行测试
mvn test -Dtest=<TestClassName>
```

**问题**: 前端类型检查失败
```bash
# 查看详细错误
cd front
npm run type-check -- --pretty

# 修复类型错误后重新检查
npm run type-check
```

**问题**: 测试覆盖率不足
```bash
# 查看覆盖率报告
mvn verify
open target/site/jacoco/index.html

# 前端覆盖率
cd front
npm run test:coverage
open coverage/index.html
```

### Sprint Automation 失败

**问题**: 任务验证失败
```bash
# 检查文件是否存在
test -f douyin-operations-live/src/main/java/.../LiveScriptVO.java

# 检查字段数量
grep -c "private" douyin-operations-live/src/main/java/.../LiveScriptVO.java

# 手动编译验证
mvn compile -pl douyin-operations-live -am
```

**问题**: 每日报告生成失败
```bash
# 检查 SPRINT-1-TASKS.md 格式
grep "⚠️ TODO" docs/SPRINT-1-TASKS.md
grep "🔄 IN PROGRESS" docs/SPRINT-1-TASKS.md
grep "✅ DONE" docs/SPRINT-1-TASKS.md
```

### Deploy 失败

**问题**: Docker 镜像构建失败
```bash
# 检查 Dockerfile
docker build -t test -f docker/Dockerfile.backend . --progress=plain

# 检查依赖
mvn dependency:tree
```

**问题**: 部署后健康检查失败
```bash
# 检查服务状态
kubectl get pods -n dy05
kubectl logs -n dy05 <pod-name>

# 检查健康端点
curl -v http://<service-url>/actuator/health
```

**问题**: 回滚失败
```bash
# 手动回滚
kubectl rollout undo deployment/dy05-backend -n dy05
kubectl rollout undo deployment/dy05-frontend -n dy05

# 检查回滚状态
kubectl rollout status deployment/dy05-backend -n dy05
```

---

## 最佳实践

### 1. 分支策略

- **main**: 主分支，保护分支，只能通过 PR 合并
- **sprint-N**: Sprint 分支，如 `sprint-1-p0-fixes`
- **feature/xxx**: 功能分支，从 sprint 分支创建
- **hotfix/xxx**: 紧急修复分支，从 main 创建

### 2. PR 流程

1. 创建功能分支: `git checkout -b feature/B1.1-livescript-vo`
2. 完成开发并提交
3. 推送到远程: `git push origin feature/B1.1-livescript-vo`
4. 创建 PR 到 sprint 分支
5. 等待 CI 检查通过
6. 代码审查通过后合并

### 3. Sprint 任务管理

- 每日自动运行任务验证
- 手动触发用于快速验证单个任务
- 任务完成后更新 `SPRINT-1-TASKS.md` 状态
- 每日站会前查看自动生成的报告

### 4. 部署策略

- **Staging**: 每次合并到 main 自动部署
- **Production**: 打 Tag 后手动触发部署
- Tag 格式: `v<major>.<minor>.<patch>` (如 `v1.0.0`)
- 生产部署需要至少 1 人审批

### 5. 监控与告警

- 所有流水线失败时发送通知
- 生产部署成功/失败发送通知
- 每日 Sprint 报告自动生成
- 测试覆盖率低于 80% 时告警

---

## 扩展配置

### 添加新的 Sprint 任务

编辑 `.github/workflows/sprint-automation.yml`，在 `run-backend-tasks` 或 `run-frontend-tasks` 中添加新步骤：

```yaml
- name: Task B1.4 - New task
  if: ${{ github.event.inputs.task_id == 'B1.4' || github.event.inputs.task_id == 'all' }}
  run: |
    echo "Executing Task B1.4: Description"
    # 验证逻辑
    echo "✅ Task B1.4 completed"
```

### 集成其他工具

#### Slack 通知
```yaml
- name: Send Slack notification
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "Deployment to production completed"
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

#### 企业微信通知
```yaml
- name: Send WeChat notification
  run: |
    curl -X POST "${{ secrets.WECOM_WEBHOOK_URL }}" \
      -H "Content-Type: application/json" \
      -d '{"msgtype":"text","text":{"content":"部署成功"}}'
```

---

## 参考资源

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Maven CI/CD 最佳实践](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Docker 多阶段构建](https://docs.docker.com/build/building/multi-stage/)
- [SonarQube 集成](https://docs.sonarqube.org/latest/analysis/github-integration/)

---

**维护者**: 项目团队  
**最后更新**: 2026-05-11
