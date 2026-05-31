# 六产品：M4 已完成 vs 全量待办 Checklist

> **状态**：Post-M4 SSOT | **基线**：M4 验收 2026-05-31  
> **M4 证据**：[`reports/automation/six-products-m4.json`](../../reports/automation/six-products-m4.json)（6/6 `passed`）  
> **M4 脚本**：[`scripts/verify-six-products-m4.ps1`](../../scripts/verify-six-products-m4.ps1)

---

## §0 M4 签收表

| 产品 | M4 主 API | verify 脚本 | JSON status |
|------|-----------|---------------|-------------|
| douyin-ops | `POST /api/v1/ai/douyin-ops-commander/brief` | `verify-product-douyin-ops.ps1` | passed |
| video-insight | `POST /api/v1/short-video/viral/deep-analyze` | `verify-product-video-insight.ps1` | passed |
| shortvideo-maker | `POST /api/v1/short-video/project/export-script` | `verify-product-shortvideo-maker.ps1` | passed |
| digital-human | `POST /api/v1/digital-human/create` | `verify-product-digital-human.ps1` | passed |
| photo-avatar-video | `POST /api/v1/photo-avatar/create` | `verify-product-photo-avatar-video.ps1` | passed |
| drama-ai | `POST /api/v1/drama/create` | `verify-product-drama-ai.ps1` | passed |

**M4 不包含**：全量 `douyin-operations-app verify`、每产品全部写 API 扣费、前端 JWT 端到端、prod 发布。

---

## §1 横切（功能 / 前端 / 上线）

| ID | 维度 | M4 | 全量待办 | 波次 |
|----|------|-----|----------|------|
| X-F1 | 功能 | 6 主 API 扣费 | Feature 全表 → 各 Controller（见 [`dy05-feature-mapping.md`](dy05-feature-mapping.md) §5） | W0 |
| X-F2 | 功能 | 积分不足 → 4420 + HTTP 402 | 无权益 → `ENTITLEMENT_DENIED`(4421) + HTTP 402 | W0 完成 |
| X-F3 | 功能 | V216/V224 规则 | `gf_product_integration_invocation` 脚本/IT 断言 | W3/W6 |
| X-F4 | 功能 | — | 第 7 产品 `knowledge-base` 全量扣费（可选） | W7 |
| X-FE1 | 前端 | 大量 shortvideo/douyin 页 | `commercialError.ts` + `request` 4420/4421 | W0 完成 |
| X-FE2 | 前端 | — | 按 `productCode` 导航/套餐显隐 | W1–W6 |
| X-FE3 | 前端 | 官网列表 | 可购/支付跳转 | W1/W7 |
| X-OPS1 | 上线 | staging 18082 + seed | dev/staging enforce 策略文档化 | W0 |
| X-OPS2 | 上线 | 手工 `apply-gaifan-commercial-migrations` | `apply-gaifan-product-flyway.ps1` + prod Flyway SSOT | W7 |
| X-OPS3 | 上线 | 本地 verify | `run-six-products-ci.ps1` + `six-products-nightly.yml` + PR gate | W0 完成 |
| X-OPS4 | 上线 | Header E2E | prod 仅 JWT，无 `X-User-Id` | W7 |

---

## §2 P1 `douyin-ops`

| 维度 | M4 已完成 | 全量待办 | 状态 |
|------|-----------|----------|------|
| 功能 | commander brief + 人设/账号扣费；支付 douyin-ops delivery | OAuth/视频分析全映射；官网可购 E2E | 基本完成 |
| 前端 | `/talent/douyin/*` + 指挥官页 | 账号 402 UX；侧栏 productCode | 已加 |
| 上线 | verify passed | brief 慢路径监控 | — |

---

## §3 P2 `video-insight`

| 维度 | M4 已完成 | 全量待办 | 状态 |
|------|-----------|----------|------|
| 功能 | deep-analyze 扣费 + delivery | `/viral/analyze`、benchmark、MCP 与 `VIDEO_*` 对齐；verify 断言 402 | verify 已支持 4421 |
| 前端 | 洞见/爆款/深度拆解 UI | 权益不足提示 + 拆解前 entitlement 预检 | 已加 |
| 上线 | verify passed | 采集合规清单 | — |

---

## §4 P3 `shortvideo-maker`

| 维度 | M4 已完成 | 全量待办 | 状态 |
|------|-----------|----------|------|
| 功能 | export reserve/commit + SvScript charge | verify delivery>0；workflow reserve；动态 project | 已完成 |
| 前端 | 全链路 shortvideo 页 | 导出前积分预检 UI | 已完成 |
| 上线 | verify passed | WorkflowCommercialGuard | 已完成 |

---

## §5 P4 `digital-human`

| 维度 | M4 已完成 | 全量待办 | 状态 |
|------|-----------|----------|------|
| 功能 | `/digital-human/create` 扣费 | Webhook 写 delivery 台账 | 已加 |
| 前端 | admin HeyGen 页 | 产品轨说明 | 已加 |
| 上线 | domain seed 表 | Flyway V209+ | W7 脚本 |

---

## §6 P5 `photo-avatar-video`

| 维度 | M4 已完成 | 全量待办 | 状态 |
|------|-----------|----------|------|
| 功能 | create 扣费 + delivery | `portraitConsentConfirmed` 必填；insightReportId 可选 | 已实现 |
| 前端 | PhotoAvatarPage | 授权勾选 + 402 UX | 已加 |
| 上线 | verify passed | Flyway V211+ | W7 |

---

## §7 P6 `drama-ai`

| 维度 | M4 已完成 | 全量待办 | 状态 |
|------|-----------|----------|------|
| 功能 | create + script 扣费 | drama→maker 互调 E2E + invocation/delivery 断言 | 已完成 |
| 前端 | DramaPage | 产品码/扣费提示、导出 maker | 已完成 |
| 上线 | verify passed | export 互调 trace + DDL 对齐 | 已完成 |

---

## §8 波次与验收（全量轮完成定义）

| 波次 | 范围 | 完成标准 |
|------|------|----------|
| W0 | 横切 | Feature §5、4421+402、前端拦截、CI 脚本/工作流 |
| W1–2 | douyin-ops | §2 功能+前端核心项 + `verify-product-douyin-ops-payment.ps1` |
| W2–3 | video-insight | §3 + 402 verify |
| W3–4 | shortvideo-maker | §4 delivery 断言 |
| W4–5 | digital-human | §5 API 轨 |
| W5–6 | photo-avatar | §6 合规 |
| W6–7 | drama-ai | §7 script 扣费 |
| W7–8 | 收口 | [`prod-release-checklist.md`](../deployment/prod-release-checklist.md) + KB 可选 |

**全量签字**：`verify-product-feature-matrix.ps1` passed + `verify-prod-smoke.ps1` exit 0 + JWT 七产品 E2E（KB 走 RAG search）。

---

## §9 修订记录

| 日期 | 说明 |
|------|------|
| 2026-05-31 | 全面升级收口：互调 E2E、payment PG、workflow reserve、动态 project、prod checklist 分 staging/prod |
