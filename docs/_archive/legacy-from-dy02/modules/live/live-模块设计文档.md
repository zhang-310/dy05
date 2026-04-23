> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# live 模块设计文档

> 版本：1.1 | 更新日期：2026-02-24 | 阶段：P1

---

## 一、需求分析（Requirements Analysis）

### 1.1 模块定位

live 模块是平台的核心业务模块之一，围绕直播全流程管理：场次创建 → 选品排序 → AI 生成话术（开场/产品/转场/结尾）→ 违规检测 → 直播数据同步 → AI 数据分析复盘。产品数据来自 douyin 模块，AI 能力来自 ai 模块，违规检测来自 script 模块。

### 1.2 用户故事（User Stories）

| 编号 | 角色 | 故事 | 验收条件 |
|------|------|------|----------|
| LV-01 | 主播 | 我要能创建直播场次 | 填写主题/时间/绑定账号，场次创建成功 |
| LV-02 | 主播 | 我要能给场次关联产品 | 从 douyin 产品库选品，设置上播顺序 |
| LV-03 | 主播 | 我要能一键生成整场直播话术 | 按人设+风格，AI 生成开场→各产品→转场→结尾完整话术 |
| LV-04 | 主播 | 我要能按风格生成不同版本的话术 | 选择亲切/激情/专业等风格，每次生成不同版本 |
| LV-05 | 主播 | 我要能对话术做违规检测 | 生成后自动检测，标红违规词，提供替换建议 |
| LV-06 | 主播 | 我要能手动编辑话术 | AI 生成后可自由修改 |
| LV-07 | 主播 | 我要能把话术保存到话术库 | 好的话术可收藏到 script 模块的话术库复用 |
| LV-08 | 主播 | 我要能看到直播的实时数据 | 通过抖音 API 获取直播中的数据指标 |
| LV-09 | 主播 | 我要能看到直播结束后的数据 | 直播结束后同步完整数据 |
| LV-10 | 主播 | 我要能让 AI 帮我分析直播数据 | AI 给出评级、分析、改进建议 |
| LV-11 | 主播 | 我要能对比多场直播的数据 | 历史直播列表、趋势图、对比分析 |
| LV-12 | 主播 | 我要能管理公共和个人违规词库 | 查看公共库、维护个人库 |
| LV-13 | 管理员 | 我要能维护公共违规词库 | 违规词 CRUD，分类/等级管理 |

### 1.3 功能清单

```
live 模块
├── 场次管理
│   ├── 创建直播场次（绑定账号/时间/主题/封面）
│   ├── 编辑场次信息
│   ├── 删除场次
│   ├── 场次关联产品（从 douyin 产品库选品 + 排序）
│   ├── 场次状态管理（准备中→直播中→已结束）
│   ├── 场次列表（分页、按状态筛选）
│   └── 场次详情
│
├── 直播话术
│   ├── 开场话术（AI 按人设 + 场次主题生成）
│   ├── 产品话术（引用 douyin 已有的产品话术，或 AI 按人设+产品重新生成）
│   ├── 转场话术（AI 生成产品间过渡）
│   ├── 结尾话术（AI 按人设生成）
│   ├── 整场话术流程（将开场→产品1→转场→产品2→...→结尾串联）
│   ├── 一键生成整场话术
│   ├── 话术风格选择（专业/亲切/激情/种草/促销）
│   ├── 手动编辑话术
│   ├── 话术违规检测（调用 script 模块）
│   └── 保存话术到话术库（调用 script 模块）
│
├── 违规管理（数据由 script 模块提供，live 提供 UI 入口）
│   ├── 查看公共违规词库
│   ├── 管理个人违规词库
│   └── 话术一键检测
│
├── 直播数据
│   ├── 通过抖音 API 同步直播数据
│   ├── 直播数据看板（观看人数/峰值/互动/GMV）
│   ├── 分时段数据曲线
│   ├── 分产品数据（每个产品的 GMV/转化）
│   └── 历史直播数据对比
│
└── AI 数据分析
    ├── AI 整场直播分析（评级+分析+建议）
    ├── AI 分产品分析
    ├── AI 复盘报告生成
    └── 改进推荐（话术/选品/时段）
```

### 1.4 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 场次关联账号 | 一个场次关联一个抖音账号 |
| BR-02 | 场次关联产品 | 从 douyin 模块的产品库选品，设置排序 |
| BR-03 | 话术生成需人设 | 所有话术 AI 生成需要激活的人设，且人设必须属于当前用户（owner_id 校验） |
| BR-04 | 整场话术流程 | 按产品排序串联：开场 → 产品1话术 → 转场 → 产品2话术 → ... → 结尾 |
| BR-05 | 话术可引用或重生成 | 产品话术可直接引用 douyin 模块已有的产品话术，也可以为当场直播重新 AI 生成 |
| BR-06 | 自动违规检测 | AI 生成话术后自动调用 script 模块做违规检测，传入 scope=live（匹配 all + live_only 违规词） |
| BR-07 | 场次状态流转 | 准备中 → 直播中 → 已结束，单向流转 |
| BR-08 | 直播数据同步 | 直播结束后通过抖音 API 同步数据，可手动触发 |
| BR-09 | 数据隔离 | 用户只能看到自己的场次和数据（owner_id） |
| BR-10 | 违规词库分层 | 公共库（管理员维护，全用户可见）+ 个人库（用户私有） |
| BR-11 | 整场生成事务 | generate-full（一键生成整场话术）采用"尽力生成"策略：逐产品调用 AI 生成，单个产品失败不回滚已成功的部分，但标记该产品话术为 failed 状态，允许用户手动重试单个产品 |
| BR-12 | 跨模块 FK 校验 | live_session.account_id 必须属于当前用户（dy_account.owner_id = 当前用户），live_session_product.product_id 必须属于同一账号 |

### 1.5 模块依赖

```
依赖关系：
auth ← douyin（账号/人设/产品）← live
ai（AI 生成话术/分析）← live
script（违规检测/话术库）← live
```

---

## 二、数据库设计（Database Design）

### 2.1 ER 关系

```
dy_account ──→ live_session (一对多，通过 account_id)
                    │
               live_session_product (场次-产品关联，多对多)
                    │              │
               dy_product ←────────┘
                    │
               live_session_script (场次话术，一对多)
                    │
               live_session_data (直播数据，一对一)
                    │
               live_product_data (分产品数据，一对多)
```

### 2.2 表结构

#### live_session — 直播场次表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| account_id | BIGINT | NOT NULL | — | 关联抖音账号 ID |
| persona_id | BIGINT | | — | 使用的人设 ID |
| session_title | VARCHAR(256) | NOT NULL | — | 场次主题 |
| session_cover | VARCHAR(512) | | — | 封面 URL |
| planned_start_time | TIMESTAMP | | — | 计划开播时间 |
| planned_end_time | TIMESTAMP | | — | 计划结束时间 |
| actual_start_time | TIMESTAMP | | — | 实际开播时间 |
| actual_end_time | TIMESTAMP | | — | 实际结束时间 |
| session_status | VARCHAR(16) | NOT NULL | 'preparing' | preparing / live / ended |
| script_style | VARCHAR(64) | | — | 话术风格：专业/亲切/激情/种草/促销 |
| notes | TEXT | | — | 备注 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(owner_id, session_status, create_time DESC) WHERE deleted=0`、`(account_id) WHERE deleted=0`、`(persona_id) WHERE deleted=0`

#### live_session_product — 场次产品关联表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| session_id | BIGINT | NOT NULL | — | 场次 ID |
| product_id | BIGINT | NOT NULL | — | 产品 ID（dy_product.id） |
| sort_order | INTEGER | NOT NULL | 0 | 上播顺序（从小到大） |
| script_source | VARCHAR(16) | | 'product' | 话术来源：product（引用产品话术）/ session（本场生成） |
| product_script_id | BIGINT | | — | 引用的产品话术 ID（script_source=product 时） |
| session_script | TEXT | | — | 本场生成的产品话术（script_source=session 时） |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(session_id, product_id)`、`(session_id, sort_order)`

#### live_session_script — 场次话术表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| session_id | BIGINT | NOT NULL | — | 场次 ID |
| script_type | VARCHAR(32) | NOT NULL | — | 话术类型：opening（开场）/ transition（转场）/ closing（结尾）/ full（整场） |
| content | TEXT | NOT NULL | — | 话术内容 |
| style | VARCHAR(64) | | — | 风格 |
| is_ai_generated | INTEGER | NOT NULL | 0 | 是否 AI 生成 |
| sort_order | INTEGER | | 0 | 排序（转场话术用，对应在第几个产品后） |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(session_id, script_type) WHERE deleted=0`

#### live_session_data — 直播数据表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| session_id | BIGINT | NOT NULL | — | 场次 ID |
| total_viewers | BIGINT | | 0 | 总观看人数 |
| peak_viewers | BIGINT | | 0 | 峰值在线人数 |
| avg_watch_duration | INTEGER | | 0 | 平均观看时长（秒） |
| new_followers | BIGINT | | 0 | 新增粉丝数 |
| likes | BIGINT | | 0 | 点赞数 |
| comments | BIGINT | | 0 | 评论数 |
| shares | BIGINT | | 0 | 分享数 |
| gift_value | DECIMAL(12,2) | | 0 | 礼物价值 |
| total_gmv | DECIMAL(12,2) | | 0 | 总 GMV |
| total_orders | BIGINT | | 0 | 总订单数 |
| conversion_rate | DECIMAL(5,2) | | 0 | 转化率（%） |
| avg_order_value | DECIMAL(10,2) | | 0 | 客单价 |
| duration_minutes | INTEGER | | 0 | 直播时长（分钟） |
| ai_analysis | TEXT | | — | AI 分析报告（JSON） |
| ai_suggestions | TEXT | | — | AI 改进建议（JSON） |
| last_sync_time | TIMESTAMP | | — | 最后同步时间 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(session_id)`

#### live_product_data — 直播分产品数据表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| session_id | BIGINT | NOT NULL | — | 场次 ID |
| product_id | BIGINT | NOT NULL | — | 产品 ID |
| gmv | DECIMAL(12,2) | | 0 | 产品 GMV |
| orders | BIGINT | | 0 | 订单数 |
| click_count | BIGINT | | 0 | 点击次数 |
| conversion_rate | DECIMAL(5,2) | | 0 | 转化率（%） |
| refund_rate | DECIMAL(5,2) | | 0 | 退款率（%） |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(session_id, product_id)`

---

## 三、接口设计（API Design）

### 3.1 接口总表

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| **场次管理** | | | | |
| 1 | POST | /api/v1/live/session/list | 登录 | 场次列表（分页、筛选） |
| 2 | POST | /api/v1/live/session/get | 登录 | 场次详情 |
| 3 | POST | /api/v1/live/session/save | 登录 | 新增/编辑场次 |
| 4 | POST | /api/v1/live/session/delete | 登录 | 删除场次 |
| 5 | POST | /api/v1/live/session/update-status | 登录 | 更新场次状态 |
| **场次选品** | | | | |
| 6 | POST | /api/v1/live/session/product/list | 登录 | 场次已选产品列表（含排序） |
| 7 | POST | /api/v1/live/session/product/save | 登录 | 保存场次选品（全量覆盖） |
| **直播话术** | | | | |
| 8 | POST | /api/v1/live/script/generate-opening | 登录 | AI 生成开场话术 |
| 9 | POST | /api/v1/live/script/generate-product | 登录 | AI 生成某产品的直播话术 |
| 10 | POST | /api/v1/live/script/generate-transition | 登录 | AI 生成转场话术 |
| 11 | POST | /api/v1/live/script/generate-closing | 登录 | AI 生成结尾话术 |
| 12 | POST | /api/v1/live/script/generate-full | 登录 | AI 一键生成整场话术 |
| 13 | POST | /api/v1/live/script/list | 登录 | 场次话术列表 |
| 14 | POST | /api/v1/live/script/save | 登录 | 手动编辑保存话术 |
| 15 | POST | /api/v1/live/script/delete | 登录 | 删除话术 |
| 16 | POST | /api/v1/live/script/check-violation | 登录 | 话术违规检测 |
| 17 | POST | /api/v1/live/script/save-to-library | 登录 | 保存话术到话术库 |
| **直播数据** | | | | |
| 18 | POST | /api/v1/live/data/sync | 登录 | 同步直播数据 |
| 19 | POST | /api/v1/live/data/get | 登录 | 获取直播数据 |
| 20 | POST | /api/v1/live/data/product | 登录 | 获取分产品数据 |
| 21 | POST | /api/v1/live/data/history | 登录 | 历史直播数据对比 |
| 22 | POST | /api/v1/live/data/dashboard | 登录 | 直播数据总览看板 |
| **AI 分析** | | | | |
| 23 | POST | /api/v1/live/analysis/generate | 登录 | AI 生成直播分析报告 |
| 24 | POST | /api/v1/live/analysis/get | 登录 | 获取已有分析报告 |

### 3.2 关键接口详情

#### 接口 12：一键生成整场话术

```
POST /api/v1/live/script/generate-full
权限：登录（检查 AI 额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | Long | 是 | 场次 ID |
| style | String | 否 | 话术风格（不填用场次默认风格） |

**业务逻辑：**
1. 获取场次信息（主题、关联产品列表含排序）
2. 获取用户当前激活人设（douyin 模块）
3. 按产品排序，依次调用 ai 模块生成：
   - 开场话术（`live_script_opening` 模板）
   - 产品1话术（`live_script_product` 模板，传入产品信息）
   - 转场话术（`live_script_transition` 模板，产品1 → 产品2）
   - 产品2话术
   - ... 重复直到最后一个产品
   - 结尾话术（`live_script_closing` 模板）
4. 所有话术写入 live_session_script
5. 自动调用 script 模块做违规检测
6. 返回完整话术流程 + 违规检测结果

**响应：**

```json
{
  "status": 200,
  "data": {
    "sessionId": 1,
    "scripts": [
      { "type": "opening", "content": "各位宝宝们好...", "sortOrder": 0 },
      { "type": "product", "productId": 10, "productName": "面霜", "content": "今天第一款...", "sortOrder": 1 },
      { "type": "transition", "content": "好啦，接下来...", "sortOrder": 2 },
      { "type": "product", "productId": 11, "productName": "精华", "content": "这款精华...", "sortOrder": 3 },
      { "type": "closing", "content": "感谢宝宝们...", "sortOrder": 99 }
    ],
    "violations": [
      { "word": "最好的", "position": "产品1话术第3行", "level": "warning", "suggestion": "非常好的" }
    ],
    "quotaRemaining": 3
  }
}
```

---

#### 接口 7：保存场次选品

```
POST /api/v1/live/session/product/save
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | Long | 是 | 场次 ID |
| products | List | 是 | 产品列表（全量覆盖） |
| products[].productId | Long | 是 | 产品 ID |
| products[].sortOrder | Integer | 是 | 上播顺序 |
| products[].scriptSource | String | 否 | 话术来源：product / session，默认 product |
| products[].productScriptId | Long | 否 | 引用的产品话术 ID（scriptSource=product 时） |

**业务逻辑：**
1. 校验场次属于当前用户
2. 校验所有产品属于当前用户
3. 删除原有的 live_session_product 记录
4. 批量插入新的关联记录
5. 整个操作在一个事务中

---

#### 接口 23：AI 生成直播分析报告

```
POST /api/v1/live/analysis/generate
权限：登录（检查 AI 额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | Long | 是 | 场次 ID |

**业务逻辑：**
1. 获取直播数据（live_session_data）
2. 获取分产品数据（live_product_data）
3. 获取用户历史直播数据作为基准
4. 调用 ai 模块 `live_analysis` 模板
5. 结果写入 live_session_data.ai_analysis 和 ai_suggestions
6. 返回分析报告

---

## 四、页面设计（Frontend Design）

### 4.1 页面清单

| # | 页面 | 路径 | 入口 | 说明 |
|---|------|------|------|------|
| 1 | 直播场次列表 | /talent/live/sessions | 侧栏菜单 | 场次管理 |
| 2 | 场次详情/编辑 | /talent/live/sessions/:id | 列表点击 | 场次信息+选品+话术+数据 |
| 3 | 话术编辑页 | /talent/live/sessions/:id/scripts | 场次详情 | 整场话术编辑 |
| 4 | 直播数据看板 | /talent/live/dashboard | 侧栏菜单 | 历史直播数据总览 |
| 5 | 直播数据详情 | /talent/live/sessions/:id/data | 看板/列表点击 | 单场数据+AI分析 |
| 6 | 违规词管理 | /talent/live/violation | 侧栏菜单 | 公共库浏览+个人库管理 |
| 7 | 机构直播总览 | /org/live/sessions | 机构侧栏 | 旗下达人场次+数据汇总（DataScope） |
| 8 | 公共违规词管理 | /admin/live/violation | 管理端菜单 | 违规词 CRUD |

### 4.2 关键页面设计

#### 页面 2：场次详情页（核心操作页面）

```
┌────────────────────────────────────────────────────────────┐
│ ← 返回列表          直播场次：年货节专场直播                  │
├────────────────────────────────────────────────────────────┤
│ 状态: [准备中]  账号: 张三的抖音号  人设: 美妆博主小美       │
│ 计划时间: 2026-02-25 19:00 - 21:00                         │
│ 话术风格: [亲切 ▼]                                          │
│                                                            │
│ ── Tab: [选品排序] [话术预览] [直播数据] ──                  │
│                                                            │
│ ===== 选品排序 Tab =====                                    │
│ [添加产品]                                                  │
│ ┌────┬──────────────┬──────┬──────────┬──────────────┐     │
│ │ 序号│ 产品名称      │ 价格  │ 话术来源  │ 操作          │     │
│ │ 1  │ 秋冬面霜      │ ¥89  │ 引用(种草) │ ↑ ↓ 切换话术 删除│  │
│ │ 2  │ 玻尿酸精华    │ ¥69  │ 引用(促销) │ ↑ ↓ 切换话术 删除│  │
│ │ 3  │ 防晒喷雾      │ ¥49  │ 未选话术   │ ↑ ↓ 选择话术 删除│  │
│ └────┴──────────────┴──────┴──────────┴──────────────┘     │
│                                                            │
│ [保存选品]  [一键生成整场话术]                                │
│                                                            │
│ ===== 话术预览 Tab =====                                    │
│ ┌──────────────────────────────────────────────────┐       │
│ │ ▶ 开场话术（亲切风格）              [编辑] [重生成]│       │
│ │ 各位宝宝们晚上好！欢迎来到小美的直播间...        │       │
│ ├──────────────────────────────────────────────────┤       │
│ │ ▶ 产品1：秋冬面霜 ¥89              [编辑] [重生成]│       │
│ │ 好啦宝宝们，今天第一款给你们带来的是...          │       │
│ │ ⚠ 违规: "最好的" → 建议改为"非常好的"            │       │
│ ├──────────────────────────────────────────────────┤       │
│ │ ▶ 转场                              [编辑] [重生成]│      │
│ │ 好的这款面霜就介绍到这里...                       │       │
│ ├──────────────────────────────────────────────────┤       │
│ │ ▶ 产品2：玻尿酸精华 ¥69            [编辑] [重生成]│       │
│ │ 接下来这款精华液绝对是今天的王炸...              │       │
│ ├──────────────────────────────────────────────────┤       │
│ │ ▶ 结尾话术                          [编辑] [重生成]│       │
│ │ 感谢宝宝们的陪伴，今天的直播就到这里...          │       │
│ └──────────────────────────────────────────────────┘       │
│                                                            │
│ [违规检测] [保存到话术库] [导出话术]                          │
│                                                            │
│ ===== 直播数据 Tab（已结束时显示）=====                      │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│ │ 总观看    │ │ 峰值在线 │ │ 新增粉丝  │ │ 总GMV    │       │
│ │ 12,350人 │ │ 856人    │ │ 123人     │ │ ¥8,500  │       │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│                                                            │
│ [同步数据] [AI分析报告]                                      │
└────────────────────────────────────────────────────────────┘
```

---

## 五、开发任务拆解（Task Breakdown）

### 5.1 后端任务

| # | 任务 | 输出 | 依赖 |
|---|------|------|------|
| B1 | 编写 SQL | schema.sql | 无 |
| B2 | Entity 层 | LiveSession, LiveSessionProduct, LiveSessionScript, LiveSessionData, LiveProductData | B1 |
| B3 | Repository 层 | 5 个 Repository | B2 |
| B4 | VO 层 | SessionSaveVO, SessionVO, SessionProductVO, ScriptVO 等 | B2 |
| B5 | LiveSessionService | 场次 CRUD、选品管理 | B3, B4, douyin |
| B6 | LiveScriptService | 话术生成（开场/产品/转场/结尾/整场）、编辑、违规检测 | B3, B4, ai, script, douyin |
| B7 | LiveDataService | 数据同步（抖音 API）、数据查询 | B3, B4, douyin |
| B8 | LiveAnalysisService | AI 分析报告生成 | B7, ai |
| B9 | LiveSessionController | 场次管理接口 | B5 |
| B10 | LiveScriptController | 话术相关接口 | B6 |
| B11 | LiveDataController | 数据和分析接口 | B7, B8 |
| B12 | resource-data.sql | live 模块资源注册 | B11 |

### 5.2 前端任务

| # | 任务 | 依赖 |
|---|------|------|
| F1 | 场次列表页 | B9 |
| F2 | 场次详情页（含选品/话术/数据三个 Tab） | B9, B10, B11 |
| F3 | 话术编辑页 | B10 |
| F4 | 直播数据看板 | B11 |
| F5 | 数据详情+AI分析 | B11 |
| F6 | 违规词管理页 | script 模块 |
| F7 | 公共违规词管理（管理端） | script 模块 |

---

## 六、测试用例（Test Cases）

### 6.1 冒烟测试

| # | 场景 | 预期 |
|---|------|------|
| S1 | 创建场次 | 200，返回场次 ID |
| S2 | 保存选品 | 200，关联记录创建 |
| S3 | 一键生成整场话术 | 200，返回完整话术流程 |
| S4 | 获取直播数据 | 200，返回数据 |

### 6.2 功能测试

**场次管理：**

| # | 场景 | 预期 |
|---|------|------|
| F01 | 创建场次 | 状态默认 preparing |
| F02 | 场次关联产品 | 产品列表正确关联，排序生效 |
| F03 | 修改产品排序 | 全量覆盖，新排序生效 |
| F04 | 场次状态变更 | preparing → live → ended 单向流转 |

**话术生成：**

| # | 场景 | 预期 |
|---|------|------|
| F05 | 生成开场话术 | 按人设+主题+风格生成 |
| F06 | 生成产品话术 | 按人设+产品信息+风格生成 |
| F07 | 一键生成整场 | 开场→产品1→转场→产品2→...→结尾完整流程 |
| F08 | 不同风格生成 | 亲切/激情/专业风格内容有明显差异 |
| F09 | 话术违规检测 | 检出违规词、标红、给出替换建议 |
| F10 | 引用产品已有话术 | 直接引用 douyin 模块的产品话术 |
| F11 | 手动编辑话术 | 编辑后保存成功 |
| F12 | 保存到话术库 | 话术写入 script 模块话术库 |

**数据分析：**

| # | 场景 | 预期 |
|---|------|------|
| F13 | 同步直播数据 | 数据写入 live_session_data |
| F14 | AI 分析报告 | 返回评级+分析+建议 |
| F15 | 历史数据对比 | 多场直播数据趋势图正确 |

---

## 七、验收标准（Acceptance Criteria）

### 7.1 功能验收

- [ ] 用户可创建/编辑/删除直播场次
- [ ] 用户可从产品库选品并设置上播顺序
- [ ] AI 可按人设+风格生成开场/产品/转场/结尾话术
- [ ] 一键生成整场话术流程正确（按产品排序串联）
- [ ] 话术生成后自动违规检测，标红违规词
- [ ] 用户可手动编辑话术
- [ ] 用户可将话术保存到话术库
- [ ] 直播数据可通过抖音 API 同步
- [ ] AI 可生成直播数据分析报告
- [ ] 公共违规词库管理员可维护
- [ ] 个人违规词库用户可管理

### 7.2 技术验收

- [ ] 一键生成整场话术的多次 AI 调用按顺序执行
- [ ] 违规检测调用 script 模块接口
- [ ] 话术保存到话术库调用 script 模块接口
- [ ] 数据隔离（owner_id）
- [ ] 逻辑删除、事务管理
- [ ] AI 调用有超时和降级处理

---

## 八、上线检查清单（Release Checklist）

- [ ] `sql/live/schema.sql` 已执行
- [ ] `sql/live/resource-data.sql` 已执行
- [ ] ai 模块的直播相关 Prompt 模板已就绪（opening/product/transition/closing/analysis）
- [ ] script 模块的违规检测接口已就绪
- [ ] 抖音开放平台"直播数据"API 权限已申请
- [ ] 对应角色已绑定 live 模块资源
- [ ] 错误码已注册
- [ ] 冒烟测试全部通过
