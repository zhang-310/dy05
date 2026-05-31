# Gaifan 工作区分批 PR 冻结方案

> **状态**：Phase 0 SSOT | **基线**：1894 未提交变更（2026-05-31）  
> **原则**：按依赖顺序合并；每 PR 附 verify 输出；**不 squash 跨模块**。

---

## 合并顺序

| PR | 分支建议 | 范围 | 必跑命令 |
|----|----------|------|----------|
| **PR-1** | `feat/freeze-ai-foundation` | `douyin-operations-ai/`、`douyin-operations-intelligence/pom.xml`、Flyway V209–V227 | `mvn -pl douyin-operations-ai,douyin-operations-app -am compile` + `pwsh scripts/run-ai-foundation-verify.ps1` |
| **PR-2** | `feat/freeze-platform-commercial` | `douyin-operations-platform/`（ProductIntegration、CommercialCredit、Payment） | `mvn -pl douyin-operations-platform -am test` |
| **PR-3** | `feat/freeze-six-products-backend` | shortvideo / drama / digital-human / photo-avatar / douyin 模块 | `mvn -pl douyin-operations-shortvideo,douyin-operations-drama,douyin-operations-digital-human,douyin-operations-photo-avatar,douyin-operations-douyin -am test -DskipTests=false`（关键单测） |
| **PR-4** | `feat/freeze-front-commercial` | `front/src/` 横切 + 六产品 UX | `cd front && npm run type-check` |
| **PR-5** | `feat/freeze-scripts-evidence` | `scripts/verify-*.ps1`、`reports/automation/`、`docs/deployment/` | `pwsh scripts/run-six-products-ci.ps1 -SkipHttpVerify` |

**合并后全量签字**（staging 18082 运行中）：

```powershell
mvn -pl douyin-operations-shortvideo,douyin-operations-ai,douyin-operations-app -am install -DskipTests
$env:GAIFAN_VERIFY_BASE_URL='http://localhost:18082'
$env:KB_STORE='pgvector'
pwsh scripts/start-gaifan-staging.ps1 -SkipMigrations
pwsh scripts/verify-prod-smoke.ps1
```

---

## PR-1：AI 基座

**包含**

- `douyin-operations-ai/` 全量
- `douyin-operations-intelligence/pom.xml` 兼容壳
- `douyin-operations-app/.../db/migration/V209*`–`V227*`
- `application-prod.yml` enforce / JWT-only / pgvector

**排除**：六产品业务逻辑、front/

---

## PR-2：平台商业化

**包含**

- `ProductIntegrationService`、`CommercialCreditHelper`
- `PaymentCreditGrantAdapter`、`PlatformEntitlementService`
- `ProductIntegrationServiceTest`

**门禁**：`mvn -pl douyin-operations-platform test -Dtest=ProductIntegrationServiceTest,PaymentCreditGrantAdapterPgTest`

---

## PR-3：六产品后端

**包含**

- `douyin-operations-shortvideo`（export、workflow、benchmark、viral）
- `douyin-operations-drama`（export-to-maker）
- `douyin-operations-digital-human`（webhook）
- `douyin-operations-photo-avatar`
- `douyin-operations-douyin`

**门禁**：各 `verify-product-*.ps1` 单产品（需 staging）

---

## PR-4：前端横切

**包含**

- `useFilteredRoleNav.tsx`、`commercialError.ts`、`request.ts`
- Gaifan 页：McpGateway、CreditGovernance、OfficialWebsite、PaymentCenter
- 六产品页 402 / entitlement 预检

**门禁**：`npm run type-check`

---

## PR-5：脚本 + 证据 + 文档

**包含**

- `scripts/verify-seven-products-sellable.ps1` 及 lib
- `scripts/verify-prod-smoke.ps1`、`verify-product-integration-e2e.ps1`
- `reports/automation/seven-products-sellable.json`（可选归档）
- `docs/deployment/prod-release-checklist.md`、`gaifan-prod-runbook.md`、本文件

**门禁**：`pwsh scripts/run-six-products-ci.ps1 -FullSellable -BaseUrl $env:GAIFAN_VERIFY_BASE_URL`

---

## CI 集成

| 场景 | 命令 |
|------|------|
| PR gate（无 staging） | `pwsh scripts/run-six-products-ci.ps1 -SkipHttpVerify` |
| Nightly / staging | `pwsh scripts/run-six-products-ci.ps1 -FullSellable -BaseUrl http://staging:18082` |
| Prod 签字 | `pwsh scripts/verify-prod-smoke.ps1` |

环境变量：

- `GAIFAN_VERIFY_BASE_URL` — HTTP 验收目标
- `GAIFAN_JWT_E2E=true` — 含 JWT 七产品 E2E
- `KB_STORE=pgvector` — KB 验收

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-05-31 | 初版：5 PR 分批 + 验收命令 |
