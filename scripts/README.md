# 自动化脚本说明

本目录包含一人公司全自动迭代升级系统的核心脚本。

## 脚本清单

| 脚本 | 触发方式 | 执行频率 | 说明 |
|------|----------|----------|------|
| `auto-audit-code-quality.sh` | Cron | 每日 03:00 | 代码质量审计 |
| `auto-check-dependencies.sh` | Cron | 每周日 04:00 | 依赖版本检查 |
| `auto-test-coverage.sh` | Git Push | 每次推送 | 测试覆盖率检查 |
| `auto-benchmark.sh` | Cron | 每周日 05:00 | 性能基准测试 |
| `auto-security-scan.sh` | Cron | 每日 03:30 | 安全漏洞扫描 |
| `auto-check-docs.sh` | Git Push | 每次推送 | 文档同步检查 |
| `auto-track-tech-debt.sh` | Cron | 每周日 06:00 | 技术债务追踪 |

## Python 辅助脚本

| 脚本 | 说明 |
|------|------|
| `generate-quality-report.py` | 生成代码质量 JSON 报告 |
| `format-quality-report.py` | 格式化为 Markdown 报告 |
| `create-issues-from-report.py` | 从报告创建 GitHub Issue |
| `auto-upgrade-security-patches.py` | 自动升级安全补丁 |
| `create-dependency-upgrade-pr.py` | 创建依赖升级 PR |
| `generate-coverage-report.py` | 生成覆盖率报告 |
| `check-coverage-threshold.py` | 检查覆盖率阈值 |
| `notify-wecom.sh` | 企业微信通知 |

## 环境变量

在 `.env` 文件中配置：

```bash
# SonarQube
SONAR_TOKEN=your_sonar_token
SONAR_HOST_URL=http://localhost:9000

# Claude API（用于智能决策）
CLAUDE_API_KEY=your_claude_api_key

# 企业微信通知
WECOM_WEBHOOK=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx

# GitHub
GITHUB_TOKEN=your_github_token
```

## 快速开始

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 赋予执行权限
chmod +x *.sh

# 3. 手动执行测试
./auto-audit-code-quality.sh

# 4. 配置 Cron（Linux/macOS）
crontab -e
# 添加以下行：
# 0 3 * * * cd /path/to/dy05 && ./scripts/auto-audit-code-quality.sh
# 30 3 * * * cd /path/to/dy05 && ./scripts/auto-security-scan.sh
# 0 4 * * 0 cd /path/to/dy05 && ./scripts/auto-check-dependencies.sh
# 0 5 * * 0 cd /path/to/dy05 && ./scripts/auto-benchmark.sh
# 0 6 * * 0 cd /path/to/dy05 && ./scripts/auto-track-tech-debt.sh
```

## Windows 配置

使用 Windows 任务计划程序：

```powershell
# 创建每日 03:00 执行的任务
schtasks /create /tn "DY05-CodeQuality" /tr "C:\claude\dy05\scripts\auto-audit-code-quality.sh" /sc daily /st 03:00
```

## 报告输出

所有报告存储在 `reports/` 目录：

```
reports/
├── code-quality/
│   ├── 2026-05-13.json
│   └── 2026-05-13.md
├── dependencies/
│   ├── 2026-05-13.json
│   └── security-2026-05-13.json
├── coverage/
│   ├── backend-2026-05-13.html
│   └── frontend-2026-05-13.html
├── performance/
│   ├── backend-2026-05-13.json
│   └── frontend-2026-05-13.json
├── security/
│   └── 2026-05-13.json
└── tech-debt/
    ├── 2026-05-13.json
    └── trend.png
```

## 故障排查

### 脚本执行失败

```bash
# 检查日志
tail -f /var/log/dy05-automation.log

# 手动执行查看详细输出
bash -x ./auto-audit-code-quality.sh
```

### SonarQube 连接失败

```bash
# 检查 SonarQube 状态
docker compose -f docker/docker-compose.yml ps sonarqube

# 查看日志
docker compose -f docker/docker-compose.yml logs sonarqube
```

### GitHub Actions 失败

```bash
# 查看工作流运行状态
gh run list --workflow=daily-health-check

# 查看失败日志
gh run view <run-id> --log-failed
```

## 下一步

参考 `docs/AUTOMATION-DESIGN.md` 了解完整系统设计。
