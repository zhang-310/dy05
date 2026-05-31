# Gaifan 六产品生产发布检查表（W7–8）

> Staging 签字完成后，prod 发布前逐项勾选并填写 **prod 日期 / 操作人**。

## 配置

| 项 | staging | prod 日期 | prod 操作人 |
|----|---------|-----------|-------------|
| `APP_CREDIT_ENFORCE=true` | [x] 2026-05-31 | [ ] | |
| `app.gaifan.allow-header-identity=false` | [x] 2026-05-31 | [ ] | |
| `KB_STORE=pgvector` | [x] 2026-05-31 | [ ] | |
| `spring.flyway.enabled=true` | [x] 2026-05-31 | [ ] | |

## 数据库

| 项 | staging | prod 日期 | prod 操作人 |
|----|---------|-----------|-------------|
| Flyway V209–V227 | [x] | [ ] | |
| `seed-gaifan-six-products.sql` + JWT tenant | [x] | [ ] | |
| `gf_product` 六产品可售行 | [x] | [ ] | |

## smoke

| 项 | staging 证据 | prod 证据 |
|----|--------------|-----------|
| JWT 七产品 E2E | [x] `verify-product-jwt-e2e.ps1` | [ ] `seven-products-sellable-prod.json` |
| 402/4420/4421 | [x] video-insight deny | [ ] |
| 互调 E2E | [x] `verify-product-integration-e2e.ps1` | [ ] |
| 支付履约 | [x] `verify-product-douyin-ops-payment-http.ps1` + `PaymentCreditGrantAdapter*Test` | [ ] |
| 汇总 | [x] `seven-products-sellable.json` passed | [ ] |
| Feature §5 矩阵 | [x] `seven-products-feature-matrix-staging.json` | [ ] `seven-products-feature-matrix-prod.json` |

## 监控

| 项 | staging | prod |
|----|---------|------|
| ledger/delivery 增量 | [x] runbook | [ ] |
| brief P99 / deep-analyze P99 | [x] 日志字段 `durationMs` / `slowPath` | [ ] |

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

**Prod（待 prod 部署后执行）**

```powershell
pwsh scripts/sign-gaifan-prod-smoke.ps1 -ProdBaseUrl 'https://your-prod-host:8088'
```
