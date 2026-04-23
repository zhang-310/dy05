# 直播模块（live）— 深度评估报告

> 评估日期：2026-02-26 | 评估维度：架构师 + 产品经理 + 抖音运营专家

---

## 一、模块概况

直播模块覆盖直播全流程：场次创建 → 商品选品 → AI 话术生成 → 合规检测 → 数据同步 → AI 分析复盘。

- 5 个数据库表（设计文档）+ 4 个已实现实体
- 4 个 Service 实现
- 4 个 Controller（19 个已实现接口）
- 21 个计划接口（含未实现的 AI 话术生成和数据分析）
- 3 个前端页面

---

## 二、三维评估

### 2.1 架构师视角

#### 优点
- 设计文档中的 5 表结构（session/product/script/data/product_data）覆盖了直播核心数据模型
- LiveMonitor 时序数据设计合理，支持直播过程中的实时监控
- LiveScript 有执行追踪（executed + actualExecutionTime），可以做话术效果归因
- 前端组件化设计（SearchBar/Table/Dialog 拆分）

#### 问题清单

| # | 级别 | 问题 | 影响 |
|---|------|------|------|
| S1 | S | **设计文档和实现严重不一致** — 设计文档定义了 5 张表（live_session/live_session_product/live_session_script/live_session_data/live_product_data），但实现用了不同的实体名（LiveSession/LiveProduct/LiveScript/LiveMonitor），字段也大量不同 | 开发时无法以文档为准，容易出错 |
| S2 | S | **AI 话术生成完全未实现** — 设计文档定义了 6 个话术生成接口（generate-opening/product/transition/closing/full/check-violation），全部未实现 | 模块核心 AI 能力缺失 |
| S3 | S | **直播数据同步未实现** — 设计文档定义了 live_session_data 和 live_product_data 表，但实现中只有 LiveMonitor（手动录入），没有从抖音 API 自动同步 | 直播数据分析无数据来源 |
| S4 | S | **AI 分析报告未实现** — 设计文档定义了 analysis/generate 和 analysis/get 接口，未实现 | 直播复盘能力缺失 |
| A1 | A | **LiveSession 实体字段与设计文档不匹配** — 实现中有 liveTitle/liveDescription/liveUrl/viewers/likes/recordingUrl/recordingDuration，设计文档中有 session_title/session_cover/planned_start_time/planned_end_time/persona_id/script_style。关键字段 persona_id 缺失 | 无法关联人设，AI 话术生成无法工作 |
| A2 | A | **LiveProduct 实体过于简化** — 实现中只有 sessionId/productId/productName/saleQuantity/revenue/position，设计文档中还有 script_source/product_script_id/session_script 等话术关联字段 | 商品和话术之间的关联断裂 |
| A3 | A | **LiveScript 实体缺少关键字段** — 实现中只有 scriptContent/sequenceNo/executionTime/executed，设计文档中还有 script_type（opening/transition/closing/full）和 style 字段 | 无法区分话术类型，AI 生成无法按类型生成 |
| A4 | A | **LiveMonitor 替代了 live_session_data** — 设计文档中 live_session_data 是直播结束后的汇总数据（total_gmv/total_orders/conversion_rate），LiveMonitor 是过程中的时序数据。实现中只有 LiveMonitor，缺少汇总表 | 直播结束后的汇总数据无处存储 |
| A5 | A | **缺少合规检测集成** — 设计文档 BR-06 要求话术生成后自动调用 script 模块做违规词检测，未实现 | 话术可能包含违规词，有封号风险 |
| A6 | A | **前端只有 3 个页面** — 缺少直播数据看板、AI 话术生成页面、直播复盘页面 | 核心功能无前端入口 |
| A7 | A | **LiveSession 状态机不完整** — 实现中 status 是 Integer（0/1/2/3），设计文档中是 preparing/live/ended。缺少状态转换校验（比如不能从 ended 回到 live） | 状态可能被错误修改 |
| B1 | B | **LiveProduct 缺少 live_product_data 对应** — 设计文档中有分产品数据表（gmv/orders/click_count/conversion_rate/refund_rate），实现中 LiveProduct 只有 saleQuantity 和 revenue | 无法分析单品表现 |
| B2 | B | **LiveScript 缺少与 ai_call_log 的关联** — 话术是 AI 生成的，但没有记录是哪次 AI 调用生成的 | 无法追踪话术生成质量 |
| B3 | B | **LiveMonitor 缺少数据采集频率控制** — 没有定义采集间隔，可能导致数据量过大或过小 | 时序数据粒度不可控 |
| B4 | B | **前端 LiveProductManagement 的 position 限制为 1-5** — 实际直播可能有几十个商品 | 商品数量受限 |
| B5 | B | **缺少直播间实时预览** — 没有 WebSocket 连接展示实时数据 | 管理员无法实时监控直播状态 |

---

### 2.2 产品经理视角

#### 核心用户旅程缺口

```
当前用户旅程（严重断裂）：

主播准备开一场直播
    ↓
① 创建直播场次 → LiveSession（✅ 基本可用，但缺少 persona_id）
    ↓
② 选择商品 → LiveProduct（✅ 基本可用，但缺少话术关联）
    ↓
③ AI 生成话术 → ❌ 完全未实现（6 个接口全部缺失）
    ↓
④ 合规检测 → ❌ 未实现
    ↓
⑤ 开始直播 → LiveSession.status 更新（✅ 可用）
    ↓
⑥ 实时数据监控 → LiveMonitor（⚠️ 有表但无自动采集，需手动录入）
    ↓
⑦ 话术执行追踪 → LiveScript.executed（✅ 可用）
    ↓
⑧ 直播结束 → 数据同步 → ❌ 未实现
    ↓
⑨ AI 复盘报告 → ❌ 未实现
    ↓
⑩ 效果归因 → ❌ 无法归因到具体话术
```

**步骤 ③④⑥⑧⑨⑩ 全部缺失或未实现，直播模块目前只是一个「场次记录工具」，不是「直播运营助手」。**

#### 产品级问题

| # | 问题 | 建议 |
|---|------|------|
| P1 | **核心价值未交付** — AI 话术生成是直播模块的核心卖点，完全未实现 | 最高优先级实现话术生成 6 个接口 |
| P2 | **没有直播准备清单** | 开播前自动检查：商品已选？话术已生成？合规已通过？ |
| P3 | **没有直播中实时助手** | 直播过程中根据实时数据给出话术调整建议（对接 ai 模块 05-直播分析） |
| P4 | **没有直播复盘** | 直播结束后自动生成复盘报告，对比历史数据 |
| P5 | **没有话术效果排行** | 基于 LiveScript.executed + LiveMonitor 时序数据，分析哪句话术效果最好 |
| P6 | **没有商品排期优化** | 基于历史数据分析最佳商品上播顺序 |

---

### 2.3 抖音运营专家视角

#### 运营场景覆盖度

| 场景 | 覆盖度 | 说明 |
|------|--------|------|
| 直播场次管理 | ✅ 70% | 基本 CRUD 可用，缺少人设关联和准备清单 |
| 商品选品 | ⚠️ 40% | 有关联但缺少话术绑定和排期优化 |
| AI 话术生成 | ❌ 0% | 6 个接口全部未实现 |
| 合规检测 | ❌ 0% | 未对接 script 模块 |
| 实时数据监控 | ⚠️ 20% | 有 LiveMonitor 表但无自动采集 |
| 话术执行追踪 | ⚠️ 50% | 有 executed 字段但无效果分析 |
| 数据同步 | ❌ 0% | 未对接抖音 API |
| AI 复盘 | ❌ 0% | 未实现 |
| 话术效果归因 | ❌ 0% | 无法关联话术和数据变化 |
| 历史对比 | ❌ 0% | 无汇总数据表 |

#### 运营专家核心建议

1. **话术是直播的灵魂** — 没有 AI 话术生成，直播模块就是一个空壳。这是最高优先级。
2. **实时数据决定成败** — 直播是实时的，数据延迟 1 分钟就可能错过调整窗口。必须实现 WebSocket 实时推送。
3. **话术效果归因是核心竞争力** — LiveScript 已经有 executed + actualExecutionTime，LiveMonitor 有时序数据，两者关联就能精确分析「哪句话术让观众增长了」。这个能力市面上几乎没有竞品做到。
4. **商品上播顺序影响 GMV** — 经验丰富的运营都知道，第一个品和最后一个品的选择直接影响整场 GMV。需要基于历史数据给出排序建议。

---

## 三、升级建议（按优先级）

### P0 — 必须立即修复

| # | 升级项 | 说明 |
|---|--------|------|
| U1 | **统一设计文档和实现** | 以设计文档为准，补齐实体字段：LiveSession 加 persona_id/session_cover/planned_start_time/planned_end_time/script_style；LiveScript 加 script_type/style/is_ai_generated；LiveProduct 加 script_source/product_script_id |
| U2 | **实现 AI 话术生成** | 实现 6 个话术生成接口，对接 ai 模块的 AiGenerateService。开场→商品→过渡→结尾→完整话术 |
| U3 | **实现直播数据同步** | 新增 live_session_data 和 live_product_data 表（按设计文档），对接抖音 API 在直播结束后自动拉取汇总数据 |
| U4 | **LiveSession 状态机校验** | 实现状态转换校验：preparing → live → ended，禁止逆向转换 |

### P1 — 核心体验提升

| # | 升级项 | 说明 |
|---|--------|------|
| U5 | **合规检测集成** | 话术生成后自动调用 script 模块的违规词检测，标记违规词并提供替换建议 |
| U6 | **话术效果归因** | 新增定时任务：直播结束后，关联 LiveScript.actualExecutionTime 和 LiveMonitor 时序数据，计算每句话术执行后 30s 内的观众/互动变化 |
| U7 | **直播数据看板页面** | 新增前端页面：直播汇总数据 + 时序图表 + 商品销售排名 + 话术效果排名 |
| U8 | **AI 复盘报告** | 直播结束后自动生成复盘报告（对接 AI 模块 12-升级路线图中的直播复盘 Agent） |
| U9 | **LiveScript 关联 ai_call_log** | 新增 ai_call_log_id 字段，追踪话术生成来源 |

### P2 — 体验优化

| # | 升级项 | 说明 |
|---|--------|------|
| U10 | **直播准备清单** | 开播前自动检查：商品已选？话术已生成？合规已通过？人设已设置？ |
| U11 | **实时数据 WebSocket** | 对接 ai 模块 05-直播分析中的 WebSocket 方案，实时推送直播数据到前端 |
| U12 | **商品排期优化** | 基于历史 live_product_data 分析最佳商品上播顺序 |
| U13 | **话术模板库** | 高效话术自动入库到 live_script_template（ai 模块 05 中已设计），下次直播可复用 |
| U14 | **前端 position 限制放开** | LiveProduct position 从 1-5 改为 1-100 |

---

## 四、数据库变更建议

```sql
-- U1: LiveSession 补齐字段
ALTER TABLE live_session ADD COLUMN persona_id BIGINT;
ALTER TABLE live_session ADD COLUMN session_cover VARCHAR(512);
ALTER TABLE live_session ADD COLUMN planned_start_time TIMESTAMP;
ALTER TABLE live_session ADD COLUMN planned_end_time TIMESTAMP;
ALTER TABLE live_session ADD COLUMN script_style VARCHAR(64);

-- U1: LiveScript 补齐字段
ALTER TABLE live_script ADD COLUMN script_type VARCHAR(32);  -- opening/product/transition/closing/full
ALTER TABLE live_script ADD COLUMN style VARCHAR(64);
ALTER TABLE live_script ADD COLUMN is_ai_generated BOOLEAN DEFAULT false;

-- U1: LiveProduct 补齐字段
ALTER TABLE live_product ADD COLUMN script_source VARCHAR(32);  -- product/session
ALTER TABLE live_product ADD COLUMN product_script_id BIGINT;

-- U3: 直播汇总数据表（按设计文档）
CREATE TABLE live_session_data (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    total_viewers INTEGER DEFAULT 0,
    peak_viewers INTEGER DEFAULT 0,
    avg_watch_duration INTEGER DEFAULT 0,
    new_followers INTEGER DEFAULT 0,
    likes INTEGER DEFAULT 0,
    comments INTEGER DEFAULT 0,
    shares INTEGER DEFAULT 0,
    gift_value DECIMAL(12,2) DEFAULT 0,
    total_gmv DECIMAL(12,2) DEFAULT 0,
    total_orders INTEGER DEFAULT 0,
    conversion_rate DECIMAL(5,2) DEFAULT 0,
    avg_order_value DECIMAL(10,2) DEFAULT 0,
    duration_minutes INTEGER DEFAULT 0,
    ai_analysis TEXT,
    ai_suggestions TEXT,
    last_sync_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_session_data ON live_session_data(session_id);

-- U3: 分产品数据表（按设计文档）
CREATE TABLE live_product_data (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    gmv DECIMAL(12,2) DEFAULT 0,
    orders INTEGER DEFAULT 0,
    click_count INTEGER DEFAULT 0,
    conversion_rate DECIMAL(5,2) DEFAULT 0,
    refund_rate DECIMAL(5,2) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_product_data ON live_product_data(session_id, product_id);

-- U6: 话术效果归因
ALTER TABLE live_script ADD COLUMN ai_call_log_id BIGINT;
ALTER TABLE live_script ADD COLUMN viewer_delta INTEGER;      -- 执行后 30s 观众变化
ALTER TABLE live_script ADD COLUMN interaction_delta INTEGER;  -- 执行后 30s 互动变化
ALTER TABLE live_script ADD COLUMN effectiveness_score DECIMAL(5,2);  -- 效果评分
```

---

## 五、与 AI 模块的协同升级

| AI 模块能力 | 直播模块配合 |
|------------|-------------|
| AI 话术生成（01-需求分析 1.3） | LiveScript 存储生成结果，关联 ai_call_log_id |
| 直播实时分析（05-直播分析） | LiveMonitor 提供时序数据，WebSocket 推送到前端 |
| 直播复盘 Agent（12-升级路线图） | live_session_data 提供汇总数据，LiveScript + LiveMonitor 提供归因数据 |
| 效果归因链（12-升级路线图） | LiveScript.ai_call_log_id 关联 AI 调用，effectiveness_score 反馈话术质量 |
| 话术模板库（05-直播分析） | 高效话术（effectiveness_score > 80）自动入库 live_script_template |

---

## 六、总评

| 维度 | 当前评分 | 目标评分 | 差距 |
|------|---------|---------|------|
| 设计完整度 | 7/10 | 9/10 | 设计文档质量高，但实现严重滞后 |
| 实现完整度 | 3/10 | 8/10 | 核心 AI 功能全部未实现 |
| 数据完整度 | 2/10 | 8/10 | 无抖音数据同步，无汇总数据 |
| AI 集成度 | 0/10 | 9/10 | 话术生成、复盘、归因全部缺失 |
| 用户体验 | 3/10 | 8/10 | 只有 3 个基础 CRUD 页面 |
| 运营实用性 | 2/10 | 9/10 | 目前只是场次记录工具 |

**一句话总结：直播模块的设计文档质量不错，但实现只完成了约 30%。核心 AI 话术生成（6 个接口）完全未实现，数据同步未对接抖音 API，直播复盘和话术效果归因也是空白。当前它只是一个「直播场次记录本」，距离「AI 直播运营助手」还有很大差距。好消息是：LiveScript 的执行追踪 + LiveMonitor 的时序数据已经为话术效果归因打好了基础，这是一个市面上几乎没有竞品做到的差异化能力。**

---
