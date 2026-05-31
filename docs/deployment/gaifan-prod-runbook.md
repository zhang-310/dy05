# Gaifan 生产运维 Runbook（W7–8）

> **SSOT**：prod 入口为 dy05 自有栈（`docker-compose.microservices.yml` + `scripts/start-gaifan-prod-stack.ps1`），**不再依赖** `D:\gaifan\gaifan-ops` / `gaifan-edge-gateway`。

## 独立 prod 栈启动

```powershell
# 微服务（nginx 8088 → platform / ai-mcp / content）
pwsh scripts/start-gaifan-prod-stack.ps1

# 或单体 prod（8088，适合 smoke / 小规模）
pwsh scripts/start-gaifan-prod-stack.ps1 -Monolith -SkipFlyway
```

签字：

```powershell
pwsh scripts/sign-gaifan-prod-smoke.ps1 -ProdBaseUrl 'http://localhost:8088' -SkipFlyway
```

网关路由见 `docker/microservices/nginx.conf`（`/api/` 兜底、legacy 路径、chat-stream、`/actuator/`）。

## 监控指标

| 指标 | 来源 | 告警建议 |
|------|------|----------|
| `gf_credit_ledger` 增量 | PostgreSQL | 1h 无写入且 enforce=true 时排查 |
| 各 `gf_*_delivery_workspace_ledger` | PostgreSQL | 产品主 API 成功率 < 95% |
| 指挥官 brief P99 | 日志 `douyin_ops_commander.brief durationMs` | `slowPath=true` 或 P99 > 120s |
| deep-analyze P99 | APM / 日志 | > 180s |
| MCP invoke 402 率 | `gf_mcp_invocation` | 突增 > 基线 3x |
| 工作流 reserve | `gf_credit_reservation` business_key `workflow-*` | RESERVED > 30min 未 commit |

## CI 门禁

| 场景 | 命令 |
|------|------|
| PR（无 staging） | `pwsh scripts/run-six-products-ci.ps1 -SkipHttpVerify` |
| Staging 全量 | `pwsh scripts/run-six-products-ci.ps1 -FullSellable -BaseUrl http://localhost:18082` |
| Prod 签字 | `pwsh scripts/verify-prod-smoke.ps1` |

## 回滚

1. 设置 `APP_CREDIT_ENFORCE=false` 并滚动重启（保留台账写入）
2. 若 KB 异常：`KB_STORE=milvus` 临时切回（需重启）
3. 验证：`pwsh scripts/run-gaifan-staging-regression.ps1`

## 发布前签字

```powershell
pwsh scripts/start-gaifan-prod-stack.ps1   # 或 -Monolith
pwsh scripts/sign-gaifan-prod-smoke.ps1 -ProdBaseUrl 'http://localhost:8088'
```

证据：`reports/automation/seven-products-sellable-prod.json`、`seven-products-feature-matrix-prod.json`
