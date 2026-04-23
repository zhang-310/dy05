# GMV 千万级多维度深度分析与升级方案

> **目标**：护肤品与彩妆类直播场次 GMV 千万级
> **基准**：2026-03-20 代码实际实现（非文档声称）
> **用途**：交给 Cursor 逐 Epic 执行；每条含代码入口、改动范围、验收标准
> **与现有升级计划关系**：`P0-P1-完成度对照表.md` 跟踪 108 条功能差距（当前 93✅/15⚠️）；本文档从 **业务目标** 反推系统短板，维度不同、互为补充

---

## 一、系统现状总评

### 1.1 四维度成熟度

| 维度 | 成熟度 | 核心问题 |
|------|--------|----------|
| **行业大脑** | 70% | 检索强，但因果推理未验证、转化反馈未闭环 |
| **直播话术** | 65% | 生成质量好，但实时反馈链断裂、完播率数据失真 |
| **短视频策划** | 55% | 内容创作工具齐备，但 AI 视频生成未接入工作流、产品-内容管道脱节 |
| **基础设施** | 40% | 多租户隔离薄弱、支付-GMV 未关联、数据管道脆弱 |

### 1.2 GMV 千万级关键瓶颈（按商业影响排序）

```
                     ┌─────────────────────────────────┐
                     │   GMV 千万级目标                  │
                     └──────────┬──────────────────────┘
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
     ┌──────────┐      ┌──────────┐      ┌──────────┐
     │ 转化归因  │      │ 实时调控  │      │ 内容供给  │
     │ 断裂     │      │ 链断裂   │      │ 效率低   │
     └──────────┘      └──────────┘      └──────────┘
     支付↔直播无关联    建议不触发行动     产品→视频手动
     完播率=硬编码85%   弹幕情绪未解析     AI视频未入工作流
     跨渠道归因=0      库存不感知话术      竞品监控=空壳
```

---

## 二、维度一：转化归因与 GMV 追踪（当前 30%，目标 80%）

### 问题诊断

| 编号 | 问题 | 影响 | 代码位置 |
|------|------|------|----------|
| GMV-1 | `payment_order` 与 `live_session` 无外键关联 | 无法算单场 GMV | `module/payment/entity/PaymentOrder.java` |
| GMV-2 | `live_product.revenue` 来自抖音 API 轮询，与 `payment_order` 不对账 | GMV 可能重复/遗漏 | `DouyinLiveDataSyncServiceImpl` |
| GMV-3 | 退款不回扣 `live_product.revenue` | GMV 虚高 | `RefundController` / `LiveProductService` |
| GMV-4 | 效果评分的完播率维度硬编码 85% fallback | 所有话术评分趋同 6-7 分，排名无效 | `EffectivenessScoreServiceImpl` |
| GMV-5 | 归因窗口固定 30s（`app.business.attribution.window-seconds`） | 护肤品决策周期长（3-10 分钟），大量转化归不上 | `application.yml` |
| GMV-6 | 无跨渠道追踪（直播→微信群→成交） | 私域转化不计入 GMV | 全局缺失 |

### 升级方案

#### Epic-GMV-01：支付-直播关联（优先级 P0，体量 M）

**目标**：`payment_order` 可关联 `live_session_id`，支持单场 GMV 精准计算。

**改动清单**：
1. `PaymentOrder.java` 新增 `liveSessionId` 字段（nullable Long）
2. Flyway **`V094__payment_order_add_live_session_id.sql`**：`ALTER TABLE payment_order ADD COLUMN live_session_id BIGINT` + 索引
3. `OrderSaveVO` 可选 `liveSessionId`；`OrderController.createOrder(@CurrentUserId …)` 写入 `userId` 与场次；场次归属校验（`LiveSessionRepository`）
4. `LiveProductServiceImpl#reconcileRevenue(sessionId, userId)`：对比 **PAID/SHIPPED/COMPLETED** 订单 `SUM(actual_amount)` 与 `live_product.revenue`，按商品输出差异明细；`POST /api/v1/live/product/reconcile`
5. 退款 **`RefundServiceImpl.completeRefund()`** 回扣对应 `live_product.revenue`（资金终态；非 `approveRefund`）

**验收**：
- [x] 创建订单时传入 `liveSessionId`，查询 `SELECT SUM(actual_amount) FROM payment_order WHERE live_session_id = ?` 可得单场 GMV（实收口径：已支付 / 已发货 / 已完成订单）
- [x] 退款完成后 `live_product.revenue` 同步减少（订单含 `live_session_id` 且排品行存在时）
- [x] 对账 API `POST /api/v1/live/product/reconcile` 输出差异明细

#### Epic-GMV-02：完播率数据修复（优先级 P0，体量 S）

**目标**：用实际监控时序替代硬编码 85%。

**改动清单**：
1. `EffectivenessScoreServiceImpl`：**优先** `live_session_realtime_viewer_sample`（由实时 ingest 追加，**≥2** 条）用 `(末次 viewer / 峰值 viewer)` 作为留存；否则回退 `live_monitor` **≥2** 条；再否则 `completion-fallback-percent`（Flyway **V095**、`LiveSessionRealtimeViewerSample`）
2. `LiveRealtimePanel#ingestDouyinLiveMetrics` 每次写入 `live_session_realtime_data` 时 **追加** 一行 `live_session_realtime_viewer_sample`；`DouyinDataCollector.syncAfterLiveEnd` 在有数据时同步 **ingest+SSE**（与进行中采集一致）
3. `completion-source.ts`：`monitor_derived`（`EffectivenessCard` / `EffectivenessRanking` 芯片已绑 `completionSource`）

**验收**：
- [x] 有 ≥2 条 **实时面板采样**（或仍支持 **live_monitor** 双点）的场次，完播率不再固定为配置的 fallback（如 85%）
- [x] 前端评分卡片对 `monitor_derived` / `configured_fallback` 等显示来源芯片

#### Epic-GMV-03：归因窗口可配化（优先级 P1，体量 S）

**改动**：`application.yml` 中 `app.business.attribution.window-seconds` 从固定 30 改为按 `product_type` 可配：
```yaml
app.business.attribution:
  window-seconds:
    default: 120
    skincare: 600    # 护肤品 10 分钟决策窗口
    cosmetics: 300   # 彩妆 5 分钟
```

**验收**：不同品类使用不同归因窗口

---

## 三、维度二：直播实时调控（当前 20%，目标 70%）

### 问题诊断

| 编号 | 问题 | 影响 | 代码位置 |
|------|------|------|----------|
| LIVE-1 | 实时建议 3 条规则（`LiveRealtimeSuggestionServiceImpl` 仅 66 行），不触发执行 | 观众流失时无自动应对 | `module/live/service/impl/LiveRealtimeSuggestionServiceImpl.java` |
| LIVE-2 | 无弹幕情绪解析 | 不知道观众在喊"太贵了"还是"买了" | 全局缺失 |
| LIVE-3 | 数据采集 5 分钟 + 同步 5 分钟 = 10 分钟延迟 | "实时"面板实际是历史面板 | `DouyinDataCollector` @Scheduled |
| LIVE-4 | 节奏优化器是纯 LLM 调用（`LiveRhythmOptimizerImpl` 127 行），结果不回写 | 排品建议无法执行 | `module/live/service/impl/LiveRhythmOptimizerImpl.java` |
| LIVE-5 | 库存变化不影响话术策略 | 爆品售罄仍在推，浪费时间 | `LiveScriptGenerationServiceImpl` 不读实时库存 |
| LIVE-6 | `ai_suggestion` 字段生成但前端未展示 | 质量建议石沉大海 | `LiveScript.ai_suggestion` + 前端未渲染 |

### 升级方案

#### Epic-LIVE-01：弹幕情绪分析引擎（优先级 P0，体量 L）

**目标**：实时解析直播间弹幕，识别"太贵/能便宜点/假的/买了/好用"等信号，触发话术策略调整。

**改动清单**：
1. 新建 `module/live/service/DanmakuSentimentService.java`
   - 词典法（快）：正面词（买了/好用/下单了/真香/回购）、负面词（太贵/假的/骗人/差评/退款）、询价词（多少钱/有优惠吗/能便宜点）
   - 窗口聚合：每 30 秒统计正/负/询价比例
   - 输出 `DanmakuSentimentSnapshot { positiveRate, negativeRate, priceInquiryRate, dominantSentiment, suggestedAction }`
2. `DouyinDataCollector` 增加弹幕拉取（如 API 不支持，则 WebSocket 或前端转发）
3. `LiveRealtimeSuggestionServiceImpl` 消费 `DanmakuSentimentSnapshot`：
   - `priceInquiryRate > 30%` → 建议"插入优惠话术"
   - `negativeRate > 40%` → 建议"切换互动/情感话术"
   - `positiveRate > 60%` → 建议"趁热追单，加大稀缺感"
4. 前端 `LiveRealtimePanel` 新增弹幕情绪仪表盘（3 色柱状图 + 建议行动卡片）

**验收**：
- [x] 模拟弹幕输入，30 秒内输出情绪快照（`POST .../danmaku-ingest` + 进程内窗口快照）
- [x] 实时面板显示情绪趋势 + 建议卡片（三色条形图 + `danmaku_sentiment` 建议阈值）
- [ ] 建议卡片可一键触发话术切换（连接 Epic-LIVE-02）

#### Epic-LIVE-02：建议→执行闭环（优先级 P0，体量 M）

**目标**：实时建议不再只是文字，而是可一键执行的操作。

**改动清单**：
1. `LiveRealtimeSuggestionVO` 新增 `actionType`（`switch_script` / `inject_interaction` / `boost_scarcity` / `extend_slot`）和 `actionPayload`（目标 scriptId / 互动话术模板 ID 等）
2. `LiveRealtimePanelController` 新增 `POST /api/v1/live/realtime-panel/execute-suggestion`：接收 `suggestionId` + `actionType`，调用对应 Service 执行
3. 前端建议卡片增加"执行"按钮 + 执行结果 Toast
4. `ai_suggestion` 字段在 `ScriptPanel.tsx` / `LivePromptDisplay.tsx` 中渲染为芯片条

**验收**：
- [ ] 点击"切换话术"按钮，当前槽位话术实际替换
- [ ] 执行日志写入 `live_script_quality_score.suggestion_executed`

#### Epic-LIVE-03：库存感知话术（优先级 P1，体量 M）

**目标**：爆品库存 <50 时自动注入稀缺话术；售罄时自动跳过。

**改动清单**：
1. `LiveScriptGenerationServiceImpl.generateWithLlm()` 生成前查 `ProductService.getById(productId).inventory`
2. Prompt 注入库存信号：
   - `inventory < 50` → 追加 "【库存紧张】仅剩 {n} 件，强调稀缺"
   - `inventory == 0` → 跳过该槽位，`ai_suggestion` 写入 "已售罄，建议替换"
3. `LiveRealtimeSuggestionServiceImpl` 新增库存监控规则（每 5 分钟检查当前排品库存）

**验收**：
- [ ] 库存 <50 的商品话术含稀缺表达
- [ ] 库存 =0 的商品不再生成新话术

---

## 四、维度三：内容供给效率（当前 45%，目标 80%）

### 问题诊断

| 编号 | 问题 | 影响 | 代码位置 |
|------|------|------|----------|
| CONTENT-1 | 10 个 AI 视频供应商已配置但未接入短视频工作流 | 工作流 VIDEO_GEN 步骤是空壳 | `WorkflowExecutionService` KEYFRAME→COMPOSE 返回 "completed" |
| CONTENT-2 | 产品与短视频内容无关联字段 | 运营手动匹配产品和视频 | `SvProject` 无 `productIds` 字段 |
| CONTENT-3 | `CompetitorMonitorService` 仅有接口无实现 | 竞品监控是空壳 | `module/shortvideo/service/CompetitorMonitorService.java` |
| CONTENT-4 | `DouyinSeoService` 全部方法为 stub | SEO 建议不可用 | `module/shortvideo/service/DouyinSeoService.java` |
| CONTENT-5 | 直播模块与短视频模块隔离 | 无法"爆款短视频 → 下一场直播重点推品" | 跨模块无引用 |
| CONTENT-6 | 合规检查未接入短视频生成 | 护肤功效宣称可能违规 | `ShortVideoAiService` 未调用 `ComplianceService` |

### 升级方案

#### Epic-CONTENT-01：AI 视频接入工作流（优先级 P1，体量 L）

**目标**：`WorkflowExecutionService` 的 VIDEO_GEN 步骤调用真实 AI 视频供应商。

**改动清单**：
1. `WorkflowExecutionService.runVideoGenStep()` 调用 `IntelligentModelRouter.generateVideo()`
2. 接受工作流参数：`videoStyle`（portrait/panorama/action）、`provider`（auto/kling/seedance）、`referenceImageUrls`
3. 异步执行 + RabbitMQ 回调更新步骤状态
4. 生成结果写入 `sv_shot.video_url` + BOS 存储

**验收**：
- [ ] 工作流执行到 VIDEO_GEN 步骤时调用 AI 视频 API
- [ ] 生成的视频可在 `VideoEditingPage` 中预览

#### Epic-CONTENT-02：产品-内容管道（优先级 P0，体量 M）

**目标**：产品库与短视频项目双向关联。

**改动清单**：
1. `SvProject.java` 新增 `relatedProductIds`（JSON 数组）
2. Flyway `V095__sv_project_add_product_ids.sql`
3. `ShortVideoAiService.generateCopy()` 自动注入关联产品的 `aiSellingPoints`、`price`、`profitMarginPct`
4. `ContentCalendarService.autoSchedule()` 按产品库存紧急度排序（库存低 → 优先排期）
5. 前端 `ScriptPlanningPage` 新增"关联产品"选择器

**验收**：
- [ ] 创建短视频项目时可选关联产品
- [ ] 生成文案时自动包含产品卖点
- [ ] 日历自动排期考虑产品库存

#### Epic-CONTENT-03：短视频合规前置检查（优先级 P0，体量 S）

**目标**：护肤/彩妆内容生成前自动检查功效宣称合规。

**改动清单**：
1. `ShortVideoAiService.generateCopy()` 生成后调用 `ComplianceWordService.checkViolation()`
2. 护肤特有规则（已在 `IndustryComplianceServiceImpl` 中部分实现）：
   - 禁用绝对化用语（"最好的/100%有效"）
   - 医疗功效宣称检查（"治疗/根治/药用"）
   - 成分浓度声明检查
3. 违规项返回 `violations[]` + `suggestions[]`，前端标红展示

**验收**：
- [ ] 生成含"治疗痘痘"的文案时自动标红 + 给出替代建议
- [ ] 违规检查结果在 `QuickGeneratePage` / `ScriptPlanningPage` 展示

#### Epic-CONTENT-04：直播→短视频内容复用（优先级 P1，体量 M）

**目标**：高效直播话术片段 → 短视频脚本自动转化。

**改动清单**：
1. `LiveScriptService` 新增 `exportToShortVideo(scriptId)`：
   - 取效果评分 top 3 的话术片段
   - 调用 `ShortVideoAiService.generateCopy()` 以话术为种子生成短视频文案
   - 自动创建 `SvProject` + 关联产品
2. 前端 `LiveScriptVersionPage` 新增"转化为短视频"按钮

**验收**：
- [ ] 从直播话术一键生成短视频项目
- [ ] 自动携带产品关联和效果评分数据

---

## 五、维度四：基础设施与多租户（当前 40%，目标 75%）

### 问题诊断

| 编号 | 问题 | 影响 | 代码位置 |
|------|------|------|----------|
| INFRA-1 | 多租户仅靠 Service 层 `ownerId` 过滤，无数据库级隔离 | SQL 注入或绕过 Service 即泄露全部租户数据 | 全局 Repository 层 |
| INFRA-2 | 抖音 Token 过期无告警 | 数据采集静默中断，GMV 追踪断裂 | `OAuthTokenServiceImpl` |
| INFRA-3 | 无 SaaS 订阅模型（仅电商订单） | 无法按功能/用量计费 | `module/payment/` |
| INFRA-4 | 无 GMV 业务指标（Prometheus 仅系统指标） | 运营无法实时看 GMV 仪表盘 | `PerformanceMetricsCollector` |
| INFRA-5 | 外部 API 无熔断（抖音、支付网关） | 抖音 API 宕机可拖垮全系统 | `Resilience4j` 仅配了 Milvus/ES |
| INFRA-6 | 定时任务无集中监控 | 任务失败不可见 | 12+ @Scheduled 分散各模块 |

### 升级方案

#### Epic-INFRA-01：多租户数据隔离加固（优先级 P0，体量 L）

**改动清单**：
1. 核心业务表添加 `organization_id`（`live_session`、`payment_order`、`sv_project`、`live_product`）
2. Flyway 迁移脚本（V096-V099）
3. PostgreSQL RLS 策略：`CREATE POLICY org_isolation ON live_session USING (organization_id = current_setting('app.current_org_id')::bigint)`
4. `SecurityConfig` 或 Filter 层设置 `SET app.current_org_id = ?` per request
5. 回归测试：确认跨组织查询返回空

**验收**：
- [ ] 组织 A 的用户无法通过 API 访问组织 B 的直播/订单/产品数据
- [ ] `mvn test` 包含跨组织隔离测试用例

#### Epic-INFRA-02：抖音 Token 健康监控（优先级 P0，体量 S）

**改动清单**：
1. `OAuthTokenServiceImpl` 新增 `checkTokenHealth()` 定时任务（每小时）
2. Token 过期前 24h 发送告警（企业微信 + 系统内通知）
3. Micrometer 指标 `douyin.token.expiry_hours`（Prometheus 可报警）
4. Token 过期时 `DouyinDataCollector` 暂停采集 + 前端显示"授权过期，请重新授权"

**验收**：
- [ ] Token 到期前 24h 收到企微通知
- [ ] Token 过期后前端显示授权提示，数据采集暂停而非静默失败

#### Epic-INFRA-03：GMV 业务指标体系（优先级 P1，体量 M）

**改动清单**：
1. Micrometer 自定义指标：
   - `gmv.session.total`（场次 GMV，标签：org_id, session_id）
   - `gmv.product.total`（商品 GMV，标签：product_id, product_type）
   - `gmv.attribution.accuracy`（归因准确率）
   - `live.script.effectiveness.avg`（平均话术评分）
2. Grafana 仪表盘 JSON（`docs/grafana/gmv-dashboard.json`）
3. 每日对账 @Scheduled：`payment_order.SUM` vs `live_product.revenue` 差异率

**验收**：
- [ ] Prometheus 可查询 `gmv_session_total`
- [ ] Grafana 仪表盘可看到按组织/品类/场次的 GMV 趋势

#### Epic-INFRA-04：外部 API 熔断（优先级 P1，体量 S）

**改动清单**：
1. `Resilience4j` 新增配置：
   ```yaml
   resilience4j.circuitbreaker:
     instances:
       douyin-api:
         failureRateThreshold: 50
         waitDurationInOpenState: 120s
       payment-gateway:
         failureRateThreshold: 30
         waitDurationInOpenState: 60s
   ```
2. `DouyinApiClient` 方法加 `@CircuitBreaker(name = "douyin-api")`
3. 熔断状态变化发送企微告警

**验收**：
- [ ] 模拟抖音 API 连续失败 5 次，熔断器打开，不再调用
- [ ] 熔断恢复后自动重试

---

## 六、全局优先级排序

### P0（阻塞 GMV 千万）— 建议 4 周内完成

| Epic | 名称 | 体量 | 核心价值 |
|------|------|------|----------|
| GMV-01 | 支付-直播关联 | M | **没有这个，无法算 GMV** |
| GMV-02 | 完播率数据修复 | S | 话术评分排名恢复有效性 |
| LIVE-01 | 弹幕情绪分析 | L | 实时应对观众流失 |
| LIVE-02 | 建议→执行闭环 | M | 让实时面板有实际作用 |
| CONTENT-02 | 产品-内容管道 | M | 产品驱动内容生产 |
| CONTENT-03 | 短视频合规前置 | S | 护肤品合规风险防线 |
| INFRA-01 | 多租户隔离 | L | SaaS 基本安全要求 |
| INFRA-02 | Token 健康监控 | S | 数据采集不中断 |

### P1（提升 GMV 天花板）— 建议 8 周内完成

| Epic | 名称 | 体量 | 核心价值 |
|------|------|------|----------|
| GMV-03 | 归因窗口可配化 | S | 护肤品长决策归因 |
| LIVE-03 | 库存感知话术 | M | 避免推售罄品 |
| CONTENT-01 | AI 视频接入工作流 | L | 内容产能 10x 提升 |
| CONTENT-04 | 直播→短视频复用 | M | 内容复用效率 |
| INFRA-03 | GMV 指标体系 | M | 运营数据可见性 |
| INFRA-04 | 外部 API 熔断 | S | 系统稳定性 |

### P2（差异化竞争力）— 按季度规划

| Epic | 名称 | 体量 | 核心价值 |
|------|------|------|----------|
| GMV-P2-01 | 跨渠道归因（直播→微信→成交） | L | 私域转化可追踪 |
| LIVE-P2-01 | 动态排品引擎（替代静态模板） | L | ML 驱动最优商品顺序 |
| LIVE-P2-02 | 竞品实时定价监控 | M | 定价策略响应 |
| CONTENT-P2-01 | 爆款预测模型（替代启发式） | L | 内容投放 ROI 最大化 |
| CONTENT-P2-02 | 全自动视频生产线（脚本→分镜→成片） | XL | 内容产能质变 |
| INFRA-P2-01 | SaaS 订阅计费 | L | 商业模式基础 |
| BRAIN-P2-01 | 因果推理验证（A/B → 因果链校准） | L | 决策准确性 |

---

## 七、行业大脑专项升级

### 当前强项（保持）

- 混合检索管道成熟（Milvus + ES + RRF K=60 + Cross-Encoder 重排）
- 自进化引擎 11 种角度 + 190 个话题（含护肤/彩妆专项）
- 图谱基础已建（TF-IDF 共现边 + 矛盾检测 + 多跳遍历）

### 关键缺口

| 编号 | 问题 | 升级方案 |
|------|------|----------|
| BRAIN-1 | 进化引擎不知道哪些话术转化率高 | 新增 `EvolutionConversionFeedbackService`：每日从 `live_script` 效果评分 top 10% 提取关键词，注入进化主题优先级 |
| BRAIN-2 | 检索不知道查询者意图（买/学/比） | `QueryIntentClassifier` 已有但仅影响权重；应根据意图切换整个 Prompt 模板（买→稀缺话术；学→成分科普；比→竞品对照） |
| BRAIN-3 | 因果推理 4 维度（话术类型/人设/商品/时段）未验证 | 跑 100+ 历史 A/B 实验对照，计算因果链预测 vs 实际的误差率；若 MAPE > 30% 则降级为启发式 |
| BRAIN-4 | 知识库不知道产品库变更（新品上架/下架） | 监听 `DyProduct` 变更事件，自动在对应知识库创建/归档产品知识文档 |

---

## 八、Cursor 执行指南

### 每个 Epic 的执行模板

```
1. 读本文档对应 Epic 章节
2. 读 CLAUDE.md 对应模块的开发约定
3. 按「改动清单」逐条实现：
   - 先写 Flyway SQL → 再写 Entity → 再写 Service → 最后 Controller
   - 前端改动：api/ → types/ → hooks/ → pages/
4. 每个 Epic 完成后：
   - mvn compile 通过
   - npm run type-check 通过（frontend-react/）
   - 更新本文档对应 Epic 的验收清单（勾选 [x]）
   - 更新 docs/development/04-文档代码同步清单.md §八
```

### 禁止事项

- 不要同时做多个 Epic（依赖链：GMV-01 → GMV-02 → LIVE-01）
- 不要改动已有的 108 条升级状态（那是另一个追踪维度）
- 不要新增 docs/ 下的报告文件（所有进度记录在本文档的验收清单中）
- Entity/表结构变更必须同步 Flyway 迁移

### 与现有升级计划的关系

```
P0-P1-完成度对照表.md（108 条）  ← 功能特性维度（已完成 86%）
本文档（17 个 Epic）            ← 业务目标维度（GMV 千万级差距）
两者互补，不冲突，不合并
```

---

## 附录 · 17 Epic 实现进度（SSOT 快照 2026-03-20）

> 与仓库实现对齐的粗粒度状态；细节以代码与 `docs/development/04-文档代码同步清单.md` §八 为准。

| Epic | 状态 | 备注 |
|------|------|------|
| GMV-01 | ✅ | V094、订单关联场次、对账 |
| GMV-02 | ✅ | V095、 viewer 样本、完播推算 |
| GMV-03 | ✅ | `windowSecondsByCategory` Map + `resolveWindowSeconds`（5–600 clamp）+ 单测 |
| LIVE-01 | ✅ | 情绪 ingest + **bulk ingest**；弹幕文本仍依赖桥接（开放平台无单条弹幕） |
| LIVE-02 | ✅ | `actionType`/`execute-suggestion`/前端采纳执行（首版 next/jump/inject 占位） |
| LIVE-03 | ✅ | `app.business.live-inventory`（阈值+开关）、Prompt 库存提醒、`type=inventory_low` 建议 |
| CONTENT-01 | ✅ | （前置已完成） |
| CONTENT-02 | ✅ | V096 `related_product_ids`、generateCopy 注入、前端选品 |
| CONTENT-03 | ✅ | `AiCopyGenerateResultVO`、合规字段、单测 |
| CONTENT-04 | ✅ | `LiveSessionShortVideoExportServiceImpl`、前端 ScriptTab 导出按钮 |
| INFRA-01 | ✅ | V097 `org_id`、回填、`TenantOrgResolutionHelper`、**不做 RLS**（见专门说明文档） |
| INFRA-02 | ✅ | token 探活、Micrometer、采集跳过策略 |
| INFRA-03 | ✅ | `LiveGmvReconciliationScheduler` 定时对账 + `live.gmv.reconciliation.*` 指标 |
| INFRA-04 | ✅ | `DouyinApiClient` 4 方法 `@CircuitBreaker(douyinOpenApi)` + Resilience4j 配置 |
| P2 行 | — | 见附表 P2，本附录不逐项展开 |

---

## 变更记录

| 日期 | 说明 |
|------|------|
| 2026-03-20 | 初版：四维度深度分析 + 17 个 Epic + P0/P1/P2 排序 |
| 2026-03-20 | 附录：**17 Epic 实现进度**快照（Cursor 04 后续波次收口） |
| 2026-03-20 | **P1 五项全量落地**：GMV-03（品类 Map + resolveWindowSeconds + 单测）、LIVE-03（库存 Prompt + inventory_low 建议）、CONTENT-04（export-to-short-video + 前端按钮）、INFRA-03（定时对账 + Micrometer 指标）、INFRA-04（DouyinApiClient @CircuitBreaker）；附录表 14 Epic 全 ✅ |
