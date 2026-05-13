# 一人公司全自动迭代升级系统设计

**设计日期**：2026-05-13  
**目标**：实现零人工干预的持续代码质量提升与自动化运维

## 设计原则

1. **完全自动化**：所有检查、分析、报告生成无需人工触发
2. **渐进式改进**：每次迭代聚焦最高优先级问题
3. **可观测性**：所有自动化任务生成可追溯的报告
4. **安全优先**：自动化操作不破坏现有功能
5. **成本可控**：优先使用开源工具，AI 调用按需触发

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    定时调度层 (Cron)                         │
│  每日 03:00 触发健康扫描 → 每周日 04:00 深度审计             │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    数据收集层 (Collectors)                   │
│  代码质量 │ 依赖版本 │ 测试覆盖 │ 性能指标 │ 安全漏洞        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    分析决策层 (Analyzers)                    │
│  优先级排序 │ 影响评估 │ 修复建议 │ 风险评级                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    执行层 (Executors)                        │
│  自动修复 │ PR 创建 │ 文档更新 │ 通知推送                    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    报告层 (Reporters)                        │
│  健康仪表盘 │ 趋势分析 │ 改进建议 │ 历史对比                 │
└─────────────────────────────────────────────────────────────┘
```

## 核心模块

### 1. 代码质量自动审计

**工具链**：
- **后端**：Checkstyle + SpotBugs + PMD + SonarQube
- **前端**：ESLint + TypeScript Compiler + Prettier

**自动化流程**：
```bash
# 每日 03:00 执行
./scripts/auto-audit-code-quality.sh
```

**输出**：
- `reports/code-quality/YYYY-MM-DD.json` - 机器可读报告
- `reports/code-quality/YYYY-MM-DD.md` - 人类可读报告
- 自动创建 GitHub Issue（P0 问题）

**修复策略**：
- **自动修复**：格式化问题、未使用导入、简单类型错误
- **PR 建议**：复杂重构、架构调整
- **人工审核**：安全相关、业务逻辑变更

### 2. 依赖版本管理

**工具链**：
- **后端**：Maven Versions Plugin + OWASP Dependency-Check
- **前端**：npm-check-updates + npm audit

**自动化流程**：
```bash
# 每周日 04:00 执行
./scripts/auto-check-dependencies.sh
```

**输出**：
- `reports/dependencies/YYYY-MM-DD.json` - 可升级依赖清单
- `reports/dependencies/security-YYYY-MM-DD.json` - 安全漏洞报告
- 自动创建 PR（安全补丁）

**升级策略**：
- **自动升级**：补丁版本（1.2.3 → 1.2.4）
- **PR 建议**：次版本（1.2.x → 1.3.0）
- **人工决策**：主版本（1.x → 2.0）

### 3. 测试覆盖率监控

**工具链**：
- **后端**：JaCoCo
- **前端**：Vitest Coverage

**自动化流程**：
```bash
# 每次 git push 触发（GitHub Actions）
./scripts/auto-test-coverage.sh
```

**输出**：
- `reports/coverage/backend-YYYY-MM-DD.html` - 后端覆盖率报告
- `reports/coverage/frontend-YYYY-MM-DD.html` - 前端覆盖率报告
- PR 评论（覆盖率变化趋势）

**告警策略**：
- 覆盖率 < 80%：阻止合并
- 覆盖率下降 > 5%：警告
- 新增代码覆盖率 < 60%：阻止合并

### 4. 性能基准测试

**工具链**：
- **后端**：JMH + Apache Bench
- **前端**：Lighthouse CI + Web Vitals

**自动化流程**：
```bash
# 每周日 05:00 执行
./scripts/auto-benchmark.sh
```

**输出**：
- `reports/performance/backend-YYYY-MM-DD.json` - API 响应时间
- `reports/performance/frontend-YYYY-MM-DD.json` - 前端性能指标
- 性能回归告警（响应时间增加 > 20%）

### 5. 安全漏洞扫描

**工具链**：
- **依赖漏洞**：OWASP Dependency-Check + npm audit
- **代码漏洞**：SpotBugs Security + ESLint Security
- **容器漏洞**：Trivy

**自动化流程**：
```bash
# 每日 03:30 执行
./scripts/auto-security-scan.sh
```

**输出**：
- `reports/security/YYYY-MM-DD.json` - 漏洞清单
- 自动创建 GitHub Security Advisory（高危漏洞）
- 企业微信通知（严重漏洞）

### 6. 文档同步检查

**工具链**：
- 自定义脚本（检查 API 文档与代码一致性）
- Swagger 规范校验

**自动化流程**：
```bash
# 每次 git push 触发
./scripts/auto-check-docs.sh
```

**输出**：
- `reports/docs/YYYY-MM-DD.json` - 文档过期清单
- 自动更新 API 文档（Swagger 生成）
- PR 评论（文档缺失提醒）

### 7. 技术债务追踪

**工具链**：
- SonarQube Technical Debt
- 自定义规则（TODO/FIXME 统计）

**自动化流程**：
```bash
# 每周日 06:00 执行
./scripts/auto-track-tech-debt.sh
```

**输出**：
- `reports/tech-debt/YYYY-MM-DD.json` - 技术债务清单
- `reports/tech-debt/trend.png` - 趋势图
- 优先级排序建议

## 实施路线图

### Phase 1: 基础设施搭建（Week 1-2）

**任务清单**：
- [ ] 创建 `scripts/` 目录，编写 7 个自动化脚本
- [ ] 配置 GitHub Actions 工作流（`.github/workflows/`）
- [ ] 搭建 SonarQube 服务器（Docker）
- [ ] 配置报告存储目录（`reports/`）
- [ ] 编写健康仪表盘前端页面

**验收标准**：
- 所有脚本可手动执行成功
- GitHub Actions 触发正常
- 报告生成格式正确

### Phase 2: 自动修复能力（Week 3-4）

**任务清单**：
- [ ] 实现代码格式化自动修复（Prettier + Checkstyle）
- [ ] 实现依赖安全补丁自动升级
- [ ] 实现 PR 自动创建（使用 GitHub CLI）
- [ ] 配置自动合并规则（安全补丁 + 测试通过）

**验收标准**：
- 格式化问题自动修复率 > 95%
- 安全补丁自动升级成功率 > 90%
- PR 创建无需人工干预

### Phase 3: 智能决策层（Week 5-6）

**任务清单**：
- [ ] 集成 Claude API（优先级排序、影响评估）
- [ ] 实现问题聚类（相似问题合并）
- [ ] 实现修复建议生成（基于历史数据）
- [ ] 配置成本控制（API 调用限额）

**验收标准**：
- 优先级排序准确率 > 85%
- API 调用成本 < $50/月
- 修复建议采纳率 > 60%

### Phase 4: 持续监控与优化（Week 7-8）

**任务清单**：
- [ ] 搭建 Grafana 仪表盘（实时监控）
- [ ] 配置告警规则（企业微信 + 邮件）
- [ ] 实现趋势分析（周报 + 月报）
- [ ] 优化脚本性能（并行执行）

**验收标准**：
- 仪表盘实时更新（延迟 < 5 分钟）
- 告警准确率 > 90%（无误报）
- 脚本执行时间 < 10 分钟

## 关键脚本设计

### auto-audit-code-quality.sh

```bash
#!/bin/bash
set -e

DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/code-quality"
mkdir -p "$REPORT_DIR"

echo "=== 后端代码质量检查 ==="
mvn checkstyle:check spotbugs:check pmd:check
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000

echo "=== 前端代码质量检查 ==="
cd front
npm run lint
npm run type-check
cd ..

echo "=== 生成报告 ==="
./scripts/generate-quality-report.py > "$REPORT_DIR/$DATE.json"
./scripts/format-quality-report.py "$REPORT_DIR/$DATE.json" > "$REPORT_DIR/$DATE.md"

echo "=== 创建 Issue（P0 问题）==="
./scripts/create-issues-from-report.py "$REPORT_DIR/$DATE.json"

echo "✅ 代码质量审计完成"
```

### auto-check-dependencies.sh

```bash
#!/bin/bash
set -e

DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/dependencies"
mkdir -p "$REPORT_DIR"

echo "=== 后端依赖检查 ==="
mvn versions:display-dependency-updates -DoutputFile="$REPORT_DIR/backend-updates.txt"
mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7

echo "=== 前端依赖检查 ==="
cd front
npx npm-check-updates --jsonUpgraded > "../$REPORT_DIR/frontend-updates.json"
npm audit --json > "../$REPORT_DIR/frontend-security.json"
cd ..

echo "=== 自动升级安全补丁 ==="
./scripts/auto-upgrade-security-patches.py "$REPORT_DIR"

echo "=== 创建 PR（次版本升级）==="
./scripts/create-dependency-upgrade-pr.py "$REPORT_DIR"

echo "✅ 依赖检查完成"
```

### auto-test-coverage.sh

```bash
#!/bin/bash
set -e

DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/coverage"
mkdir -p "$REPORT_DIR"

echo "=== 后端测试覆盖率 ==="
mvn clean test jacoco:report
cp target/site/jacoco/index.html "$REPORT_DIR/backend-$DATE.html"

echo "=== 前端测试覆盖率 ==="
cd front
npm run test:coverage
cp coverage/index.html "../$REPORT_DIR/frontend-$DATE.html"
cd ..

echo "=== 生成覆盖率报告 ==="
./scripts/generate-coverage-report.py > "$REPORT_DIR/$DATE.json"

echo "=== 检查覆盖率阈值 ==="
./scripts/check-coverage-threshold.py "$REPORT_DIR/$DATE.json"

echo "✅ 测试覆盖率检查完成"
```

## GitHub Actions 工作流

### .github/workflows/daily-health-check.yml

```yaml
name: Daily Health Check

on:
  schedule:
    - cron: '0 3 * * *'  # 每日 03:00 UTC+8
  workflow_dispatch:

jobs:
  health-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
      
      - name: Run Code Quality Audit
        run: ./scripts/auto-audit-code-quality.sh
      
      - name: Run Security Scan
        run: ./scripts/auto-security-scan.sh
      
      - name: Upload Reports
        uses: actions/upload-artifact@v4
        with:
          name: health-reports
          path: reports/
      
      - name: Notify on Failure
        if: failure()
        run: ./scripts/notify-wecom.sh "健康检查失败"
```

### .github/workflows/pr-quality-gate.yml

```yaml
name: PR Quality Gate

on:
  pull_request:
    types: [opened, synchronize]

jobs:
  quality-gate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Tests with Coverage
        run: ./scripts/auto-test-coverage.sh
      
      - name: Check Coverage Threshold
        run: |
          COVERAGE=$(jq '.backend.lineCoverage' reports/coverage/latest.json)
          if (( $(echo "$COVERAGE < 80" | bc -l) )); then
            echo "❌ 覆盖率不足 80%: $COVERAGE%"
            exit 1
          fi
      
      - name: Comment PR
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const report = JSON.parse(fs.readFileSync('reports/coverage/latest.json'));
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `## 测试覆盖率报告\n\n- 后端: ${report.backend.lineCoverage}%\n- 前端: ${report.frontend.lineCoverage}%`
            });
```

## 健康仪表盘设计

### 数据模型

```typescript
interface HealthReport {
  date: string;
  codeQuality: {
    backend: { score: number; issues: Issue[] };
    frontend: { score: number; issues: Issue[] };
  };
  dependencies: {
    outdated: number;
    vulnerabilities: Vulnerability[];
  };
  coverage: {
    backend: number;
    frontend: number;
  };
  performance: {
    apiResponseTime: number;  // ms
    frontendLoadTime: number; // ms
  };
  techDebt: {
    total: number;  // 小时
    trend: 'up' | 'down' | 'stable';
  };
}
```

### 前端页面路由

- `/system/health` - 健康仪表盘首页
- `/system/health/code-quality` - 代码质量详情
- `/system/health/dependencies` - 依赖管理
- `/system/health/coverage` - 测试覆盖率
- `/system/health/performance` - 性能基准
- `/system/health/tech-debt` - 技术债务

## 成本估算

| 项目 | 工具 | 成本 |
|------|------|------|
| 代码质量 | SonarQube Community | 免费 |
| 依赖检查 | OWASP + npm audit | 免费 |
| 测试覆盖 | JaCoCo + Vitest | 免费 |
| 性能测试 | JMH + Lighthouse | 免费 |
| 安全扫描 | Trivy + SpotBugs | 免费 |
| CI/CD | GitHub Actions | 免费（2000 分钟/月）|
| AI 决策 | Claude API | ~$30/月 |
| 监控告警 | Grafana + Prometheus | 免费 |
| **总计** | | **~$30/月** |

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 自动修复引入 bug | 高 | 所有自动修复必须通过测试 + 人工审核 PR |
| 依赖升级破坏兼容性 | 中 | 仅自动升级补丁版本，次版本需人工确认 |
| CI/CD 资源耗尽 | 中 | 限制并发任务数，优先级队列 |
| 误报导致告警疲劳 | 低 | 调整阈值，白名单机制 |
| API 成本超支 | 低 | 设置月度限额 $50，超额暂停 |

## 成功指标

### 短期指标（1 个月）

- [ ] 代码质量分数 > 85/100（SonarQube）
- [ ] 测试覆盖率 > 80%（后端 + 前端）
- [ ] 安全漏洞 = 0（高危 + 严重）
- [ ] 依赖过期率 < 10%
- [ ] 自动化脚本执行成功率 > 95%

### 中期指标（3 个月）

- [ ] 技术债务减少 30%
- [ ] API 响应时间改善 20%
- [ ] 前端加载时间 < 2 秒
- [ ] 自动修复率 > 60%（无需人工干预）
- [ ] 问题发现到修复时间 < 24 小时

### 长期指标（6 个月）

- [ ] 代码质量分数 > 90/100
- [ ] 测试覆盖率 > 85%
- [ ] 零安全漏洞（持续 3 个月）
- [ ] 技术债务减少 50%
- [ ] 完全自动化运维（人工干预 < 5%）

## 快速启动

### 前置条件

- Docker Desktop 已安装
- GitHub CLI (`gh`) 已安装
- Python 3.9+ 已安装
- 企业微信 Webhook 已配置（可选）

### 初始化步骤

```bash
# 1. 创建脚本目录
mkdir -p scripts reports/{code-quality,dependencies,coverage,performance,security,tech-debt}

# 2. 安装 Python 依赖
pip install -r scripts/requirements.txt

# 3. 配置环境变量
cp .env.example .env
# 编辑 .env，填入 SONAR_TOKEN, CLAUDE_API_KEY, WECOM_WEBHOOK

# 4. 启动 SonarQube
docker compose -f docker/docker-compose.yml up -d sonarqube

# 5. 手动执行首次扫描
./scripts/auto-audit-code-quality.sh

# 6. 配置 GitHub Actions
gh workflow enable daily-health-check
gh workflow enable pr-quality-gate

# 7. 访问健康仪表盘
# 前端：http://localhost:3000/system/health
# SonarQube：http://localhost:9000
```

### 验证安装

```bash
# 检查所有脚本可执行
ls -l scripts/*.sh | grep -v 'x'  # 应无输出

# 手动触发健康检查
gh workflow run daily-health-check

# 查看最新报告
cat reports/code-quality/$(ls -t reports/code-quality | head -1)
```

## 维护与演进

### 每周维护任务

- 审查自动创建的 PR（优先级：安全 > 依赖 > 重构）
- 检查告警准确率，调整阈值
- 审查技术债务清单，规划修复

### 每月优化任务

- 分析趋势报告，识别系统性问题
- 优化脚本性能（并行化、缓存）
- 更新自动修复规则（基于历史数据）

### 每季度审计任务

- 评估自动化效果（成功指标达成率）
- 调整优先级策略（基于业务变化）
- 升级工具链版本

## 附录：工具链版本

| 工具 | 版本 | 用途 |
|------|------|------|
| SonarQube | 10.4 Community | 代码质量分析 |
| Checkstyle | 10.12 | Java 代码风格 |
| SpotBugs | 4.8 | Java 静态分析 |
| PMD | 7.0 | Java 代码检查 |
| ESLint | 8.57 | JavaScript/TypeScript 检查 |
| JaCoCo | 0.8.11 | Java 覆盖率 |
| Vitest | 1.6 | 前端测试 + 覆盖率 |
| OWASP Dependency-Check | 9.0 | 依赖漏洞扫描 |
| Trivy | 0.50 | 容器漏洞扫描 |
| Lighthouse | 11.7 | 前端性能测试 |

---

**文档版本**：v1.0  
**创建日期**：2026-05-13  
**下次审计**：2026-06-13（1 个月后）
