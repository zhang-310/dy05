> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# config 模块设计文档

> 版本：2.1 | 更新日期：2026-02-24 | 阶段：P0

---

## 一、需求分析

### 1.1 模块定位

config 模块是全平台的配置中心，提供**分组层级配置管理**和**行业分类管理**。支持运行时动态修改配置，无需重启服务。

**核心场景：** AI 模型接口众多（知识库嵌入、文案生成、话术生成、图像生成、视频生成、TTS 等），每个接口有不同的 API 地址、密钥、参数，且需要支持主备切换。通过分组配置可以直观管理这些配置项。

### 1.2 功能清单

```
config 模块
├── 配置分组管理（管理员）
│   ├── 分组树形展示（支持 2 级层级）
│   ├── 分组 CRUD（名称/编码/图标/描述/排序）
│   └── 分组内配置项管理
│
├── 配置项管理（管理员）
│   ├── 配置项 CRUD（在分组内新增/编辑/删除配置项）
│   ├── 配置值类型（string / number / boolean / json / password）
│   ├── 敏感值脱敏展示（API Key 等）
│   ├── 配置项搜索（跨分组全局搜索）
│   └── 配置导入/导出
│
├── 行业分类管理（管理员）
│   ├── 行业列表（树形，支持父子层级）
│   ├── 行业 CRUD
│   └── 行业排序
│
└── 配置查询（系统内部）
    ├── 按 config_key 获取配置值
    ├── 按分组编码获取批量配置
    └── 配置缓存（内存缓存，修改时立即刷新）
```

### 1.3 预设分组结构

```
系统配置
├── 基本信息 (system_basic)      — 系统名称、版本、Logo
├── 安全设置 (system_auth)       — Token 有效期、密码策略
└── 通知设置 (system_notify)     — 邮件/短信通知配置

AI 配置
├── 额度设置 (ai_quota)          — 免费/专业/企业版每日调用次数
├── 知识库设置 (ai_knowledge)    — Milvus/ES 地址、嵌入参数、检索参数
└── 进化引擎设置 (ai_evolve)     — 采样数、质量阈值、主题池上限、调度周期

存储配置
├── 对象存储 (storage_oss)       — BOS/OSS 配置（Endpoint/Bucket/AK/SK）
├── 上传限制 (storage_upload)    — 文件大小限制、允许类型
└── CDN 配置 (storage_cdn)       — CDN 域名

抖音接口
├── OAuth2 配置 (douyin_oauth)   — Client ID/Secret、回调地址
└── 开放平台 (douyin_api)        — API 地址、调用限制
```

> **注意：** AI 模型的具体接口配置（哪个任务用哪个模型）不在 sys_config 中，而是通过 ai 模块的 `ai_model_config` + `ai_task_model_config` 两张表专门管理，因为模型配置比较复杂，需要独立的管理页面。

### 1.4 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | config_key 全局唯一 | 键名不可重复 |
| BR-02 | 分组层级 | 支持 2 级分组（大类 → 子分组），如「AI 配置 → 额度设置」|
| BR-03 | 分组编码唯一 | group_code 全局唯一，用于代码中按分组查配置 |
| BR-04 | 缓存策略 | 配置加载到 ConcurrentHashMap 内存缓存，修改时调用 `configCache.put(key, value)` 立即刷新。若刷新失败，记录错误日志并抛 CONFIG_SAVE_FAIL(3602)，保证缓存与数据库一致 |
| BR-05 | 敏感值 | config_type=password 的配置项，页面展示时脱敏（显示 ******） |
| BR-06 | 行业分类全局共用 | 行业数据被 douyin、script、shortvideo 等模块引用 |
| BR-07 | 行业分类支持层级 | 支持一级行业和二级行业（如 美妆→护肤/彩妆/美发） |
| BR-08 | 配置项归组 | 每个配置项必须属于一个分组，不允许无归属配置 |
| BR-09 | 预设分组不可删除 | 系统预设的分组（is_system=1）不允许删除，但可编辑名称和排序 |

---

## 二、数据库设计

### 2.1 ER 关系

```
sys_config_group (配置分组，树形)
    └── sys_config (配置项，属于某个分组)

sys_industry (行业分类，树形)
```

#### sys_config_group — 配置分组表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| parent_id | BIGINT | | 0 | 父分组 ID（0=一级分组） |
| group_code | VARCHAR(64) | NOT NULL | — | 分组编码（唯一，如 ai_quota） |
| group_name | VARCHAR(64) | NOT NULL | — | 分组名称 |
| icon | VARCHAR(64) | | — | 图标 CSS 类名（如 fa fa-cog） |
| description | VARCHAR(256) | | — | 分组说明 |
| sort_order | INTEGER | | 0 | 排序 |
| is_system | INTEGER | NOT NULL | 0 | 是否系统预设（1=不可删除） |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(group_code) WHERE deleted=0`、`(parent_id, sort_order) WHERE deleted=0`

#### sys_config — 系统配置表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| group_id | BIGINT | NOT NULL | — | 所属分组 ID（关联 sys_config_group.id） |
| config_key | VARCHAR(128) | NOT NULL | — | 配置键（唯一） |
| config_value | TEXT | | — | 配置值 |
| config_type | VARCHAR(16) | | 'string' | 值类型：string / number / boolean / json / password |
| label | VARCHAR(128) | | — | 配置项显示名称（中文，如「Token 有效期」） |
| placeholder | VARCHAR(256) | | — | 输入提示（如「请输入小时数」） |
| description | VARCHAR(256) | | — | 配置说明 |
| sort_order | INTEGER | | 0 | 同组内排序 |
| is_required | INTEGER | NOT NULL | 0 | 是否必填（1=必填） |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(config_key) WHERE deleted=0`、`(group_id, sort_order) WHERE deleted=0`

#### sys_industry — 行业分类表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| parent_id | BIGINT | | 0 | 父行业 ID（0=一级行业） |
| industry_name | VARCHAR(64) | NOT NULL | — | 行业名称 |
| industry_code | VARCHAR(32) | NOT NULL | — | 行业编码（唯一） |
| icon | VARCHAR(128) | | — | 行业图标 |
| sort_order | INTEGER | | 0 | 排序 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(industry_code) WHERE deleted=0`、`(parent_id, sort_order) WHERE deleted=0`

### 初始数据

```sql
-- ==============================
-- 配置分组（2 级层级）
-- ==============================
-- 一级分组
INSERT INTO sys_config_group (parent_id, group_code, group_name, icon, description, sort_order, is_system) VALUES
(0, 'system',  '系统配置',  'fa fa-cog',     '系统基本设置',      1, 1),
(0, 'ai',      'AI 配置',   'fa fa-brain',   'AI 能力相关配置',    2, 1),
(0, 'storage',  '存储配置',  'fa fa-cloud',   '文件存储相关配置',   3, 1),
(0, 'douyin',   '抖音接口',  'fa fa-video-camera', '抖音开放平台配置', 4, 1);

-- 二级分组（system 下）
INSERT INTO sys_config_group (parent_id, group_code, group_name, description, sort_order, is_system) VALUES
((SELECT id FROM sys_config_group WHERE group_code='system'), 'system_basic',  '基本信息',  '系统名称、版本等', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='system'), 'system_auth',   '安全设置',  'Token、密码策略',  2, 1),
((SELECT id FROM sys_config_group WHERE group_code='system'), 'system_notify', '通知设置',  '邮件短信通知',     3, 0);

-- 二级分组（ai 下）
INSERT INTO sys_config_group (parent_id, group_code, group_name, description, sort_order, is_system) VALUES
((SELECT id FROM sys_config_group WHERE group_code='ai'), 'ai_quota',     '额度设置',      'AI 调用次数限制',       1, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai'), 'ai_knowledge', '知识库设置',    'Milvus/ES、嵌入参数',    2, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai'), 'ai_evolve',    '进化引擎设置',  '自进化调度与参数',      3, 1);

-- 二级分组（storage 下）
INSERT INTO sys_config_group (parent_id, group_code, group_name, description, sort_order, is_system) VALUES
((SELECT id FROM sys_config_group WHERE group_code='storage'), 'storage_oss',    '对象存储',  'BOS/OSS 配置',   1, 1),
((SELECT id FROM sys_config_group WHERE group_code='storage'), 'storage_upload', '上传限制',  '文件大小和类型', 2, 1);

-- 二级分组（douyin 下）
INSERT INTO sys_config_group (parent_id, group_code, group_name, description, sort_order, is_system) VALUES
((SELECT id FROM sys_config_group WHERE group_code='douyin'), 'douyin_oauth', 'OAuth2 配置', '抖音授权登录',     1, 1),
((SELECT id FROM sys_config_group WHERE group_code='douyin'), 'douyin_api',   '开放平台',    'API 地址和限制',   2, 1);

-- ==============================
-- 配置项（按分组归属）
-- ==============================

-- 基本信息
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order) VALUES
((SELECT id FROM sys_config_group WHERE group_code='system_basic'), 'system.name', '抖音AI运营系统', 'string', '系统名称', '显示在页面标题和 Logo 旁', 1),
((SELECT id FROM sys_config_group WHERE group_code='system_basic'), 'system.version', '1.0.0', 'string', '系统版本', '当前系统版本号', 2),
((SELECT id FROM sys_config_group WHERE group_code='system_basic'), 'system.logo.url', '', 'string', '系统 Logo', 'Logo 图片 URL', 3);

-- 安全设置
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order, is_required) VALUES
((SELECT id FROM sys_config_group WHERE group_code='system_auth'), 'auth.token.expire.hours', '24', 'number', 'Token 有效期（小时）', '用户登录 Token 的过期时间', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='system_auth'), 'auth.password.min.length', '6', 'number', '密码最短长度', '用户注册/修改密码的最短长度要求', 2, 1);

-- AI 额度设置
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order, is_required) VALUES
((SELECT id FROM sys_config_group WHERE group_code='ai_quota'), 'ai.quota.free.daily', '10', 'number', '免费版每日次数', '免费用户每日 AI 调用次数上限', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai_quota'), 'ai.quota.pro.daily', '-1', 'number', '专业版每日次数', '-1 表示无限制', 2, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai_quota'), 'ai.quota.enterprise.daily', '-1', 'number', '企业版每日次数', '-1 表示无限制', 3, 1);

-- AI 知识库设置
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, placeholder, description, sort_order, is_required) VALUES
((SELECT id FROM sys_config_group WHERE group_code='ai_knowledge'), 'ai.milvus.host', 'localhost', 'string', 'Milvus 地址', 'hostname', 'Milvus 向量数据库连接地址', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai_knowledge'), 'ai.milvus.port', '19530', 'number', 'Milvus 端口', '默认 19530', 'Milvus 向量数据库端口', 2, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai_knowledge'), 'ai.knowledge.top_k', '10', 'number', '检索返回条数', '默认 10', '语义检索默认返回的结果条数', 3, 0),
((SELECT id FROM sys_config_group WHERE group_code='ai_knowledge'), 'ai.knowledge.cache.ttl', '3600', 'number', '检索缓存 TTL（秒）', '默认 3600', '相同查询的缓存有效期', 4, 0),
((SELECT id FROM sys_config_group WHERE group_code='ai_knowledge'), 'ai.knowledge.chunk.max_chars', '800', 'number', '分块最大字符数', '默认 800', '知识文本分块的最大字符数', 5, 0),
((SELECT id FROM sys_config_group WHERE group_code='ai_knowledge'), 'ai.knowledge.chunk.overlap', '120', 'number', '分块重叠字符数', '默认 120', '相邻分块的重叠字符数', 6, 0);

-- AI 进化引擎设置
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order, is_required) VALUES
((SELECT id FROM sys_config_group WHERE group_code='ai_evolve'), 'ai.evolve.cron', '0 0 6 * * ?', 'string', '进化调度 Cron', '定时进化任务的 Cron 表达式（默认每天 6:00）', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai_evolve'), 'ai.evolve.sample.count', '10', 'number', '每轮采样主题数', '每轮进化从主题池采样的数量', 2, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai_evolve'), 'ai.evolve.topic.max', '1000', 'number', '主题池上限', '主题池最大容量，超出时轮换旧主题', 3, 0),
((SELECT id FROM sys_config_group WHERE group_code='ai_evolve'), 'ai.evolve.quality.threshold', '50', 'number', '入库质量阈值', '报告评分 ≥ 此值才入库（0-100）', 4, 1),
((SELECT id FROM sys_config_group WHERE group_code='ai_evolve'), 'ai.evolve.deepen.threshold', '25', 'number', '深化队列阈值', '报告评分 < 此值时主题加入深化队列', 5, 0),
((SELECT id FROM sys_config_group WHERE group_code='ai_evolve'), 'ai.evolve.context.max_chars', '32000', 'number', '上下文最大字符数', '防止 LLM 超限', 6, 0),
((SELECT id FROM sys_config_group WHERE group_code='ai_evolve'), 'ai.evolve.similarity.threshold', '0.75', 'string', '主题去重阈值', '新主题与现有主题相似度 ≥ 此值时去重', 7, 0);

-- 存储 - 对象存储
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order, is_required) VALUES
((SELECT id FROM sys_config_group WHERE group_code='storage_oss'), 'storage.provider', 'local', 'string', '存储类型', 'local（本地）/ bos（百度）/ oss（阿里云）', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='storage_oss'), 'storage.endpoint', '', 'string', 'Endpoint', '对象存储服务端点地址', 2, 0),
((SELECT id FROM sys_config_group WHERE group_code='storage_oss'), 'storage.bucket', '', 'string', 'Bucket', '存储桶名称', 3, 0),
((SELECT id FROM sys_config_group WHERE group_code='storage_oss'), 'storage.access_key', '', 'password', 'Access Key', '访问密钥 ID', 4, 0),
((SELECT id FROM sys_config_group WHERE group_code='storage_oss'), 'storage.secret_key', '', 'password', 'Secret Key', '访问密钥 Secret', 5, 0);

-- 存储 - 上传限制
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order, is_required) VALUES
((SELECT id FROM sys_config_group WHERE group_code='storage_upload'), 'storage.max.image.size.mb', '10', 'number', '图片最大大小(MB)', '单张图片上传最大大小', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='storage_upload'), 'storage.max.video.size.mb', '200', 'number', '视频最大大小(MB)', '单个视频上传最大大小', 2, 1),
((SELECT id FROM sys_config_group WHERE group_code='storage_upload'), 'storage.max.audio.size.mb', '50', 'number', '音频最大大小(MB)', '单个音频上传最大大小', 3, 1),
((SELECT id FROM sys_config_group WHERE group_code='storage_upload'), 'storage.allowed.types', 'jpg,jpeg,png,gif,webp,mp4,avi,mp3,wav,pdf', 'string', '允许的文件类型', '逗号分隔的文件扩展名', 4, 1);

-- 抖音 OAuth2
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order, is_required) VALUES
((SELECT id FROM sys_config_group WHERE group_code='douyin_oauth'), 'douyin.oauth.client_id', '', 'string', 'Client ID', '抖音开放平台应用的 Client Key', 1, 1),
((SELECT id FROM sys_config_group WHERE group_code='douyin_oauth'), 'douyin.oauth.client_secret', '', 'password', 'Client Secret', '抖音开放平台应用的 Client Secret', 2, 1),
((SELECT id FROM sys_config_group WHERE group_code='douyin_oauth'), 'douyin.oauth.redirect_uri', '', 'string', '回调地址', 'OAuth2 授权回调地址', 3, 1);

-- 抖音 API
INSERT INTO sys_config (group_id, config_key, config_value, config_type, label, description, sort_order) VALUES
((SELECT id FROM sys_config_group WHERE group_code='douyin_api'), 'douyin.api.base_url', 'https://open.douyin.com', 'string', 'API 基础地址', '抖音开放平台 API 地址', 1),
((SELECT id FROM sys_config_group WHERE group_code='douyin_api'), 'douyin.api.rate_limit', '100', 'number', '每秒调用限制', 'API 调用频率限制', 2);

-- ==============================
-- 行业分类
-- ==============================
INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order) VALUES
(0, '美妆', 'beauty', 1),
(0, '健身', 'fitness', 2),
(0, '美食', 'food', 3),
(0, '电商', 'ecommerce', 4),
(0, '教育', 'education', 5),
(0, '服装', 'fashion', 6),
(0, '数码', 'digital', 7),
(0, '母婴', 'baby', 8),
(0, '家居', 'home', 9),
(0, '旅游', 'travel', 10),
(0, '汽车', 'auto', 11),
(0, '医疗健康', 'health', 12),
(0, '金融', 'finance', 13),
(0, '娱乐', 'entertainment', 14),
(0, '其他', 'other', 99);
```

---

## 三、接口设计

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| **配置分组** | | | | |
| 1 | POST | /api/v1/config/group/tree | 管理员 | 配置分组树（2 级） |
| 2 | POST | /api/v1/config/group/save | 管理员 | 新增/编辑分组 |
| 3 | POST | /api/v1/config/group/delete | 管理员 | 删除分组（is_system=1 不可删除） |
| **配置项** | | | | |
| 4 | POST | /api/v1/config/list | 管理员 | 按分组查配置项列表 |
| 5 | POST | /api/v1/config/get | 管理员 | 获取单个配置详情 |
| 6 | POST | /api/v1/config/save | 管理员 | 新增/编辑配置项 |
| 7 | POST | /api/v1/config/delete | 管理员 | 删除配置项 |
| 8 | POST | /api/v1/config/search | 管理员 | 跨分组搜索配置（按 key/label 模糊搜索） |
| 9 | POST | /api/v1/config/value | 内部 | 按 key 获取配置值（Service 层调用） |
| 10 | POST | /api/v1/config/batch-value | 内部 | 按分组编码获取批量配置（Service 层调用） |
| **行业分类** | | | | |
| 11 | POST | /api/v1/config/industry/tree | 登录 | 行业分类树（用户端选行业用） |
| 12 | POST | /api/v1/config/industry/list | 管理员 | 行业分类管理列表 |
| 13 | POST | /api/v1/config/industry/save | 管理员 | 新增/编辑行业分类 |
| 14 | POST | /api/v1/config/industry/delete | 管理员 | 删除行业分类 |

---

## 四、页面设计

| # | 页面 | 路径 | 说明 |
|---|------|------|------|
| 1 | 系统配置 | /admin/config/list.html | 左侧分组树 + 右侧配置表单 |
| 2 | 行业分类管理 | /admin/config/industry-list.html | 行业分类树形管理 |

### 关键页面设计

#### 页面 1：系统配置（左右布局）

```
┌────────────────────────────────────────────────────────────────┐
│ 系统配置                                    [搜索配置项___🔍]  │
├──────────────┬─────────────────────────────────────────────────┤
│              │                                                 │
│ 📁 系统配置  │  安全设置                          [+ 新增配置]  │
│   ├ 基本信息 │  ─────────────────────────────────────────────  │
│   ├ 安全设置 ●│                                                │
│   └ 通知设置 │  Token 有效期（小时）                            │
│              │  [24___________]                                │
│ 📁 AI 配置   │  用户登录 Token 的过期时间                       │
│   ├ 额度设置 │                                                 │
│   ├ 知识库   │  密码最短长度                                    │
│   └ 进化引擎 │  [6____________]                                │
│              │  用户注册/修改密码的最短长度要求                   │
│ 📁 存储配置  │                                                 │
│   ├ 对象存储 │                                                 │
│   └ 上传限制 │                                                 │
│              │                                                 │
│ 📁 抖音接口  │                          [保存]  [重置]         │
│   ├ OAuth2   │                                                 │
│   └ 开放平台 │                                                 │
│              │                                                 │
│ [+ 新增分组] │                                                 │
│              │                                                 │
├──────────────┴─────────────────────────────────────────────────┤
│ ● 点击左侧分组 → 右侧显示该分组下所有配置项（表单形式）        │
│ ● 配置项按 sort_order 排序                                     │
│ ● password 类型显示 ●●●●●● + 「显示」按钮                     │
│ ● 修改后点保存 → 批量保存该分组的所有配置项                     │
└────────────────────────────────────────────────────────────────┘
```

---

## 五、开发任务拆解

| # | 任务 | 依赖 |
|---|------|------|
| B1 | SQL schema.sql + demo.sql（分组 + 配置项 + 行业） | 无 |
| B2 | Entity + Repository（SysConfigGroup + SysConfig + SysIndustry） | B1 |
| B3 | ConfigGroupService（分组 CRUD + 树形查询） | B2 |
| B4 | ConfigService（配置 CRUD + 内存缓存 + 按 key/分组查询） | B2 |
| B5 | IndustryService（行业 CRUD + 树形查询） | B2 |
| B6 | ConfigGroupController | B3 |
| B7 | ConfigController | B4 |
| B8 | IndustryController | B5 |
| B9 | resource-data.sql | B8 |
| F1 | 系统配置页面（左右布局 + 分组树 + 表单） | B6, B7 |
| F2 | 行业分类管理页面 | B8 |

---

## 六、测试用例

| # | 场景 | 预期 |
|---|------|------|
| T01 | 分组树加载 | 返回 2 级分组树 |
| T02 | 点击分组查配置项 | 返回该分组下配置项列表 |
| T03 | 编辑配置项保存 | 值更新，缓存刷新 |
| T04 | 按 key 获取配置值 | Service 层正确返回 |
| T05 | 按分组编码批量获取 | 返回该分组所有配置 |
| T06 | password 类型配置 | 页面展示脱敏，API 不返回原值 |
| T07 | 新增分组 + 新增配置项 | 创建成功，树刷新 |
| T08 | 删除系统预设分组 | 拒绝删除（is_system=1） |
| T09 | 跨分组搜索 | 按 key/label 模糊匹配返回结果 |
| T10 | 行业分类树 | 返回层级行业数据 |

---

## 七、验收标准

### 功能验收

- [ ] 管理员可查看配置分组树
- [ ] 点击分组显示该组配置项（表单形式）
- [ ] 管理员可新增/编辑/删除配置项
- [ ] 管理员可新增/编辑/删除分组
- [ ] password 类型配置页面脱敏
- [ ] 配置修改后缓存立即刷新
- [ ] 按 config_key 查询配置值正确
- [ ] 搜索可跨分组模糊查找
- [ ] 行业分类树形管理正常

### 技术验收

- [ ] 配置有内存缓存，修改时刷新
- [ ] config_key 唯一约束
- [ ] group_code 唯一约束
- [ ] 系统预设分组不可删除

---

## 八、上线检查清单

- [ ] `sql/config/schema.sql` 已执行（含 sys_config_group、sys_config、sys_industry）
- [ ] `sql/config/demo.sql` 已执行（含分组 + 配置项 + 行业）
- [ ] `sql/config/resource-data.sql` 已执行
- [ ] 对应角色已绑定 config 模块资源
- [ ] 冒烟测试全部通过
