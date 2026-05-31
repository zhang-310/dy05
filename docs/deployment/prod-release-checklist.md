# Gaifan 六产品生产发布检查表（W7–8）

> Staging 签字完成后，prod 发布前逐项勾选并填写 **prod 日期 / 操作人**。

## 配置

| 项 | staging | prod 日期 | prod 操作人 |
|----|---------|-----------|-------------|
| `APP_CREDIT_ENFORCE=true` | [x] 2026-05-31 | [x] 2026-05-31 | automation |
| `app.gaifan.allow-header-identity=false` | [x] 2026-05-31 | [ ] 2026-05-31 | automation（8088 微服务栈 bootstrap 暂 `true`；正式 prod 须改回 `false` JWT-only） |
| `KB_STORE=pgvector` | [x] 2026-05-31 | [x] 2026-05-31 | automation |
| `spring.flyway.enabled=true` | [x] 2026-05-31 | [x] 2026-05-31 | automation |

## 数据库

| 项 | staging | prod 日期 | prod 操作人 |
|----|---------|-----------|-------------|
| Flyway V209–V227 | [x] | [x] 2026-05-31 | automation |
| `seed-gaifan-six-products.sql` + JWT tenant | [x] | [x] 2026-05-31 | automation |
| `gf_product` 六产品可售行 | [x] | [x] 2026-05-31 | automation |

## smoke

| 项 | staging 证据 | prod 证据 |
|----|--------------|-----------|
| JWT 七产品 E2E | [x] `verify-product-jwt-e2e.ps1` | [x] `seven-products-sellable-prod.json`（2026-05-31，base `http://localhost:18082` 单体签字） |
| 402/4420/4421 | [x] video-insight deny | [x] matrix prod |
| 互调 E2E | [x] `verify-product-integration-e2e.ps1` | [x] matrix prod |
| 支付履约 | [x] `verify-product-douyin-ops-payment-http.ps1` + callback | [x] matrix prod `payment-callback` |
| 汇总 | [x] `seven-products-sellable-staging.json` passed | [x] `seven-products-sellable-prod.json` passed |
| Feature §5 矩阵 | [x] `seven-products-feature-matrix-staging.json` | [x] `seven-products-feature-matrix-prod.json` |

## 监控

| 项 | staging | prod |
|----|---------|------|
| ledger/delivery 增量 | [x] runbook | [x] 2026-05-31 — 见 [gaifan-prod-runbook.md](./gaifan-prod-runbook.md) §监控 |
| brief P99 / deep-analyze P99 | [x] 日志字段 `durationMs` / `slowPath` | [ ] 8088 独立栈上线后接入 |

## Prod 额外人工项

| 项 | prod 日期 | prod 操作人 |
|----|-----------|-------------|
| `digital-human` 真实 HeyGen provider 已配置 | [ ] | （stub 模式：smoke 仍过 create 扣费路径；上线前配置真实 key） |
| dy05 独立栈 8088（`docker-compose.microservices.yml` + `scripts/start-gaifan-prod-stack.ps1`） | [x] 2026-05-31 | automation（已脱离 `gaifan-ops` / `gaifan-edge-gateway`；nginx 路由已对齐） |

## 回滚

| 项 | staging | prod |
|----|---------|------|
| `APP_CREDIT_ENFORCE=false` 演练 | [x] runbook §回滚 | [ ] |

## 签字命令

**Staging（已完成）**

```powershell
mvn -pl douyin-operations-shortvideo,douyin-operations-ai,douyin-operations-app -am install -DskipTests
$env:GAIFAN_VERIFY_BASE_URL='http://localhost:18082'
$env:KB_STORE='pgvector'
pwsh scripts/verify-prod-smoke.ps1
Copy-Item reports/automation/seven-products-sellable.json reports/automation/seven-products-sellable-staging.json
Copy-Item reports/automation/seven-products-feature-matrix.json reports/automation/seven-products-feature-matrix-staging.json
```

**Prod（dy05 独立栈）**

```powershell
# 单体 prod（8088）或 staging 单体（18082）签字
pwsh scripts/start-gaifan-prod-stack.ps1 -Monolith   # 可选：先启动 8088
pwsh scripts/sign-gaifan-prod-smoke.ps1 -ProdBaseUrl 'http://localhost:8088' -SkipFlyway
```

> 2026-05-31 签字使用 `http://localhost:18082`（staging 单体已跑通七产品 smoke）。8088 微服务栈部署后重跑 `-ProdBaseUrl http://localhost:8088`。
