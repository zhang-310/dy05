# 下一步迭代升级计划

> 基于 `docs/cursor/项目完成度与需求对照分析报告.md` 和 PRD  
> 当前完成度：78-82%  
> 目标完成度：90%+

---

## 迭代优先级

### P0 - 业务闭环补全（本迭代必做）

#### 1. 产品 AI 话术生成 ⭐⭐⭐
**优先级**: 最高  
**工作量**: 2-3 天

**需求**:
- PRD 要求「AI 生成产品话术（按人设+风格）」
- 当前 ProductController 和 LiveAiController 缺少产品话术生成接口

**实现方案**:
1. 在 `LiveAiController` 添加 `generateProductScript` 接口
2. 集成产品信息（名称、卖点、价格、库存）
3. 集成人设上下文（PersonaId）
4. 支持多种话术风格：
   - 种草话术（强调体验和感受）
   - 促销话术（强调优惠和限时）
   - 正式话术（专业介绍）
5. 使用 LLM 生成结构化话术
6. 保存到产品话术表

**API 设计**:
```java
POST /api/v1/live/ai/generate-product-script
{
  "productId": 123,
  "personaId": 456,
  "scriptType": "seed|promotion|formal",
  "style": "enthusiastic|professional|casual"
}
```

**数据库变更**:
- 扩展 `dy_product` 表或创建 `dy_product_script` 表

---

#### 2. 产品多套话术管理 ⭐⭐⭐
**优先级**: 最高  
**工作量**: 2-3 天

**需求**:
- 种草 / 促销 / 正式 三套话术管理
- 每个产品可保存多个版本的话术
- 话术启用/禁用状态

**实现方案**:
1. 创建 `dy_product_script` 表：
   ```sql
   CREATE TABLE dy_product_script (
       id BIGSERIAL PRIMARY KEY,
       product_id BIGINT NOT NULL,
       script_type VARCHAR(32) NOT NULL, -- seed/promotion/formal
       script_content TEXT NOT NULL,
       persona_id BIGINT,
       version INTEGER DEFAULT 1,
       is_active BOOLEAN DEFAULT true,
       created_by BIGINT,
       create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```

2. 创建 `ProductScriptController`：
   - `POST /api/v1/product/{productId}/script` - 保存话术
   - `GET /api/v1/product/{productId}/scripts` - 获取话术列表
   - `PUT /api/v1/product/script/{scriptId}` - 更新话术
   - `DELETE /api/v1/product/script/{scriptId}` - 删除话术
   - `PUT /api/v1/product/script/{scriptId}/activate` - 激活话术

3. 前端页面：
   - 产品话术管理页面
   - 话术类型切换
   - 话术版本管理
   - 话术预览和编辑

---

#### 3. 粉丝画像同步 ⭐⭐⭐
**优先级**: 高  
**工作量**: 3-4 天

**需求**:
- 抖音 API 粉丝画像同步
- 粉丝年龄、性别、地域分布
- 粉丝兴趣标签

**实现方案**:
1. 创建 `dy_fan_profile` 表：
   ```sql
   CREATE TABLE dy_fan_profile (
       id BIGSERIAL PRIMARY KEY,
       account_id BIGINT NOT NULL,
       age_range VARCHAR(32),
       gender VARCHAR(16),
       province VARCHAR(64),
       city VARCHAR(64),
       interest_tags TEXT, -- JSON array
       active_time VARCHAR(32),
       device_type VARCHAR(32),
       sync_time TIMESTAMP,
       create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```

2. 完善 `DouyinDataCollector`：
   - 添加粉丝画像采集任务
   - 调用抖音 API `/fans/data`
   - 定时同步（每天一次）

3. 创建 `FanProfileController`：
   - `GET /api/v1/douyin/fan-profile/{accountId}` - 获取粉丝画像
   - `POST /api/v1/douyin/fan-profile/sync` - 手动同步

4. 前端页面：
   - 粉丝画像展示页面
   - 年龄分布图表
   - 性别比例图表
   - 地域分布地图
   - 兴趣标签云

---

### P1 - 体验与功能增强（建议做）

#### 4. 违规词 AI 替换建议 ⭐⭐
**优先级**: 中高  
**工作量**: 2 天

**需求**:
- script 模块「AI 推荐合规替代词」
- 在 ViolationWord 检测后增加 LLM 替换接口

**实现方案**:
1. 在 `ScriptController` 添加 `suggestReplacement` 接口：
   ```java
   POST /api/v1/script/violation/suggest-replacement
   {
     "text": "原始文本",
     "violationWords": ["违规词1", "违规词2"]
   }
   ```

2. 使用 LLM 生成替换建议：
   - 保持原意和语境
   - 提供 3-5 个替换选项
   - 标注替换理由

3. 前端集成：
   - 违规检测后自动显示替换建议
   - 一键替换功能
   - 批量替换

---

#### 5. 增强复盘分析 ⭐⭐
**优先级**: 中  
**工作量**: 3-4 天

**需求**:
- 视频 vs 同类视频对比分析
- 视频 vs 历史视频对比分析
- 单视频深度 AI 分析

**实现方案**:
1. 在 `EvolutionController` 添加复盘分析接口：
   ```java
   POST /api/v1/evolution/video/compare
   {
     "videoId": 123,
     "compareType": "similar|history",
     "compareIds": [456, 789]
   }
   ```

2. AI 分析维度：
   - 数据对比（播放量、点赞率、完播率）
   - 内容结构对比
   - 标题对比
   - 封面对比
   - 发布时间对比
   - 改进建议

3. 前端页面：
   - 视频复盘分析页面
   - 对比图表
   - AI 分析报告
   - 改进建议列表

---

#### 6. 历史趋势分析 ⭐⭐
**优先级**: 中  
**工作量**: 2-3 天

**需求**:
- live 模块「历史直播数据对比、分时段分析」
- 需 LiveSessionData 聚合与趋势接口

**实现方案**:
1. 在 `LiveSessionController` 添加趋势分析接口：
   ```java
   GET /api/v1/live/session/trend
   ?accountId=123&startDate=2026-01-01&endDate=2026-02-01
   ```

2. 数据聚合：
   - 按日期聚合
   - 按时段聚合（早中晚）
   - 关键指标趋势（观看人数、互动率、转化率）

3. 前端页面：
   - 历史趋势图表
   - 分时段对比
   - 最佳时段推荐

---

### P2 - 技术债与一致性（可选）

#### 7. 整理 init.sql 和 migration 脚本 ⭐
**优先级**: 中低  
**工作量**: 1-2 天

**需求**:
- 将 migration-persona、migration-data-sync、migration-template-uservw 并入 init 或提供升级说明

**实现方案**:
1. 合并所有 migration 脚本到 `init.sql`
2. 创建 `MIGRATION.md` 文档：
   - 版本历史
   - 升级步骤
   - 回滚方案
3. 提供数据库版本管理机制

---

#### 8. 统一响应体格式 ⭐
**优先级**: 中低  
**工作量**: 2-3 天

**需求**:
- KnowledgeBaseController、ShortVideoAiController 使用 Result
- 其余为 RESTResult
- 建议统一为 RESTResult

**实现方案**:
1. 检查所有 Controller 的响应格式
2. 统一使用 `RESTResult`
3. 更新前端 API 调用
4. 确保向后兼容

---

#### 9. AI 从关键帧生成视频 ⭐
**优先级**: 低  
**工作量**: 5-7 天

**需求**:
- PRD 要求「首尾帧→视频片段」
- 当前 MediaController 以剪辑为主，可接入 Runway / Pika 等

**实现方案**:
1. 集成 Runway Gen-2 或 Pika API
2. 在 `MediaController` 添加关键帧生成视频接口
3. 支持首尾帧插值
4. 支持运动控制
5. 预留接口，待 AI 视频生成技术成熟后完善

---

## 本迭代实施计划

### 第一周（必做）
- ✅ Day 1-2: 产品 AI 话术生成
- ✅ Day 3-4: 产品多套话术管理
- ✅ Day 5: 粉丝画像同步（数据库设计 + API）

### 第二周（建议做）
- ✅ Day 1-2: 粉丝画像同步（前端页面）
- ✅ Day 3: 违规词 AI 替换建议
- ✅ Day 4-5: 整理数据库脚本

### 第三周（可选）
- ⭕ Day 1-2: 增强复盘分析
- ⭕ Day 3-4: 历史趋势分析
- ⭕ Day 5: 统一响应体格式

---

## 预期成果

完成本迭代后：
- **完成度**: 从 78-82% 提升到 90%+
- **业务闭环**: 产品话术、粉丝画像完整
- **用户体验**: 违规词替换、复盘分析增强
- **技术债**: 数据库脚本整理、响应体统一

---

## 后续规划（Phase 4-6）

### Phase 4: 高级分析
- 用户画像分析
- 内容推荐引擎
- A/B 测试平台
- 数据可视化大屏

### Phase 5: 自动化运营
- 自动发布调度
- 智能评论回复
- 粉丝互动自动化
- 数据报表自动生成

### Phase 6: 移动端
- 移动端 H5 页面
- 小程序版本
- App 版本

---

**文档版本**: v1.0  
**创建时间**: 2026-02-27  
**下次更新**: 迭代完成后
