# GMV 计算口径规范

**版本**：v1.0  
**创建**：2026-03-26  
**关联代码**：`GmvCalculationService.java`、`frontend-react/src/utils/format.ts`

---

## 一、GMV 口径定义

| 场景 | 字段/方法 | 数据来源 | 更新时机 | 前端展示 |
|------|---------|---------|---------|---------|
| **单场次累计 GMV** | `GmvCalculationService.sessionGmv(sessionId)` | `SUM(live_product.revenue)` WHERE sessionId + deleted=0 | 抖音数据同步后（场次结束时） | `LiveSessionPage` 列表、`LiveSessionDetailPage` 数据 Tab |
| **实时面板成交金额** | `live_session_realtime_data.productPurchaseAmount` | 抖音 collector 轮询或弹幕 ingest 写入 | 实时 SSE 推送 | `LiveRealtimePanelPage` 顶部统计卡 |
| **Dashboard 今日 GMV** | `GmvCalculationService.todayGmv(orgId)` | `SUM(live_product.revenue)` WHERE createTime >= 今日零时 | 定时刷新或按需计算 | `DashboardPage` KPI 卡片 |
| **KPI 区间 GMV** | `GmvCalculationService.gmvBetween(orgId, start, end)` | 同上，按时间范围 | 按需计算 | `UnifiedKpiPage` 区间图表 |

---

## 二、字段对照表

| 展示位置 | 后端字段 | 前端字段 | 说明 |
|---------|---------|---------|------|
| 场次列表/详情 | `LiveSessionVO.cumulativeGmv` | `session.cumulativeGmv` | 由 `GmvCalculationService.sessionGmv()` 填充 |
| 实时面板 SSE | `LiveSessionRealtimeDataVO.productPurchaseAmount` | `realtimeData.productPurchaseAmount` | 实时数据，与累计 GMV 口径不同 |
| 实时面板礼物 | `LiveSessionRealtimeDataVO.giftAmount` | `realtimeData.giftAmount` | **不计入 GMV**，仅单独展示 |
| Dashboard | `BusinessDashboardVO.todayGmv` | `dashboard.todayGmv` | 今日自然日汇总 |
| KPI 表格 | 按需聚合 | `kpi.gmvAmount` | 区间汇总 |

---

## 三、前端统一格式化

**必须使用 `formatGmv()` 函数**（`src/utils/format.ts`）：

```typescript
import { formatGmv, GMV_SOURCE_TOOLTIP } from '@/utils/format'

// 基础使用（紧凑格式，默认）
formatGmv(12345.67)    // → '¥1.23万'
formatGmv(1000000)     // → '¥100.00万'
formatGmv(100000000)   // → '¥1.00亿'

// 带 Tooltip 说明来源
<Tooltip title={GMV_SOURCE_TOOLTIP.SESSION}>
  <span>{formatGmv(session.cumulativeGmv)}</span>
</Tooltip>

// 非紧凑格式（用于详情页精确显示）
formatGmv(12345.67, { compact: false })  // → '¥12,345.67'
```

---

## 四、禁止事项

- ❌ 不要直接使用 `formatMoney()` 展示 GMV（缺少万/亿单位转换）
- ❌ 不要在不同页面用不同字段展示"GMV"（如有时用 `revenue` 有时用 `purchaseAmount`）
- ❌ 不要将礼物收入（`giftAmount`）混入 GMV 计算
- ❌ 不要在实时面板中将 `productPurchaseAmount` 标注为"累计 GMV"（两者有时间差）

---

## 五、数据差异说明（运营告知）

`productPurchaseAmount`（实时）与 `cumulativeGmv`（历史）可能存在差异，原因：

1. **时间差**：实时数据为直播间即时成交，`revenue` 为抖音后台结算数据
2. **退款**：`revenue` 可能包含退款扣减，实时数据不含退款
3. **同步延迟**：场次结束后抖音数据同步有 5-30 分钟延迟

**运营指导**：
- 播中决策参考：使用实时面板的 `productPurchaseAmount`
- 场次复盘参考：使用 `cumulativeGmv`（更准确）
- KPI 考核参考：使用 `GmvCalculationService` 的区间计算结果

---

## 六、相关代码位置

| 类型 | 位置 |
|------|------|
| 后端接口 | `live/service/GmvCalculationService.java` |
| 后端实现 | `live/service/impl/GmvCalculationServiceImpl.java` |
| 数据库查询 | `live/repository/LiveProductRepository.java`（`sumRevenue*` 系列方法） |
| 前端格式化 | `frontend-react/src/utils/format.ts`（`formatGmv`、`GMV_SOURCE_TOOLTIP`） |
| 实时数据 VO | `live/vo/LiveSessionRealtimeDataVO.java` |
| 历史数据 VO | `live/vo/LiveSessionVO.java`（`cumulativeGmv` 字段） |
