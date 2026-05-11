# GitHub Actions Workflows

本目录包含所有 GitHub Actions 工作流配置文件。

---

## Workflows 列表

### 1. PR Quality Check (`pr-check.yml`)

**用途**: 自动检查 Pull Request 的代码质量

**触发条件**:
- Pull Request 到 `main` 或 `sprint-*` 分支
- Push 到 `main` 或 `sprint-*` 分支

**执行内容**:
- ✅ 后端编译检查
- ✅ 后端单元测试
- ✅ 后端测试覆盖率（目标 80%）
- ✅ 前端类型检查
- ✅ 前端单元测试
- ✅ 前端构建验证
- ✅ SonarQube 代码扫描
- ✅ Quality Gate 检查

**预计运行时间**: 8-12 分钟

---

### 2. Sprint Task Automation (`sprint-automation.yml`)

**用途**: 自动化 Sprint 任务验证和进度追踪

**触发条件**:
- 每日 9:00 AM UTC (北京时间 5:00 PM)
- 手动触发（可指定 Sprint 和任务 ID）

**执行内容**:
- ✅ 检查 Sprint 状态
- ✅ 验证后端任务（B1.1, B1.2, B1.3...）
- ✅ 验证前端任务（F1.1, F1.2...）
- ✅ 生成每日进度报告
- ✅ 失败时发送通知

**手动触发示例**:
```bash
# 运行所有任务
gh workflow run sprint-automation.yml -f sprint=1 -f task_id=all

# 运行单个任务
gh workflow run sprint-automation.yml -f sprint=1 -f task_id=B1.1
```

**预计运行时间**: 5-8 分钟

---

### 3. Deploy to Production (`deploy.yml`)

**用途**: 自动化部署到 Staging 和 Production 环境

**触发条件**:
- 推送 Tag (`v*.*.*` 格式)
- 手动触发（可选择环境）

**执行内容**:
- ✅ 构建后端 Docker 镜像
- ✅ 构建前端 Docker 镜像
- ✅ 推送到 Docker Registry
- ✅ 部署到目标环境
- ✅ 运行冒烟测试
- ✅ 失败时自动回滚

**手动触发示例**:
```bash
# 部署到 Staging
gh workflow run deploy.yml -f environment=staging

# 部署到 Production
gh workflow run deploy.yml -f environment=production
```

**预计运行时间**: 10-15 分钟

---

## 使用指南

### 查看 Workflow 运行状态

```bash
# 列出所有 Workflow
gh workflow list

# 查看特定 Workflow 的运行历史
gh run list --workflow=pr-check.yml

# 查看运行详情
gh run view <run-id>

# 查看运行日志
gh run view <run-id> --log
```

### 手动触发 Workflow

```bash
# 触发 Sprint 任务自动化
gh workflow run sprint-automation.yml \
  -f sprint=1 \
  -f task_id=all

# 触发部署
gh workflow run deploy.yml \
  -f environment=staging
```

### 取消运行中的 Workflow

```bash
# 取消特定运行
gh run cancel <run-id>

# 取消所有运行中的 Workflow
gh run list --status in_progress --json databaseId -q '.[].databaseId' | xargs -I {} gh run cancel {}
```

---

## 配置要求

### GitHub Secrets

在 Settings → Secrets and variables → Actions 中配置：

| Secret | 说明 | 必需 |
|--------|------|------|
| `SONAR_TOKEN` | SonarQube 访问令牌 | PR Check |
| `SONAR_HOST_URL` | SonarQube 服务器地址 | PR Check |
| `DOCKER_USERNAME` | Docker Registry 用户名 | Deploy |
| `DOCKER_PASSWORD` | Docker Registry 密码 | Deploy |
| `DOCKER_REGISTRY` | Docker Registry 地址 | Deploy |
| `API_BASE_URL` | 前端 API 基础 URL | Deploy |

### GitHub Environments

在 Settings → Environments 中创建：

#### Staging
- **URL**: `https://staging.dy05.example.com`
- **Protection rules**: 无需审批

#### Production
- **URL**: `https://dy05.example.com`
- **Protection rules**:
  - Required reviewers: 至少 1 人
  - Wait timer: 5 分钟

---

## 故障排查

### PR Check 失败

**问题**: 后端测试失败
```bash
# 本地运行测试
mvn test

# 查看详细日志
mvn test -X
```

**问题**: 前端类型检查失败
```bash
# 本地运行类型检查
cd front
npm run type-check -- --pretty
```

**问题**: SonarQube 扫描失败
```bash
# 检查 sonar-project.properties 配置
cat sonar-project.properties

# 本地运行扫描
mvn sonar:sonar -Dsonar.login=<token>
```

### Sprint Automation 失败

**问题**: 任务验证失败
```bash
# 检查文件是否存在
test -f douyin-operations-live/src/main/java/.../LiveScriptVO.java

# 手动运行验证
mvn compile -pl douyin-operations-live -am
```

**问题**: 每日报告生成失败
```bash
# 检查 SPRINT-1-TASKS.md 格式
grep "⚠️ TODO" docs/SPRINT-1-TASKS.md
```

### Deploy 失败

**问题**: Docker 构建失败
```bash
# 本地构建测试
docker build -t test -f docker/Dockerfile.backend . --progress=plain
```

**问题**: 部署后健康检查失败
```bash
# 检查服务状态
curl -v http://<service-url>/actuator/health
```

---

## 最佳实践

### 1. 分支策略

- `main`: 主分支，保护分支
- `sprint-N`: Sprint 分支
- `feature/xxx`: 功能分支
- `hotfix/xxx`: 紧急修复分支

### 2. PR 流程

1. 创建功能分支
2. 完成开发并提交
3. 推送到远程
4. 创建 PR
5. 等待 CI 检查通过
6. 代码审查
7. 合并到 Sprint 分支

### 3. 部署策略

- **Staging**: 每次合并到 main 自动部署
- **Production**: 打 Tag 后手动触发
- Tag 格式: `v<major>.<minor>.<patch>`

---

## 监控与通知

### 通知渠道

- GitHub 通知（默认）
- Slack（需配置）
- 企业微信（需配置）
- 邮件（GitHub 自带）

### 添加 Slack 通知

在 Workflow 中添加：

```yaml
- name: Send Slack notification
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "Deployment completed"
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

**创建时间**: 2026-05-11  
**维护者**: 项目团队
