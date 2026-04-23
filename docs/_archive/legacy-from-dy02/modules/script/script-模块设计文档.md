> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# script 模块设计文档

> 版本：1.1 | 更新日期：2026-02-24 | 阶段：P1

---

## 一、需求分析（Requirements Analysis）

### 1.1 模块定位

script 模块是话术和违规管理的基础服务，为 live 模块和 shortvideo 模块提供：

1. **话术库** — 用户收藏/创建的话术，支持分类、标签、搜索，可在直播和短视频创作中复用
2. **违规词库** — 公共违规词库（平台级，管理员维护）+ 个人违规词库（用户私有）
3. **违规检测服务** — 对话术/文案文本做违规词扫描，返回违规词位置和替换建议

### 1.2 用户故事（User Stories）

| 编号 | 角色 | 故事 | 验收条件 |
|------|------|------|----------|
| SC-01 | 主播/达人 | 我要能管理自己的话术库 | 话术 CRUD，按分类/标签/关键词搜索 |
| SC-02 | 主播/达人 | 我要能给话术分类和打标签 | 支持自定义分类（开场/产品/通用等）和标签 |
| SC-03 | 主播/达人 | 我要能搜索和复用话术 | 关键词搜索、按分类筛选、一键复用到直播/短视频 |
| SC-04 | 主播/达人 | 我要能查看公共违规词库 | 浏览公共违规词、了解违规等级和分类 |
| SC-05 | 主播/达人 | 我要能维护个人违规词库 | 个人违规词 CRUD，支持自定义替换建议 |
| SC-06 | 主播/达人 | 我要能对任意文本做违规检测 | 输入文本 → 返回违规词列表+位置+等级+替换建议 |
| SC-07 | 管理员 | 我要能维护公共违规词库 | 违规词 CRUD，分类/等级管理，批量导入导出 |
| SC-08 | 管理员 | 我要能管理话术模板 | 系统预设话术模板，按行业/场景分类 |
| SC-09 | 达人/主播 | 我要能保存自己的话术为模板 | 把好的话术保存为个人模板，标注行业/场景/风格 |
| SC-10 | 达人/主播 | 我要能从模板快速创建话术 | 浏览系统/个人模板 → 一键引用 → 修改后保存 |

### 1.3 功能清单

```
script 模块
├── 话术库（用户私有）
│   ├── 话术 CRUD
│   ├── 话术分类（开场/产品讲解/转场/结尾/通用/互动）
│   ├── 话术标签（自定义标签）
│   ├── 话术搜索（关键词+分类+标签）
│   ├── 话术收藏（从直播/AI生成中保存）
│   └── 话术列表（分页）
│
├── 违规词库
│   ├── 公共违规词库（平台级）
│   │   ├── 违规词 CRUD
│   │   ├── 违规分类（广告法/平台规则/敏感词/低俗词）
│   │   ├── 违规等级（forbidden=禁止 / warning=警告 / suggest=建议修改）
│   │   ├── 替换建议（每个违规词可配置推荐替换词）
│   │   └── 批量导入/导出（CSV）
│   │
│   └── 个人违规词库（用户私有）
│       ├── 违规词 CRUD
│       ├── 自定义替换建议
│       └── 与公共库合并检测
│
├── 违规检测服务
│   ├── 单文本违规扫描（返回违规词+位置+等级+替换）
│   ├── 批量文本检测（整场直播话术一键检测）
│   └── AI 替换建议（调用 ai 模块推荐合规替代表达）
│
└── 话术模板
    ├── 系统预设话术模板 CRUD（管理员维护）
    ├── 个人话术模板 CRUD（用户从话术库保存）
    ├── 按行业/场景分类
    └── 从模板创建话术
```

### 1.4 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 违规检测合并 | 检测时同时匹配公共库和用户个人库 |
| BR-02 | 违规等级优先 | 同一词在公共库和个人库都有时，取更高等级 |
| BR-03 | 公共库只读 | 达人（talent）和机构（institution）只能浏览公共违规词库，不能修改 |
| BR-04 | 话术数据隔离 | 用户只能看到自己的话术库（owner_id） |
| BR-05 | 话术来源标记 | 话术记录来源：manual（手动创建）/ live（从直播保存）/ ai（AI 生成） |
| BR-06 | 违规词匹配 | 使用字符串包含匹配，不区分大小写 |
| BR-07 | 违规词适用范围 | 违规词按 scope 区分：all（全场景）/ live_only（仅直播违规）/ video_only（仅短视频违规）。检测时调用方传入场景类型进行过滤 |
| BR-08 | 直播场景检测 | 直播话术检测时匹配 scope=all 和 scope=live_only 的违规词 |
| BR-09 | 短视频场景检测 | 短视频文案检测时匹配 scope=all 和 scope=video_only 的违规词 |
| BR-10 | 公共库缓存策略 | 公共违规词库 Caffeine 缓存 TTL=5 分钟。管理员修改公共库时不主动刷新缓存（等 TTL 自动过期），因修改频率低 |
| BR-11 | 个人库无缓存 | 个人违规词库不缓存（数据量小，直接查数据库），添加/修改后立即生效 |
| BR-12 | 批量导入策略 | CSV 批量导入采用"跳过重复"策略：逐行处理，word 已存在的行跳过（不报错），不存在的行插入。完成后返回导入成功数 + 跳过数 |
| BR-13 | 违规词匹配策略 | 使用字符串包含匹配（LIKE），不区分大小写。PostgreSQL 使用 pg_trgm 扩展加速 ILIKE 查询 |

### 1.5 模块依赖

```
依赖关系：
auth ← script

script 被依赖：
live → script（话术库复用、违规检测）
shortvideo → script（违规检测）
ai → script（AI 替换建议）
```

---

## 二、数据库设计（Database Design）

### 2.1 ER 关系

```
auth_user ──→ sc_script (一对多，用户话术库)
auth_user ──→ sc_user_violation_word (一对多，个人违规词)
sc_violation_word (公共违规词库，平台级)
sc_script_template (话术模板，管理员维护)
```

### 2.2 表结构

#### sc_script — 话术表（用户私有）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| title | VARCHAR(128) | NOT NULL | — | 话术标题 |
| content | TEXT | NOT NULL | — | 话术内容 |
| category | VARCHAR(32) | | — | 分类：opening / product / transition / closing / general / interaction |
| tags | VARCHAR(512) | | — | 标签（JSON 数组） |
| source | VARCHAR(16) | | 'manual' | 来源：manual / live / ai |
| source_id | BIGINT | | — | 来源 ID（如直播场次 ID） |
| persona_id | BIGINT | | — | 关联的人设 ID |
| style | VARCHAR(64) | | — | 风格 |
| duration_hint | VARCHAR(32) | | — | 建议时长 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(owner_id, category, create_time DESC) WHERE deleted=0`

#### sc_violation_word — 公共违规词表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| word | VARCHAR(128) | NOT NULL | — | 违规词 |
| category | VARCHAR(32) | NOT NULL | — | 分类：ad_law（广告法）/ platform（平台规则）/ sensitive（敏感词）/ vulgar（低俗词） |
| level | VARCHAR(16) | NOT NULL | 'warning' | 等级：forbidden（禁止）/ warning（警告）/ suggest（建议修改） |
| scope | VARCHAR(16) | NOT NULL | 'all' | 适用范围：all（全场景）/ live_only（仅直播违规）/ video_only（仅短视频违规） |
| replacement | VARCHAR(256) | | — | 推荐替换词 |
| description | VARCHAR(256) | | — | 说明（为什么违规） |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(word) WHERE deleted=0 AND status=1`、`(category, level) WHERE deleted=0`、`(scope) WHERE deleted=0 AND status=1`、`GIN(word gin_trgm_ops) WHERE deleted=0 AND status=1`（pg_trgm 加速 ILIKE 匹配）

#### sc_user_violation_word — 个人违规词表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| word | VARCHAR(128) | NOT NULL | — | 违规词 |
| level | VARCHAR(16) | NOT NULL | 'warning' | 等级 |
| scope | VARCHAR(16) | NOT NULL | 'all' | 适用范围：all / live_only / video_only |
| replacement | VARCHAR(256) | | — | 推荐替换词 |
| description | VARCHAR(256) | | — | 说明 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(owner_id, word) WHERE deleted=0`

#### sc_script_template — 话术模板表（系统+个人）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | | — | 所属用户 ID（NULL=系统模板） |
| template_type | VARCHAR(16) | NOT NULL | 'system' | 类型：system（系统预设）/ user（个人模板） |
| template_name | VARCHAR(128) | NOT NULL | — | 模板名称 |
| content | TEXT | NOT NULL | — | 模板内容 |
| category | VARCHAR(32) | | — | 分类：opening / product / transition / closing / general / interaction |
| industry_id | BIGINT | | — | 行业 ID（关联 sys_industry.id） |
| style | VARCHAR(64) | | — | 风格 |
| tags | VARCHAR(512) | | — | 标签（JSON 数组） |
| use_count | INTEGER | | 0 | 使用次数 |
| sort_order | INTEGER | | 0 | 排序（系统模板用） |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(template_type, category, industry) WHERE deleted=0`、`(owner_id, create_time DESC) WHERE deleted=0`

### 2.3 初始数据

```sql
-- 公共违规词（广告法高频词）
INSERT INTO sc_violation_word (word, category, level, scope, replacement, description) VALUES
('最好的', 'ad_law', 'forbidden', 'all', '非常好的', '广告法禁止使用绝对化用语'),
('第一', 'ad_law', 'forbidden', 'all', '领先的', '广告法禁止使用排名用语'),
('最便宜', 'ad_law', 'forbidden', 'all', '性价比高的', '广告法禁止使用绝对化用语'),
('国家级', 'ad_law', 'forbidden', 'all', '高品质', '广告法禁止使用国家级等用语'),
('100%', 'ad_law', 'warning', 'all', '接近100%', '绝对化数据需有权威证明'),
('永久', 'ad_law', 'forbidden', 'all', '持久', '广告法禁止使用绝对化时间用语'),
('万能', 'ad_law', 'forbidden', 'all', '多功能', '广告法禁止使用绝对化用语'),
('秒杀全网', 'platform', 'warning', 'all', '优惠力度大', '平台不允许与竞品直接对比'),
('假一赔十', 'platform', 'suggest', 'all', '正品保证', '需有相关资质支撑'),
('免费送', 'platform', 'warning', 'all', '赠送', '平台对免费赠送有限制'),
-- 仅直播违规的词（短视频不违规）
('点点关注', 'platform', 'warning', 'live_only', '感谢关注', '直播中过度引导关注可能被限流'),
('家人们', 'platform', 'suggest', 'live_only', '朋友们', '直播中过度使用可能被标记为低质'),
('扣1', 'platform', 'warning', 'live_only', '评论区互动', '直播中引导刷屏评论可能被限制'),
('去直播间', 'platform', 'forbidden', 'live_only', NULL, '直播中引导跳转其他直播间违规'),
('截屏抽奖', 'platform', 'forbidden', 'live_only', '福袋抽奖', '直播中截屏抽奖不合规，需使用平台福袋');
```

---

## 三、接口设计（API Design）

### 3.1 接口总表

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| **话术库** | | | | |
| 1 | POST | /api/v1/script/list | 登录 | 我的话术列表（分页、搜索） |
| 2 | POST | /api/v1/script/get | 登录 | 话术详情 |
| 3 | POST | /api/v1/script/save | 登录 | 新增/编辑话术 |
| 4 | POST | /api/v1/script/delete | 登录 | 删除话术 |
| **违规检测** | | | | |
| 5 | POST | /api/v1/script/violation/check | 登录 | 文本违规检测 |
| 6 | POST | /api/v1/script/violation/check-batch | 登录 | 批量文本违规检测 |
| **公共违规词** | | | | |
| 7 | POST | /api/v1/script/violation/public/list | 登录 | 公共违规词列表（分页） |
| **个人违规词** | | | | |
| 8 | POST | /api/v1/script/violation/user/list | 登录 | 我的违规词列表 |
| 9 | POST | /api/v1/script/violation/user/save | 登录 | 新增/编辑个人违规词 |
| 10 | POST | /api/v1/script/violation/user/delete | 登录 | 删除个人违规词 |
| **话术模板** | | | | |
| 11 | POST | /api/v1/script/template/list | 登录 | 话术模板列表（系统+个人） |
| 12 | POST | /api/v1/script/template/get | 登录 | 模板详情 |
| 13 | POST | /api/v1/script/template/save-user | 登录 | 保存个人话术模板 |
| 14 | POST | /api/v1/script/template/delete-user | 登录 | 删除个人话术模板 |
| **管理端** | | | | |
| 15 | POST | /api/v1/script/admin/violation/list | 管理员 | 公共违规词管理列表 |
| 16 | POST | /api/v1/script/admin/violation/save | 管理员 | 新增/编辑公共违规词 |
| 17 | POST | /api/v1/script/admin/violation/delete | 管理员 | 删除公共违规词 |
| 18 | POST | /api/v1/script/admin/violation/import | 管理员 | 批量导入违规词（CSV） |
| 19 | POST | /api/v1/script/admin/violation/export | 管理员 | 导出违规词（CSV） |
| 20 | POST | /api/v1/script/admin/template/list | 管理员 | 系统话术模板管理列表 |
| 21 | POST | /api/v1/script/admin/template/save | 管理员 | 新增/编辑系统话术模板 |
| 22 | POST | /api/v1/script/admin/template/delete | 管理员 | 删除系统话术模板 |

### 3.2 关键接口详情

#### 接口 5：文本违规检测

```
POST /api/v1/script/violation/check
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| text | String | 是 | 待检测文本 |
| scope | String | 是 | 适用场景：all（检测所有违规词）/ live（直播场景，匹配 scope=all + live_only）/ video（短视频场景，匹配 scope=all + video_only）。调用方必须传入，不可省略 |

**响应：**

```json
{
  "status": 200,
  "data": {
    "hasViolation": true,
    "totalCount": 3,
    "violations": [
      {
        "word": "最好的",
        "position": 15,
        "length": 3,
        "category": "ad_law",
        "level": "forbidden",
        "scope": "all",
        "replacement": "非常好的",
        "description": "广告法禁止使用绝对化用语",
        "source": "public"
      },
      {
        "word": "秒杀全网",
        "position": 45,
        "length": 4,
        "category": "platform",
        "level": "warning",
        "replacement": "优惠力度大",
        "source": "public"
      },
      {
        "word": "微信",
        "position": 80,
        "length": 2,
        "category": "platform",
        "level": "warning",
        "replacement": "私信",
        "source": "user"
      }
    ]
  }
}
```

**业务逻辑：**
1. 加载公共违规词库（可缓存，5 分钟 TTL）
2. 加载用户个人违规词库
3. 按 scope 过滤违规词（live 场景 = all + live_only，video 场景 = all + video_only）
4. 合并两个词库（同一词在公共库和个人库都有时，取更高等级）
5. 遍历文本，对每个违规词做字符串包含匹配
6. 记录匹配到的位置、等级、替换建议、来源
7. 返回结果

---

#### 接口 6：批量文本违规检测

```
POST /api/v1/script/violation/check-batch
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| texts | List\<TextItem\> | 是 | 待检测文本列表 |
| texts[].key | String | 是 | 文本标识（如"opening""product_1"） |
| texts[].text | String | 是 | 文本内容 |

**响应：**

```json
{
  "status": 200,
  "data": {
    "results": {
      "opening": { "hasViolation": false, "violations": [] },
      "product_1": { "hasViolation": true, "violations": [...] }
    },
    "totalViolations": 2
  }
}
```

---

## 四、页面设计（Frontend Design）

### 4.1 页面清单

| # | 页面 | 路径 | 入口 | 说明 |
|---|------|------|------|------|
| 1 | 话术库 | /talent/script/list | 侧栏菜单 | 我的话术列表+编辑 |
| 2 | 违规词库 | /talent/script/violation | 侧栏菜单 | 公共库浏览+个人库管理 |
| 3 | 违规检测 | /talent/script/check | 侧栏菜单 | 文本违规在线检测 |
| 4 | 机构话术查看 | /org/script/list | 机构侧栏 | 旗下达人话术只读查看（DataScope） |
| 5 | 公共违规词管理 | /admin/script/violation | 管理端菜单 | 违规词 CRUD |
| 6 | 话术模板管理 | /admin/script/templates | 管理端菜单 | 模板 CRUD |

### 4.2 关键页面设计

#### 页面 3：违规检测页

```
┌────────────────────────────────────────────────────────┐
│ 话术违规检测                                            │
├────────────────────────────────────────────────────────┤
│                                                        │
│  输入或粘贴话术文本:                                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 各位宝宝们！这是全网最好的面霜，秒杀全网所有     │  │
│  │ 品牌！100%纯天然成分，加我微信还有优惠...        │  │
│  │                                                  │  │
│  │                                                  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  [开始检测]                                             │
│                                                        │
│  ── 检测结果 ──                                         │
│  发现 4 处违规                                          │
│                                                        │
│  ┌ 🔴 禁止 ─────────────────────────────────────────┐  │
│  │ "最好的"（位置15）→ 建议改为"非常好的"            │  │
│  │  原因: 广告法禁止使用绝对化用语                    │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌ 🟡 警告 ─────────────────────────────────────────┐  │
│  │ "秒杀全网"（位置30）→ 建议改为"优惠力度大"       │  │
│  │ "100%"（位置45）→ 建议改为"接近100%"             │  │
│  │ "微信"（位置60）→ 建议改为"私信"（个人库）       │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  [一键替换所有建议]  [AI推荐更好的替换]                   │
└────────────────────────────────────────────────────────┘
```

---

## 五、开发任务拆解（Task Breakdown）

### 5.1 后端任务

| # | 任务 | 输出 | 依赖 |
|---|------|------|------|
| B1 | 编写 SQL | schema.sql + demo.sql（含默认违规词） | 无 |
| B2 | Entity 层 | ScScript, ScViolationWord, ScUserViolationWord, ScScriptTemplate | B1 |
| B3 | Repository 层 | 4 个 Repository | B2 |
| B4 | VO 层 | ScriptSaveVO, ViolationCheckVO, ViolationResultVO 等 | B2 |
| B5 | ScScriptService | 话术 CRUD、搜索 | B3, B4 |
| B6 | ScViolationService | 违规检测（合并公共+个人库、文本匹配） | B3, B4 |
| B7 | ScViolationAdminService | 公共违规词 CRUD、导入导出 | B3, B4 |
| B8 | ScScriptController | 话术库接口 | B5 |
| B9 | ScViolationController | 违规检测 + 个人违规词接口 | B6 |
| B10 | ScAdminController | 管理端接口 | B7 |
| B11 | resource-data.sql | script 模块资源注册 | B10 |

### 5.2 前端任务

| # | 任务 | 依赖 |
|---|------|------|
| F1 | 话术库页面 | B8 |
| F2 | 违规词库页面 | B9 |
| F3 | 违规检测页面 | B9 |
| F4 | 公共违规词管理（管理端） | B10 |
| F5 | 话术模板管理（管理端） | B10 |

---

## 六、测试用例（Test Cases）

### 6.1 冒烟测试

| # | 场景 | 预期 |
|---|------|------|
| S1 | 话术列表 | 200，返回用户话术 |
| S2 | 违规检测 | 200，返回检测结果 |
| S3 | 公共违规词列表 | 200，返回违规词 |

### 6.2 功能测试

| # | 场景 | 预期 |
|---|------|------|
| F01 | 文本包含公共违规词 | 检测出违规，source=public |
| F02 | 文本包含个人违规词 | 检测出违规，source=user |
| F03 | 同一词公共和个人都有 | 取更高等级 |
| F04 | 文本无违规 | hasViolation=false |
| F05 | 批量检测多段文本 | 每段独立检测，按 key 返回 |
| F06 | 添加个人违规词后立即检测 | 新词生效 |
| F07 | 话术保存来源标记 | source 正确（manual/live/ai） |
| F08 | CSV 导入违规词 | 批量创建，重复词跳过 |
| F09 | 直播场景检测只匹配 all+live_only | scope=live_only 的词在直播检测时命中，在短视频检测时不命中 |
| F10 | 短视频场景检测只匹配 all+video_only | scope=video_only 的词在短视频检测时命中，在直播检测时不命中 |
| F11 | 个人模板 CRUD | 创建/编辑/删除个人话术模板，数据隔离 |
| F12 | 从模板创建话术 | 引用模板内容创建话术正确 |

---

## 七、验收标准（Acceptance Criteria）

### 7.1 功能验收

- [ ] 达人（talent）/机构（institution）可 CRUD 话术，支持分类和标签
- [ ] 达人（talent）/机构（institution）可搜索话术（关键词+分类+标签）
- [ ] 达人（talent）/机构（institution）可浏览公共违规词库
- [ ] 达人（talent）/机构（institution）可管理个人违规词库
- [ ] 违规检测可同时匹配公共和个人库
- [ ] 违规检测返回词/位置/等级/替换建议
- [ ] 管理员（admin）可 CRUD 公共违规词
- [ ] 管理员（admin）可批量导入导出违规词
- [ ] 管理员（admin）可管理话术模板

### 7.2 技术验收

- [ ] 违规词库有缓存（公共库 5 分钟 TTL）
- [ ] 批量检测性能可接受（<1s / 10 段文本）
- [ ] 数据隔离（话术 owner_id、个人违规词 owner_id）
- [ ] 逻辑删除、事务管理

---

## 八、上线检查清单（Release Checklist）

- [ ] `sql/script/schema.sql` 已执行
- [ ] `sql/script/demo.sql` 已执行（含默认违规词）
- [ ] `sql/script/resource-data.sql` 已执行
- [ ] 对应角色已绑定 script 模块资源
- [ ] 错误码已注册
- [ ] 冒烟测试全部通过
