> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# douyin 模块设计文档

> 版本：1.1 | 更新日期：2026-02-24 | 阶段：P0

---

## 一、需求分析（Requirements Analysis）

### 1.1 模块定位

douyin 模块是核心业务基础，负责抖音账号绑定、人设管理和产品管理。用户注册后默认绑定一个抖音账号，在此基础上创建人设和管理产品。人设是短视频创作和直播话术生成的核心参数，产品是直播带货话术的基础数据。

### 1.2 用户故事（User Stories）

| 编号 | 角色 | 故事 | 验收条件 |
|------|------|------|----------|
| DY-01 | 达人/主播 | 我要能绑定自己的抖音号 | OAuth2 授权绑定、授权成功后自动同步基本信息 |
| DY-02 | 达人/主播 | 我要能看到绑定账号的基本数据 | 粉丝数、获赞数、作品数等基础指标展示 |
| DY-03 | 达人/主播 | 我要能管理账号的授权状态 | 查看授权是否过期、手动刷新授权、解除绑定 |
| DY-04 | 达人/主播 | 我要能创建自己的人设 | 填写定位/风格/标签/目标受众，保存后关联账号 |
| DY-05 | 达人/主播 | 我要能修改和切换人设 | 人设支持多个，可切换当前激活的人设 |
| DY-06 | 达人/主播 | 我要能使用系统预设的人设模板 | 从模板创建人设，修改后保存为自己的 |
| DY-07 | 平台管理员 | 我要能查看所有用户的账号绑定情况 | 账号列表、绑定状态、授权状态 |
| DY-08 | 平台管理员 | 我要能管理人设模板 | 预设模板 CRUD，按行业/类型分类 |
| DY-09 | 达人/主播 | 我要能管理我的产品 | 添加/编辑/删除产品，填写名称/价格/卖点 |
| DY-10 | 达人/主播 | 我要能给产品生成话术 | 选产品 + 人设 → AI 生成产品讲解话术 |
| DY-11 | 达人/主播 | 我要能管理产品的多套话术 | 一个产品可有多套话术（种草/促销/正式） |
| DY-12 | 达人/主播 | 我要能在直播时关联产品 | 产品可被直播场次选用 |

### 1.3 功能清单

```
douyin 模块
├── 账号管理
│   ├── 发起 OAuth2 授权绑定
│   ├── OAuth2 回调处理（获取 access_token/open_id）
│   ├── 同步账号基本信息（头像/昵称/粉丝数/获赞数）
│   ├── 刷新授权 Token（access_token 过期时）
│   ├── 解除账号绑定
│   ├── 查看绑定账号信息
│   └── 账号授权状态检查
│
├── 人设管理
│   ├── 创建人设（定位/风格/标签/受众/简介）
│   ├── 编辑人设
│   ├── 删除人设
│   ├── 切换当前激活人设
│   ├── 人设列表
│   └── 人设详情
│
├── 人设模板（管理员）
│   ├── 模板列表
│   ├── 模板 CRUD
│   └── 模板分类管理
│
├── 产品管理
│   ├── 添加产品（名称/价格/卖点/图片/规格）
│   ├── 编辑产品
│   ├── 删除产品
│   ├── 产品列表（按账号筛选）
│   ├── 产品详情
│   ├── AI 生成产品话术（调用 ai 模块）
│   ├── 产品话术管理（一个产品多套话术）
│   └── 产品分类/标签
│
└── 管理端
    ├── 账号列表（全平台，管理员）
    ├── 账号详情
    └── 数据同步记录
```

### 1.4 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 账号绑定数量限制 | 免费版 1 个、专业版 3 个、企业版无限。绑定时从 sys_config 读取 `ai.quota.xxx.daily` 对应套餐的上限，并查 dy_account 当前绑定数量做校验 |
| BR-02 | OAuth2 授权流程 | 使用抖音开放平台 OAuth2 授权码模式 |
| BR-03 | Token 自动刷新 | access_token 过期前自动用 refresh_token 刷新（定时任务每小时检查） |
| BR-04 | 人设至少一个 | 绑定账号后必须创建至少一个人设才能使用 AI 功能 |
| BR-05 | 当前激活人设 | 用户有多个人设时，有且仅有一个激活人设，AI 生成默认使用激活人设 |
| BR-06 | 人设模板为只读 | 用户从模板创建人设时，复制模板内容，不影响原模板 |
| BR-07 | 解除绑定清理 | 解除绑定时不删除已有的视频/直播数据，但标记为"账号已解绑" |
| BR-08 | 数据隔离 | 用户只能看到自己的账号、人设和产品（owner_id） |
| BR-09 | 产品属于账号 | 产品关联到抖音账号，一个账号下可有多个产品 |
| BR-10 | 产品多套话术 | 一个产品可有多套话术（种草/促销/正式等），按风格区分 |
| BR-11 | 产品话术 AI 生成 | 生成话术时必须选择人设和风格，调用 ai 模块生成 |
| BR-12 | 人设删除保护 | 删除人设时：1) 若为唯一人设，拒绝删除；2) 若为激活人设，自动激活最早创建的其他人设；3) 已关联的 sv_plan / live_session 中的 persona_id 保留历史值不清空 |
| BR-13 | 行业分类关联 | dy_account.industry_id 关联 sys_industry.id，行业被删除时置为 NULL（ON DELETE SET NULL） |

### 1.5 模块依赖

```
依赖关系：
common (config/vo/util) ← auth ← douyin
config ← douyin（行业分类 sys_industry）
                                    ↑
              shortvideo, live 都依赖 douyin（账号/人设/产品）
              ai ← douyin（产品话术生成）
```

---

## 二、数据库设计（Database Design）

### 2.1 ER 关系

```
auth_user ──(owner_id)──→ dy_account (一对多，按套餐限制)
                              │
                         dy_account_data (一对一，账号数据快照)
                              │
                         dy_product (一对多，账号下的产品)
                              │
                         dy_product_script (一对多，产品的话术)

auth_user ──(owner_id)──→ dy_persona (一对多)
                              │
                         dy_persona 关联 dy_account (通过 account_id)

dy_persona_template (系统模板，管理员维护)
```

### 2.2 表结构

#### dy_account — 抖音账号表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID（auth_user.id） |
| open_id | VARCHAR(256) | NOT NULL | — | 抖音 open_id |
| union_id | VARCHAR(256) | | — | 抖音 union_id |
| nickname | VARCHAR(128) | | — | 抖音昵称 |
| avatar | VARCHAR(512) | | — | 抖音头像 URL |
| douyin_id | VARCHAR(128) | | — | 抖音号（可能为空） |
| industry_id | BIGINT | | — | 行业分类 ID（关联 sys_industry.id） |
| account_desc | VARCHAR(512) | | — | 账号简介 |
| access_token | VARCHAR(512) | | — | OAuth2 access_token |
| refresh_token | VARCHAR(512) | | — | OAuth2 refresh_token |
| token_expire_time | TIMESTAMP | | — | access_token 过期时间 |
| refresh_expire_time | TIMESTAMP | | — | refresh_token 过期时间 |
| auth_status | INTEGER | NOT NULL | 1 | 授权状态：1=正常 0=过期 2=已解绑 |
| last_sync_time | TIMESTAMP | | — | 最后同步时间 |
| status | INTEGER | NOT NULL | 1 | 状态：1=正常 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(owner_id, open_id) WHERE deleted=0`、`(owner_id) WHERE deleted=0`、`(industry_id) WHERE deleted=0`

#### dy_account_data — 账号数据快照表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| account_id | BIGINT | NOT NULL | — | 关联 dy_account.id |
| follower_count | BIGINT | | 0 | 粉丝数 |
| following_count | BIGINT | | 0 | 关注数 |
| total_favorited | BIGINT | | 0 | 总获赞数 |
| video_count | BIGINT | | 0 | 作品数 |
| snapshot_date | DATE | NOT NULL | — | 快照日期（每天一条） |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(account_id, snapshot_date)`、`(account_id, snapshot_date DESC)`

#### dy_persona — 人设表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| account_id | BIGINT | | — | 关联的抖音账号 ID（可空，通用人设） |
| persona_name | VARCHAR(64) | NOT NULL | — | 人设名称 |
| positioning | VARCHAR(256) | | — | 定位（如"美妆博主""健身达人"） |
| style | VARCHAR(128) | | — | 风格（如"专业""幽默""亲切"） |
| tags | VARCHAR(512) | | — | 标签（JSON 数组，如 ["美妆","护肤","平价"]） |
| target_audience | VARCHAR(256) | | — | 目标受众（如"18-30岁女性"） |
| tone | VARCHAR(128) | | — | 语气/口吻（如"闺蜜聊天""专家讲解"） |
| description | TEXT | | — | 人设详细描述（自由文本，给 AI 的 context） |
| is_active | INTEGER | NOT NULL | 0 | 是否当前激活：1=是 0=否 |
| sort_order | INTEGER | | 0 | 排序 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(owner_id) WHERE deleted=0`、`(owner_id, is_active) WHERE deleted=0`

#### dy_persona_template — 人设模板表（管理员维护）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| template_name | VARCHAR(64) | NOT NULL | — | 模板名称 |
| category | VARCHAR(64) | | — | 分类（对应行业，如美妆/健身/美食/教育/电商，建议关联 sys_industry） |
| positioning | VARCHAR(256) | | — | 定位 |
| style | VARCHAR(128) | | — | 风格 |
| tags | VARCHAR(512) | | — | 标签（JSON 数组） |
| target_audience | VARCHAR(256) | | — | 目标受众 |
| tone | VARCHAR(128) | | — | 语气/口吻 |
| description | TEXT | | — | 详细描述 |
| sort_order | INTEGER | | 0 | 排序 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

#### dy_product — 产品表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| account_id | BIGINT | NOT NULL | — | 关联的抖音账号 ID |
| product_name | VARCHAR(128) | NOT NULL | — | 产品名称 |
| product_image | VARCHAR(512) | | — | 产品主图 URL |
| price | DECIMAL(10,2) | | — | 价格 |
| original_price | DECIMAL(10,2) | | — | 原价（用于锚定对比） |
| category | VARCHAR(64) | | — | 产品分类 |
| selling_points | TEXT | | — | 核心卖点（JSON 数组或多行文本） |
| specifications | VARCHAR(512) | | — | 规格信息 |
| usage_scenario | VARCHAR(512) | | — | 使用场景 |
| target_audience | VARCHAR(256) | | — | 目标人群 |
| description | TEXT | | — | 产品详细描述 |
| sort_order | INTEGER | | 0 | 排序 |
| status | INTEGER | NOT NULL | 1 | 1=正常 0=下架 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(owner_id, account_id, status) WHERE deleted=0`

#### dy_product_script — 产品话术表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| product_id | BIGINT | NOT NULL | — | 关联产品 ID |
| persona_id | BIGINT | | — | 生成时使用的人设 ID |
| script_name | VARCHAR(128) | NOT NULL | — | 话术名称（如"种草版""促销版"） |
| script_style | VARCHAR(64) | | — | 风格：种草 / 促销 / 专业讲解 / 亲切推荐 |
| content | TEXT | NOT NULL | — | 话术内容 |
| duration_hint | VARCHAR(32) | | — | 建议时长（如"3-5分钟"） |
| is_ai_generated | INTEGER | NOT NULL | 0 | 是否 AI 生成：1=是 0=手动编写 |
| sort_order | INTEGER | | 0 | 排序 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(product_id) WHERE deleted=0`、`(owner_id) WHERE deleted=0`

```sql
-- 人设模板（预设）
INSERT INTO dy_persona_template (template_name, category, positioning, style, tags, target_audience, tone, description) VALUES
('美妆博主', '美妆', '平价美妆分享', '亲切', '["美妆","护肤","平价","测评"]', '18-30岁女性', '闺蜜聊天', '分享平价好物，真实测评，像闺蜜一样推荐适合的美妆产品'),
('健身达人', '健身', '家庭健身教练', '专业', '["健身","减脂","塑形","居家运动"]', '20-40岁男女', '教练指导', '专业但不枯燥，用简单动作帮助大家在家就能练出好身材'),
('美食探店', '美食', '本地美食探店', '幽默', '["美食","探店","本地生活","吃货"]', '18-35岁', '吃货分享', '用夸张有趣的方式分享本地美食，让人看了就想去'),
('电商带货', '电商', '好物推荐官', '激情', '["好物","性价比","推荐","种草"]', '25-45岁', '促销讲解', '用数据和对比说服消费者，突出性价比和使用场景'),
('知识博主', '教育', '行业知识分享', '专业', '["干货","知识","行业","涨知识"]', '22-40岁', '专家讲解', '把专业知识讲得通俗易懂，有深度但不难懂');
```

---

## 三、接口设计（API Design）

### 3.1 接口总表

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 1 | GET | /api/v1/douyin/oauth/authorize | 登录 | 发起 OAuth2 授权（重定向到抖音） |
| 2 | GET | /api/v1/douyin/oauth/callback | 公开 | OAuth2 回调处理 |
| 3 | POST | /api/v1/douyin/account/info | 登录 | 获取当前用户的绑定账号信息 |
| 4 | POST | /api/v1/douyin/account/refresh | 登录 | 手动刷新授权 Token |
| 5 | POST | /api/v1/douyin/account/unbind | 登录 | 解除账号绑定 |
| 6 | POST | /api/v1/douyin/account/sync | 登录 | 手动触发数据同步 |
| 7 | POST | /api/v1/douyin/account/data | 登录 | 获取账号数据趋势 |
| 8 | POST | /api/v1/douyin/persona/list | 登录 | 人设列表 |
| 9 | POST | /api/v1/douyin/persona/get | 登录 | 人设详情 |
| 10 | POST | /api/v1/douyin/persona/save | 登录 | 新增/编辑人设 |
| 11 | POST | /api/v1/douyin/persona/delete | 登录 | 删除人设 |
| 12 | POST | /api/v1/douyin/persona/activate | 登录 | 切换激活人设 |
| 13 | POST | /api/v1/douyin/persona/active | 登录 | 获取当前激活人设 |
| 14 | POST | /api/v1/douyin/persona/template/list | 登录 | 人设模板列表 |
| 15 | POST | /api/v1/douyin/persona/template/use | 登录 | 使用模板创建人设 |
| 16 | POST | /api/v1/douyin/admin/account/list | 管理员 | 全平台账号列表 |
| 17 | POST | /api/v1/douyin/admin/template/list | 管理员 | 模板列表（分页） |
| 18 | POST | /api/v1/douyin/admin/template/save | 管理员 | 新增/编辑模板 |
| 19 | POST | /api/v1/douyin/admin/template/delete | 管理员 | 删除模板 |
| **产品管理** | | | | |
| 20 | POST | /api/v1/douyin/product/list | 登录 | 我的产品列表 |
| 21 | POST | /api/v1/douyin/product/get | 登录 | 产品详情 |
| 22 | POST | /api/v1/douyin/product/save | 登录 | 新增/编辑产品 |
| 23 | POST | /api/v1/douyin/product/delete | 登录 | 删除产品 |
| 24 | POST | /api/v1/douyin/product/script/list | 登录 | 产品话术列表 |
| 25 | POST | /api/v1/douyin/product/script/save | 登录 | 新增/编辑产品话术 |
| 26 | POST | /api/v1/douyin/product/script/delete | 登录 | 删除产品话术 |
| 27 | POST | /api/v1/douyin/product/script/generate | 登录 | AI 生成产品话术 |

### 3.2 关键接口详情

#### 接口 1：发起 OAuth2 授权

```
GET /api/v1/douyin/oauth/authorize
权限：登录
```

**业务逻辑：**
1. 生成 state 参数（UUID），存入 session/缓存，关联当前 userId
2. 构建抖音授权 URL：`https://open.douyin.com/platform/oauth/connect?client_key={appId}&response_type=code&scope=user_info&redirect_uri={callbackUrl}&state={state}`
3. 重定向到抖音授权页

**响应：** 302 重定向

---

#### 接口 2：OAuth2 回调

```
GET /api/v1/douyin/oauth/callback?code={code}&state={state}
权限：公开
```

**业务逻辑：**
1. 验证 state 参数，获取 userId
2. 用 code 换取 access_token（POST `https://open.douyin.com/oauth/access_token/`）
3. 用 access_token 获取用户信息（`/oauth/userinfo/`）
4. 创建/更新 dy_account 记录
5. 同步账号基本数据（粉丝数等）
6. 重定向到前端绑定成功页面

**错误处理：** 授权失败重定向到失败页面并携带错误信息

---

#### 接口 10：新增/编辑人设

```
POST /api/v1/douyin/persona/save
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 有值为编辑，无值为新增 |
| accountId | Long | 否 | 关联的账号 ID |
| personaName | String | 是 | 人设名称，最长 64 |
| positioning | String | 否 | 定位 |
| style | String | 否 | 风格 |
| tags | List\<String\> | 否 | 标签列表 |
| targetAudience | String | 否 | 目标受众 |
| tone | String | 否 | 语气/口吻 |
| description | String | 否 | 详细描述 |

**响应：**

```json
{
  "status": 200,
  "message": "添加成功!",
  "data": 1
}
```

**业务逻辑：**
1. 校验 personaName 不为空
2. 新增时：检查用户的人设数量（后续可按套餐限制）
3. 编辑时：校验 owner_id 必须是当前用户
4. tags 存储为 JSON 字符串
5. 如果是用户的第一个人设，自动设为激活状态

---

#### 接口 12：切换激活人设

```
POST /api/v1/douyin/persona/activate
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 人设 ID |

**业务逻辑：**
1. 校验人设属于当前用户
2. 将当前用户所有人设的 is_active 设为 0
3. 将目标人设的 is_active 设为 1
4. 整个操作在一个事务中

---

#### 接口 22：新增/编辑产品

```
POST /api/v1/douyin/product/save
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 有值为编辑，无值为新增 |
| accountId | Long | 是 | 关联的抖音账号 ID |
| productName | String | 是 | 产品名称 |
| productImage | String | 否 | 产品主图 URL |
| price | BigDecimal | 否 | 价格 |
| originalPrice | BigDecimal | 否 | 原价 |
| category | String | 否 | 分类 |
| sellingPoints | String | 否 | 核心卖点 |
| specifications | String | 否 | 规格 |
| usageScenario | String | 否 | 使用场景 |
| targetAudience | String | 否 | 目标人群 |
| description | String | 否 | 详细描述 |

---

#### 接口 27：AI 生成产品话术

```
POST /api/v1/douyin/product/script/generate
权限：登录（检查 AI 额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | Long | 是 | 产品 ID |
| personaId | Long | 否 | 人设 ID（不填则使用当前激活人设） |
| style | String | 是 | 话术风格：种草 / 促销 / 专业讲解 / 亲切推荐 |

**业务逻辑：**
1. 获取产品信息（名称/价格/卖点/场景等）
2. 获取人设信息
3. 调用 ai 模块 `live_script_product` 模板，传入产品 + 人设 + 风格
4. 生成结果写入 dy_product_script
5. 返回生成的话术内容

**响应：**

```json
{
  "status": 200,
  "data": {
    "scriptId": 1,
    "content": "【AI 生成的产品话术】...",
    "quotaRemaining": 7
  }
}
```（Frontend Design）

### 4.1 页面清单

| # | 页面 | 路径 | 入口 | 说明 |
|---|------|------|------|------|
| 1 | 账号绑定页 | /talent/douyin/bind | 工作台引导/侧栏 | 绑定流程引导 |
| 2 | 账号详情页 | /talent/douyin/account | 侧栏菜单 | 账号信息 + 数据 |
| 3 | 人设管理页 | /talent/douyin/persona | 侧栏菜单 | 人设列表 + 编辑 |
| 4 | 产品管理页 | /talent/douyin/product | 侧栏菜单 | 产品列表 + 编辑 + 话术 |
| 5 | 机构账号总览 | /org/douyin/accounts | 机构侧栏 | 旗下达人账号列表（DataScope） |
| 6 | 机构人设/产品查看 | /org/douyin/overview | 机构侧栏 | 旗下达人人设与产品只读查看 |
| 7 | 账号管理（管理端） | /admin/douyin/accounts | 管理端菜单 | 全平台账号列表 |
| 8 | 模板管理（管理端） | /admin/douyin/templates | 管理端菜单 | 人设模板管理 |

### 4.2 页面详细设计

#### 页面 1：账号绑定页

```
┌────────────────────────────────────────┐
│ 绑定抖音账号                            │
├────────────────────────────────────────┤
│                                        │
│  ┌──────────────────────────────────┐  │
│  │  还未绑定抖音账号                  │  │
│  │                                  │  │
│  │  绑定后可以：                      │  │
│  │  ✓ 同步视频数据和直播数据          │  │
│  │  ✓ AI 根据人设生成专属内容         │  │
│  │  ✓ 数据分析和改进建议              │  │
│  │                                  │  │
│  │     [ 授权绑定抖音号 ]            │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ── 或已绑定状态 ──                     │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │  [头像] 张三的抖音号               │  │
│  │  抖音号: zhangsan123              │  │
│  │  粉丝: 12.3万  获赞: 56.8万       │  │
│  │  授权状态: ✓ 正常                  │  │
│  │  最后同步: 2026-02-24 10:00       │  │
│  │                                  │  │
│  │  [刷新授权] [同步数据] [解除绑定]  │  │
│  └──────────────────────────────────┘  │
└────────────────────────────────────────┘
```

#### 页面 3：人设管理页

```
┌────────────────────────────────────────────────────┐
│ 我的人设                         [从模板创建] [新建] │
├────────────────────────────────────────────────────┤
│                                                    │
│  ┌─ 当前激活 ─────────────────────────────────┐    │
│  │ 美妆博主小美        [编辑] [切换]           │    │
│  │ 定位: 平价美妆分享   风格: 亲切              │    │
│  │ 标签: 美妆 护肤 平价 测评                    │    │
│  │ 受众: 18-30岁女性                           │    │
│  └─────────────────────────────────────────────┘   │
│                                                    │
│  ┌─────────────────────────────────────────────┐   │
│  │ 健身教练版         [编辑] [激活] [删除]      │   │
│  │ 定位: 家庭健身教练  风格: 专业               │   │
│  │ 标签: 健身 减脂 居家运动                     │   │
│  └─────────────────────────────────────────────┘   │
│                                                    │
│  ── 模板选择弹窗 ──                                 │
│  ┌──────────────────────────────────────────┐      │
│  │ 选择人设模板          [搜索___]           │      │
│  │                                          │      │
│  │ 美妆博主  健身达人  美食探店  电商带货     │      │
│  │ 知识博主  ...                             │      │
│  │                                          │      │
│  │           [使用此模板]  [取消]            │      │
│  └──────────────────────────────────────────┘      │
└────────────────────────────────────────────────────┘
```

- 编辑使用 Offcanvas 侧滑窗
- 模板选择使用 Modal 弹窗

#### 页面 4：产品管理页

```
┌────────────────────────────────────────────────────────┐
│ 我的产品                              [添加产品]       │
├────────────────────────────────────────────────────────┤
│ 账号: [张三的抖音号 ▼]  分类: [全部 ▼]  [搜索___]     │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ [图] 秋冬保湿面霜           ¥89（原价¥159）      │  │
│  │     卖点: 24h保湿、敏感肌可用、成分安全          │  │
│  │     话术: 3套  [编辑] [管理话术] [删除]          │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ [图] 玻尿酸精华液           ¥69（原价¥128）      │  │
│  │     卖点: 补水、抗老、医美同款                    │  │
│  │     话术: 1套  [编辑] [管理话术] [删除]          │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ── 话术管理弹窗（点击"管理话术"打开）──               │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 秋冬保湿面霜 - 话术管理      [AI生成新话术]      │  │
│  │                                                  │  │
│  │ ┌─ 种草版（AI生成）────────────────────────┐    │  │
│  │ │ 姐妹们这款面霜真的绝了！我自己用了...    │    │  │
│  │ │ 建议时长: 3-5分钟         [编辑] [删除]  │    │  │
│  │ └──────────────────────────────────────────┘    │  │
│  │                                                  │  │
│  │ ┌─ 促销版（AI生成）────────────────────────┐    │  │
│  │ │ 倒计时开始！原价159的面霜，今天直播间...  │    │  │
│  │ │ 建议时长: 2-3分钟         [编辑] [删除]  │    │  │
│  │ └──────────────────────────────────────────┘    │  │
│  │                                                  │  │
│  │ AI生成选项: 人设[美妆博主 ▼] 风格[种草 ▼]        │  │
│  │              [生成话术]                           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

- 产品编辑使用 Offcanvas 侧滑窗
- 话术管理使用 Modal 弹窗
- AI 生成话术需选择人设和风格

---

## 五、开发任务拆解（Task Breakdown）

### 5.1 后端任务

| # | 任务 | 输出 | 依赖 |
|---|------|------|------|
| B1 | 编写 SQL | schema.sql + demo.sql | 无 |
| B2 | Entity 层 | DyAccount, DyAccountData, DyPersona, DyPersonaTemplate, DyProduct, DyProductScript | B1 |
| B3 | Repository 层 | 6 个 Repository 接口 | B2 |
| B4 | VO 层 | AccountVO, PersonaSaveVO, PersonaVO, PersonaTemplateVO, ProductSaveVO, ProductVO, ProductScriptSaveVO 等 | B2 |
| B5 | DyAccountService | OAuth2 绑定、Token 刷新、数据同步、解绑 | B3, B4 |
| B6 | DyPersonaService | 人设 CRUD、激活切换 | B3, B4 |
| B7 | DyPersonaTemplateService | 模板 CRUD（管理员） | B3, B4 |
| B7.5 | DyProductService | 产品 CRUD | B3, B4 |
| B7.6 | DyProductScriptService | 产品话术 CRUD、AI 生成话术 | B3, B4, ai |
| B8 | DouyinOAuthController | OAuth2 授权/回调 | B5 |
| B9 | DyAccountController | 账号信息/刷新/解绑/同步 | B5 |
| B10 | DyPersonaController | 人设 CRUD + 模板使用 | B6, B7 |
| B10.5 | DyProductController | 产品 CRUD + 话术管理 + AI 生成 | B7.5, B7.6 |
| B11 | DyAdminController | 管理端：账号列表、模板管理 | B5, B7 |
| B12 | resource-data.sql | douyin 模块菜单/API/按钮资源注册 | B11 |

### 5.2 前端任务

| # | 任务 | 依赖 |
|---|------|------|
| F1 | 账号绑定页 | B8, B9 |
| F2 | 账号详情页 | B9 |
| F3 | 人设管理页 | B10 |
| F3.5 | 产品管理页 | B10.5 |
| F4 | 账号管理（管理端） | B11 |
| F5 | 模板管理（管理端） | B11 |

---

## 六、测试用例（Test Cases）

### 6.1 冒烟测试

| # | 场景 | 操作 | 预期 |
|---|------|------|------|
| S1 | 发起授权 | GET /douyin/oauth/authorize | 302 重定向到抖音 |
| S2 | 人设列表 | POST /douyin/persona/list | 200，返回当前用户人设 |
| S3 | 创建人设 | POST /douyin/persona/save | 200，返回人设 ID |
| S4 | 模板列表 | POST /douyin/persona/template/list | 200，返回模板 |
| S5 | 产品列表 | POST /douyin/product/list | 200，返回用户产品 |
| S6 | AI 生成产品话术 | POST /douyin/product/script/generate | 200，返回话术内容 |

### 6.2 功能测试

**账号绑定：**

| # | 场景 | 预期 |
|---|------|------|
| F01 | OAuth2 回调成功 | 创建 dy_account 记录，同步基本信息 |
| F02 | 重复绑定同一 open_id | 更新已有记录，不重复创建 |
| F03 | 刷新 Token | access_token 更新，过期时间刷新 |
| F04 | Token 过期后刷新失败 | auth_status 变为 0，提示重新授权 |
| F05 | 解除绑定 | auth_status 变为 2，不影响已有数据 |

**人设管理：**

| # | 场景 | 预期 |
|---|------|------|
| F06 | 创建第一个人设 | 自动设为激活状态 |
| F07 | 创建第二个人设 | 不自动激活，保持第一个为激活 |
| F08 | 切换激活人设 | 旧的变为非激活，新的变为激活 |
| F09 | 删除激活人设 | 如有其他人设，自动激活排序最前的 |
| F10 | 从模板创建人设 | 复制模板内容，生成独立的人设记录 |
| F11 | 编辑他人的人设 | 2002 权限拒绝 |

**产品管理：**

| # | 场景 | 预期 |
|---|------|------|
| F12 | 添加产品 | 产品创建成功，关联到账号 |
| F13 | AI 生成产品话术 | 按人设+风格生成话术，写入 dy_product_script |
| F14 | 同一产品生成多套话术 | 每次生成独立记录，不覆盖旧话术 |
| F15 | 删除产品 | 逻辑删除产品，关联话术也逻辑删除 |
| F16 | 编辑他人的产品 | 2002 权限拒绝 |

### 6.3 边界测试

| # | 场景 | 预期 |
|---|------|------|
| E01 | personaName 超过 64 字符 | 1001 参数校验失败 |
| E02 | tags 数组为空 | 正常保存，tags 为 null |
| E03 | 未绑定账号时创建人设 | 正常（accountId 可空） |
| E04 | 免费用户绑定第二个账号 | 提示升级套餐（后续实现） |

---

## 七、验收标准（Acceptance Criteria）

### 7.1 功能验收

- [ ] 用户可通过 OAuth2 绑定抖音账号
- [ ] 绑定后自动同步头像/昵称/粉丝数等基本信息
- [ ] 用户可查看账号基本信息和授权状态
- [ ] 用户可手动刷新授权 Token
- [ ] 用户可解除账号绑定
- [ ] 用户可创建/编辑/删除人设
- [ ] 第一个人设自动激活
- [ ] 用户可切换激活人设
- [ ] 用户可从系统模板创建人设
- [ ] 管理员可查看全平台账号列表
- [ ] 管理员可管理人设模板
- [ ] 用户可添加/编辑/删除产品
- [ ] 用户可为产品 AI 生成多套话术（按风格）
- [ ] 产品话术支持手动编辑和 AI 生成

### 7.2 技术验收

- [ ] OAuth2 Token 安全存储（数据库加密或独立存储）
- [ ] 数据隔离（owner_id 校验）
- [ ] Entity 有 @Where(clause="deleted=0")
- [ ] 写操作有 @Transactional
- [ ] 接口返回 RESTResult 格式

---

## 八、上线检查清单（Release Checklist）

- [ ] `sql/douyin/schema.sql` 已执行
- [ ] `sql/douyin/demo.sql` 已执行（预设人设模板）
- [ ] `sql/douyin/resource-data.sql` 已执行
- [ ] 抖音开放平台应用已申请并通过审核
- [ ] OAuth2 回调 URL 已配置（抖音开放平台 + application.yml）
- [ ] 对应角色已绑定 douyin 模块资源
- [ ] 错误码 3101-3104 已在 ErrorCode.java 中
- [ ] 冒烟测试 S1-S6 全部通过
