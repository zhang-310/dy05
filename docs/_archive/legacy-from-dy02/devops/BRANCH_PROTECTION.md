# 分支保护策略配置指南

## 概述

本文档详细说明如何为 Douyin Operations 项目配置 GitHub 分支保护规则，确保代码质量和部署安全性。

---

## 目录

1. [Main 分支保护策略](#main-分支保护策略)
2. [Develop 分支保护策略](#develop-分支保护策略)
3. [Release 分支保护策略](#release-分支保护策略)
4. [Hotfix 分支保护策略](#hotfix-分支保护策略)
5. [GitHub 配置步骤](#github-配置步骤)
6. [保护规则详解](#保护规则详解)
7. [工作流集成](#工作流集成)
8. [常见问题](#常见问题)

---

## Main 分支保护策略

**分支**: `main`
**用途**: 生产环境代码，每个提交都应该是稳定的发布版本
**保护级别**: 最高级

### 配置规则

| 规则 | 启用 | 说明 |
|------|------|------|
| Require pull request reviews before merging | ✅ | 必须有 PR 审查 |
| Dismiss stale pull request approvals | ✅ | 新提交后自动驳回旧的批准 |
| Require review from Code Owners | ✅ | 必须从代码所有者获取批准 |
| Number of required reviews | 2 | 至少需要 2 个批准 |
| Require status checks to pass | ✅ | 必须通过所有状态检查 |
| Require branches to be up to date | ✅ | 必须与 base branch 最新同步 |
| Require code scanning results | ✅ | 必须通过代码扫描 |
| Require deployments to succeed | ✅ | 部署必须成功 |
| Block force pushes | ✅ | 禁止强制推送 |
| Restrict who can push | ✅ | 限制能推送代码的人员 |

### 必须通过的检查

```
✓ test-backend
✓ test-frontend
✓ quality-checks
✓ docker-build
✓ deploy
✓ code-scanning (CodeQL)
✓ sonarqube-quality-gate
```

### 代码所有者

在 `.github/CODEOWNERS` 文件中定义:

```
# 全局所有者
* @admin-team

# 后端代码
src/main/java/ @backend-team
pom.xml @backend-team

# 前端代码
frontend/src/ @frontend-team
frontend/package.json @frontend-team

# Docker & DevOps
Dockerfile* @devops-team
docker-compose.yml @devops-team
.github/workflows/ @devops-team

# 文档
docs/ @tech-lead
*.md @tech-lead
```

---

## Develop 分支保护策略

**分支**: `develop`
**用途**: 开发环境代码，用于集成开发分支
**保护级别**: 中等

### 配置规则

| 规则 | 启用 | 说明 |
|------|------|------|
| Require pull request reviews before merging | ✅ | 必须有 PR 审查 |
| Dismiss stale pull request approvals | ✅ | 新提交后自动驳回旧的批准 |
| Number of required reviews | 1 | 至少需要 1 个批准 |
| Require status checks to pass | ✅ | 必须通过所有状态检查 |
| Require branches to be up to date | ✅ | 必须与 base branch 最新同步 |
| Block force pushes | ✅ | 禁止强制推送 |
| Allow force pushes | 仅 Admins | 仅管理员可强制推送 |

### 必须通过的检查

```
✓ test-backend
✓ test-frontend
✓ quality-checks
✓ docker-build
```

---

## Release 分支保护策略

**分支**: `release/*`
**用途**: 发布分支，用于准备生产环境发布
**保护级别**: 高

### 配置规则

| 规则 | 启用 | 说明 |
|------|------|------|
| Require pull request reviews before merging | ✅ | 必须有 PR 审查 |
| Number of required reviews | 1 | 至少需要 1 个批准 |
| Require status checks to pass | ✅ | 必须通过所有状态检查 |
| Require branches to be up to date | ✅ | 必须与 base branch 最新同步 |
| Block force pushes | ✅ | 禁止强制推送 |

---

## Hotfix 分支保护策略

**分支**: `hotfix/*`
**用途**: 紧急修复分支，直接从 main 分支创建
**保护级别**: 高

### 配置规则

| 规则 | 启用 | 说明 |
|------|------|------|
| Require pull request reviews before merging | ✅ | 必须有 PR 审查 |
| Number of required reviews | 1 | 至少需要 1 个批准 |
| Require status checks to pass | ✅ | 必须通过所有状态检查 |
| Block force pushes | ✅ | 禁止强制推送 |

---

## GitHub 配置步骤

### 第 1 步：访问 Settings

1. 进入 GitHub 仓库
2. 点击 **Settings** 选项卡
3. 在左侧菜单中选择 **Branches**

### 第 2 步：添加分支保护规则

1. 点击 **Add rule**
2. 输入分支名称模式（例如：`main`, `develop`, `release/*`）
3. 勾选所需的保护规则

### 第 3 步：配置状态检查

1. 勾选 **Require status checks to pass before merging**
2. 勾选 **Require branches to be up to date before merging**
3. 在搜索框中搜索并添加以下检查：

```
- test-backend
- test-frontend
- quality-checks
- docker-build
- deploy (仅 main 分支)
```

### 第 4 步：配置代码所有者审查

1. 在 `.github/CODEOWNERS` 文件中定义代码所有者
2. 勾选 **Require review from Code Owners**
3. 勾选 **Dismiss stale pull request approvals**

### 第 5 步：配置管理员例外

1. 选择 **Allow force pushes**
2. 选择 **Restricted to admins** (仅在 develop 分支)

### 第 6 步：配置分解规则

1. 勾选 **Restrict who can push to matching branches**
2. 选择允许推送的用户/团队

---

## 保护规则详解

### 必须通过的状态检查

#### test-backend (30 分钟超时)
- **触发条件**: 任何 push 或 PR
- **验证内容**: Maven 单元测试、集成测试
- **失败处理**: 阻止 merge

#### test-frontend (15 分钟超时)
- **触发条件**: 任何 push 或 PR
- **验证内容**: npm 测试、TypeScript 类型检查
- **失败处理**: 阻止 merge

#### quality-checks (30 分钟超时)
- **触发条件**: 任何 push 或 PR
- **验证内容**: SonarQube、代码覆盖率
- **失败处理**: 阻止 merge（main 分支）/ 警告（develop 分支）

#### docker-build (45 分钟超时)
- **触发条件**: main 或 develop 分支的 push
- **验证内容**: Docker 镜像构建、推送到 GHCR
- **失败处理**: 阻止 merge

#### deploy (10 分钟超时)
- **触发条件**: main 分支的 push
- **验证内容**: 部署到生产环境成功
- **失败处理**: 阻止合并下一个 PR

### 代码审查要求

#### Main 分支
- **所需批准数**: 2
- **驳回过时的批准**: 是
- **需要代码所有者批准**: 是
- **申请者可自我批准**: 否

#### Develop 分支
- **所需批准数**: 1
- **驳回过时的批准**: 是
- **需要代码所有者批准**: 否
- **申请者可自我批准**: 否

### 强制推送和删除

| 分支 | 强制推送 | 删除 | 备注 |
|------|---------|------|------|
| main | ❌ 禁止 | ❌ 禁止 | 绝不允许 |
| develop | 仅 Admins | 仅 Admins | 需管理员权限 |
| release/* | ❌ 禁止 | ❌ 禁止 | 保护发布过程 |
| hotfix/* | ❌ 禁止 | ❌ 禁止 | 保护修复过程 |

---

## 工作流集成

### GitHub Actions 与分支保护的集成

当分支保护规则要求特定的状态检查通过时，GitHub Actions 工作流会自动触发：

1. **自动触发**

```yaml
on:
  push:
    branches: [main, develop, release/*, hotfix/*]
  pull_request:
    branches: [main, develop]
```

2. **状态报告**

工作流完成后，GitHub 自动将结果发送给分支保护规则检查器。

3. **阻止合并**

如果任何必须通过的检查失败，PR 的 merge 按钮将被禁用，显示：
```
This branch has 1 failing check
```

### 创建 CODEOWNERS 文件

```bash
cat > .github/CODEOWNERS << 'EOF'
# Douyin Operations CODEOWNERS

# 全局所有者 - 最终代码审查者
* @admin-team @tech-lead

# 后端代码所有者
src/main/java/ @backend-team
pom.xml @backend-team
Dockerfile @devops-team

# 前端代码所有者
frontend/src/ @frontend-team
frontend/package.json @frontend-team
frontend/Dockerfile @devops-team

# DevOps 和基础设施
.github/workflows/ @devops-team
docker-compose.yml @devops-team
kubernetes/ @devops-team

# 文档
docs/ @tech-lead
*.md @tech-lead
IMPLEMENTATION_GUIDE.md @tech-lead

# 配置文件
.env.example @devops-team
application*.properties @devops-team
application*.yml @devops-team
EOF
```

---

## 常见问题

### Q: 如何在紧急情况下跳过分支保护规则？

A: 不建议跳过规则。但在必要时，仅管理员可：
1. 在 main 分支配置中选择 **Allow force pushes** (不推荐)
2. 或通过 GitHub CLI 临时禁用规则（需要管理员权限）

```bash
# 临时禁用规则 (24 小时)
gh api repos/{owner}/{repo}/branches/main/protection/enforce_admins -X DELETE

# 重新启用
gh api repos/{owner}/{repo}/branches/main/protection/enforce_admins -X POST
```

### Q: 如何更新已通过审查的 PR 的提交？

A: 新提交将自动触发:
1. 所有状态检查重新运行
2. 过时的批准被驳回
3. 需要新的批准

建议使用 `git commit --amend` 和 `git push --force-with-lease` 来更新提交。

### Q: 代码所有者的审查不显示？

A: 检查以下内容：
1. `.github/CODEOWNERS` 文件存在且格式正确
2. 用户名必须是 GitHub 用户名或团队引用
3. 勾选了 **Require review from Code Owners**
4. 用户不在 PR 的申请者中

### Q: 如何配置多个代码所有者？

A: 在 CODEOWNERS 中使用空格分隔多个用户：

```
src/main/java/cn/gaifan/douyinOperations/module/auth/ @auth-lead @devops-team
```

### Q: 合并 hotfix 分支时是否需要特殊处理？

A: Hotfix 分支应该：
1. 从 `main` 创建
2. 修复完成后创建 PR 到 `main` 和 `develop`
3. 遵循相同的分支保护规则
4. 两个 PR 都必须通过审查和检查

---

## 最佳实践

### 1. 定期审查保护规则

```bash
# 导出当前配置
gh api repos/{owner}/{repo}/branches/main/protection --jq '.' > main-protection.json
```

### 2. 使用分支命名约定

- `main` - 生产环境，标记版本
- `develop` - 开发环境，集成分支
- `feature/xxx` - 功能开发
- `bugfix/xxx` - 错误修复
- `hotfix/xxx` - 紧急修复
- `release/x.x.x` - 发布准备

### 3. 保持 CODEOWNERS 更新

- 新成员加入时更新
- 成员离职时移除
- 定期审查权限

### 4. 监控分支保护活动

```bash
# 查看最近的 merge 活动
gh pr list --state merged --limit 20 --json title,mergedAt,author
```

### 5. 文档化例外情况

如果需要跳过某个检查，应该：
1. 创建 issue 记录原因
2. 获取高级管理员批准
3. 在 PR 中链接 issue
4. 事后进行 review

---

## 配置脚本

### 使用 GitHub CLI 自动配置

```bash
#!/bin/bash
set -e

OWNER="your-org"
REPO="douyin-operations"

echo "Configuring branch protection for main branch..."

# 配置 main 分支
gh api repos/$OWNER/$REPO/branches/main/protection \
  --input - <<EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "test-backend",
      "test-frontend",
      "quality-checks",
      "docker-build",
      "deploy"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": true,
    "required_approving_review_count": 2,
    "require_last_push_approval": false
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_linear_history": false,
  "required_deployments": {
    "strict_required_status_checks_policy": true,
    "environments": ["production"]
  }
}
EOF

echo "✅ Main branch protection configured"

# 配置 develop 分支
echo "Configuring branch protection for develop branch..."

gh api repos/$OWNER/$REPO/branches/develop/protection \
  --input - <<EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "test-backend",
      "test-frontend",
      "quality-checks",
      "docker-build"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "allow_force_pushes": {
    "restricted_to_admins": true
  },
  "allow_deletions": false
}
EOF

echo "✅ Develop branch protection configured"

# 配置 release/* 和 hotfix/* 分支
for pattern in "release/*" "hotfix/*"; do
  echo "Configuring branch protection for $pattern..."

  gh api repos/$OWNER/$REPO/branches/$pattern/protection \
    --input - <<EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "test-backend",
      "test-frontend",
      "quality-checks"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "required_approving_review_count": 1
  },
  "allow_force_pushes": false,
  "allow_deletions": false
}
EOF

  echo "✅ $pattern branch protection configured"
done

echo ""
echo "✅ All branch protection rules configured successfully!"
```

### 保存脚本

```bash
chmod +x configure-branch-protection.sh
./configure-branch-protection.sh
```

---

## 总结

通过实施这些分支保护策略，我们可以：

✅ **提高代码质量** - 确保所有代码都经过审查和测试
✅ **减少生产事故** - 阻止不合适的代码部署
✅ **规范工作流程** - 统一开发工作流
✅ **审计跟踪** - 记录所有更改和审查
✅ **自动化流程** - 减少手动操作

遵循这些规则，团队可以安全而高效地开发和部署代码。
