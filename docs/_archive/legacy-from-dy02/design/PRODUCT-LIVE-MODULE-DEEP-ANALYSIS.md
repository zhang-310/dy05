# 商品模块与直播模块 — 深度分析与升级建议

> 分析日期：2026-03-04 | 更新：2026-03-04 P1/P2 升级已完成 | 对照文档：product 3.0、live 3.0、PRODUCT-VS-LIVE-PRODUCT-ANALYSIS

---

## 一、总体结论

| 维度 | 商品模块 | 直播模块 | 关联关系 |
|------|----------|----------|----------|
| **完成度** | ~85% | ~95% | 数据模型正确，销售数据闭环缺失 |
| **核心价值** | 商品主数据 + 销售历史 | AI 话术 + 场次管理 + 数据看板 | 直播选品依赖商品库 |
| **主要差距** | 销售数据与 live 未打通、文档与实现不一致 | 抖音 API 未对接、命名历史遗留 | 场次结束未自动写入 dy_product_sales_history |

---

## 二、商品模块深度分析

### 2.1 模块定位与职责

**设计文档：** 商品全生命周期管理：商品 CRUD → 上下架 → 库存 → 直播选品 → 销售记录 → 数据统计。

**实际实现：**
- ✅ 商品 CRUD、上下架、库存、推荐、多维度搜索
- ✅ 销售历史 CRUD、累计销售额/销售量
- ✅ 链接提取（ProductLinkExtractService）、排品表导入（PaipingImportService）
- ✅ 商品话术（DyProductScript、ProductScriptService）
- ⚠️ **销售数据与直播未打通**：设计文档 05 写明「直播场次结束后由 live 模块自动写入 dy_product_sales_history」，当前实现中 **live 模块未调用 SalesHistoryService**，销售数据需手动录入

### 2.2 数据模型

| 表 | 设计 | 实现 | 差距 |
|----|------|------|------|
| dy_product | 基础字段 | 已实现 + 扩展字段 | 扩展：profitMarginPct、lossPerUnit、controlStrategy、productLink、aiSellingPoints |
| dy_product_sales_history | 销售历史 | 已实现 | session_id 为 VARCHAR(128)，live 用 Long sessionId |
| dy_product_script | 未在设计文档 | 已实现 | 商品话术，供 live 引用 |

**dy_product 扩展字段（实现超前于文档）：**

| 字段 | 用途 | 建议 |
|------|------|------|
| profit_margin_pct | 利润百分比 | 文档 02 补充 |
| loss_per_unit | 每单亏损 | 文档 02 补充 |
| control_strategy | 控单策略（控3单、憋单等） | 文档 02 补充 |
| product_link | 商品链接，AI 提取 | 文档 02 补充 |
| ai_selling_points | AI 提炼卖点 | 文档 02 补充 |

### 2.3 与直播模块的关联

```
dy_product (user_id)
    │
    ├── live_product.product_id → 选品关联
    │       └── 校验：dyProduct.userId == session.userId
    │
    ├── dy_product_script → 产品话术，live 可引用
    │
    └── dy_product_sales_history ← ⚠️ 设计上应由 live 场次结束写入，当前未实现
```

**user_id vs account_id：**
- 商品：`dy_product.user_id`（所属用户）
- 直播场次：`live_session.user_id`（所属用户）、`live_session.account_id`（抖音账号）
- 选品校验：`dyProduct.userId.equals(session.userId)`，按用户隔离，未按 account_id 校验
- **结论**：多账号场景下，同一用户下多账号共享商品库，符合「用户级商品库」设计

### 2.4 商品模块升级建议

| 优先级 | 项 | 说明 |
|--------|-----|------|
| **P0** | 销售数据闭环 | live 场次结束（status→ended）时，自动将 live_product 的 sale_quantity、revenue 写入 dy_product_sales_history（channel_source=douyin_live, session_id=场次ID） |
| **P1** | 文档同步 | 02-数据库设计补充 dy_product 扩展字段、dy_product_script 表 |
| **P1** | session_id 类型统一 | dy_product_sales_history.session_id 设计为 VARCHAR，live 用 Long；建议统一为 Long 或 String，并在写入时一致 |
| **P2** | 销售历史 DataScope | SalesHistoryServiceImpl.search 未显式做 DataScope 过滤（依赖 productId 时需校验商品归属） |
| **P2** | 库存自动扣减 | 直播销售写入后，可选调用 update-inventory 扣减库存（需产品确认策略） |
| **P3** | 商品销售排行 API | 05-销售数据与分析 中的「商品销售排行」SQL 未暴露为 API |

---

## 三、直播模块深度分析

### 3.1 模块定位与职责

**设计文档：** AI 驱动的直播运营助手 — 场次管理、AI 话术、数据同步、AI 复盘。

**实际实现：**
- ✅ 场次 CRUD、状态机、开播准备、选品、话术生成、违规检测、效果归因、高效话术入库
- ✅ 数据看板、LiveMonitor 时序、AI 分析、历史对比、机构端
- ⚠️ 抖音 API 未对接，数据来源为手动录入或 LiveMonitor 聚合
- ⚠️ 场次结束后未自动写入 dy_product_sales_history

### 3.2 数据模型（live_product vs live_product_data）

| 表 | 用途 | 与 product 关系 |
|----|------|-----------------|
| live_product | 场次-商品关联，含 position、script_source、sale_quantity、revenue | product_id → dy_product.id |
| live_product_data | 分产品数据（gmv、orders、conversion_rate 等） | 同步自抖音或手动，与 live_product 独立 |

**数据流：**
- 选品：用户从 dy_product 选品 → 写入 live_product
- 直播中：live_product.sale_quantity、revenue 可手动/同步更新
- 直播后：live_product_data 存分产品详细数据；**dy_product_sales_history 应由 live 写入，当前缺失**

### 3.3 直播模块升级建议

| 优先级 | 项 | 说明 |
|--------|-----|------|
| **P0** | 销售数据回写 product | 场次 status→ended 时，遍历 live_product，调用 SalesHistoryService.save 写入 dy_product_sales_history |
| **P1** | 抖音 API 对接 | 直播数据同步对接抖音开放平台（需产品确认） |
| **P1** | 文档字段映射表 | 02-数据库设计 2.6 已补充，保持更新 |
| **P2** | live_product_data 与 live_product 同步 | 若抖音同步写入 live_product_data，可同时更新 live_product.revenue、sale_quantity，保持一致性 |
| **P3** | 菜单命名 | 「直播商品」→「直播选品」或「场次选品」，避免与「商品管理」混淆 |

---

## 四、两模块协同升级建议

### 4.1 销售数据闭环（P0，必做）

**现状：**
- live_product 有 sale_quantity、revenue
- live_product_data 有 gmv、orders 等
- dy_product_sales_history 设计上应收录每场直播的销售，但 live 未写入

**实现方案：**

1. **触发时机**：`LiveSession` 状态从 live(1) 变为 ended(2) 时
2. **写入逻辑**：
   ```
   遍历 live_product WHERE session_id = ?
   对每条记录：
     SalesHistorySaveVO vo = new SalesHistorySaveVO();
     vo.setProductId(lp.getProductId());
     vo.setSaleQuantity(lp.getSaleQuantity());
     vo.setSaleAmount(lp.getRevenue());
     vo.setSaleTime(session.getEndTime());
     vo.setChannelSource("douyin_live");
     vo.setSessionId(String.valueOf(session.getId()));  // 或 Long，需与 dy_product_sales_history 字段一致
     salesHistoryService.save(vo);
   ```
3. **幂等性**：同一 session 多次触发时，可先按 session_id 查询是否已有记录，有则跳过或更新
4. **数据来源优先级**：若 live_product_data 有数据，优先用 live_product_data；否则用 live_product

**代码位置建议：**
- `LiveSessionServiceImpl` 状态变更处，或
- `LiveDataSyncServiceImpl.saveSessionData` 在 session 已 ended 时触发，或
- 新增 `LiveSessionStatusListener` 监听 status 变更事件

### 4.2 session_id 类型统一

| 表 | 字段 | 当前类型 | 建议 |
|----|------|----------|------|
| dy_product_sales_history | session_id | VARCHAR(128) | 与 live_session.id 一致，建议 BIGINT，或保持 VARCHAR 存 String.valueOf(sessionId) |
| live_product | session_id | BIGINT | 保持 |

若 dy_product_sales_history.session_id 保持 VARCHAR，写入时 `vo.setSessionId(String.valueOf(session.getId()))` 即可。

### 4.3 选品流程优化

| 现状 | 建议 |
|------|------|
| 直播商品独立菜单 `/admin/live/product` 展示所有 live_product 扁平列表 | 默认按 session_id 筛选，或改为场次详情内「选品」Tab |
| 选品从商品库添加 | 保持，确保 product 模块的 featured 优先排序在选品列表中生效 |

### 4.4 文档同步

| 文档 | 修改 |
|------|------|
| live 01-需求分析 | 「产品数据来自 douyin 模块」→「产品数据来自 product 模块（dy_product）」 |
| product 02-数据库设计 | 补充 dy_product 扩展字段、dy_product_script |
| product 05-销售数据与分析 | 补充「live 场次结束自动写入」的实现说明（待 P0 完成后） |

---

## 五、升级任务清单（按优先级）

### P0 — 销售数据闭环（必做）

| # | 任务 | 负责模块 | 说明 |
|---|------|----------|------|
| 1 | 场次结束自动写入 dy_product_sales_history | live | 在 status→ended 时调用 SalesHistoryService |
| 2 | 幂等与数据来源 | live | 同一 session 不重复写入；优先 live_product_data，否则 live_product |
| 3 | session_id 写入格式 | product + live | 确认 dy_product_sales_history.session_id 存 Long 还是 String，统一写入 |

### P1 — 文档与一致性 ✅ 已完成

| # | 任务 | 说明 |
|---|------|------|
| 4 | product 02 补充扩展字段 | ✅ profit_margin_pct、loss_per_unit、control_strategy、product_link、ai_selling_points |
| 5 | product 02 补充 dy_product_script | ✅ 表结构、与 live 引用关系 |
| 6 | live 01 修正产品来源描述 | ✅ douyin → product |
| 7 | SalesHistoryServiceImpl DataScope | ✅ search/getById/save/totalSalesAmount/totalSalesQuantity 均增加 DataScope 校验 |

### P2 — 增强与优化 ✅ 已完成

| # | 任务 | 说明 |
|----|------|------|
| 8 | 抖音 API 对接 | ✅ DouyinLiveDataSyncService + POST /session/sync-from-douyin，详见 docs/devops/DOUYIN-LIVE-API-SETUP.md |
| 9 | 库存自动扣减 | ✅ product.sales.auto-deduct-inventory（默认 false） |
| 10 | 商品销售排行 API | 待实现 |
| 11 | 菜单命名 | ✅ 直播商品 → 直播选品（sql/auth/resource-tree.sql、AdminLayout 已为「直播选品」） |

### P3 — 可选

| # | 任务 | 说明 |
|----|------|------|
| 12 | live_product_data 与 live_product 同步 | 抖音同步时双写 |
| 13 | RESTful 路径统一 | 工作量大，收益有限 |

---

## 六、总结

| 维度 | 商品模块 | 直播模块 | 协同 |
|------|----------|----------|------|
| **核心能力** | 商品主数据完整，销售历史独立 | AI 话术闭环完整，数据看板齐全 | 选品关联正确 |
| **最大差距** | 销售数据未与 live 打通 | 场次结束未回写 product 销售历史 | **P0：销售数据闭环** |
| **文档** | 扩展字段、dy_product_script 未文档化 | 字段映射已补，产品来源需修正 | 两处文档同步 |
| **建议节奏** | P0 与 live 协同完成 → P1 文档 → P2 增强 | P0 实现回写 → P1 抖音 API（待确认） | 优先完成 P0 |

**结论：** 两模块数据模型与职责划分清晰，核心差距在 **销售数据闭环**。完成 P0 后，直播带货场景下的「选品 → 直播 → 销售记录 → 累计统计」全链路可打通。
