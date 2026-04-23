# 05-product-douyin-script-copy-模块分析

> 校正说明（2026-04-10）：
> 本文主体是早期合并分析口径，覆盖 `product / douyin / script / copy` 四个模块，部分统计和实现描述仍带有旧阶段快照。
> 当前 `dy02` 实际基线中：
> `product` 约 `8` 个 controller、`30` 个 service 类、`16` 个 repository、`16` 个 entity、`14` 个后端测试；
> `douyin` 约 `4` 个 controller、`10` 个 service 类、`5` 个 repository、`5` 个 entity、`6` 个后端测试；
> `script` 约 `9` 个 controller、`25` 个 service 类、`12` 个 repository、`12` 个 entity、`11` 个后端测试；
> `copy` 约 `4` 个 controller、`6` 个 service 类、`3` 个 repository、`3` 个 entity、`7` 个后端测试。
> 前端页面与测试覆盖仍明显不足，尤其 `script` 页面的页面级测试仍为空白。
> 后续升级请以 `docs/analysis/11-逐模块前后端风险矩阵-20260410.md` 与当前代码统计为准。

完美！现在我有足够的信息生成完整的分析报告。让我进行最后的总结分析：

---

# 深度模块分析报告：Product / Douyin / Script / Copy

## 一、PRODUCT 模块（商品管理）

### 1. Entity/表结构设计

**表结构：** `dy_product` + `dy_product_sales_history` + `style_preset`

**主要特性：**
- 乐观锁版本控制：`@Version private Integer version` 防并发库存超卖
- 完整的商品属性：价格、成本价、利润率、亏损金额、库存、SKU、条形码等
- 扩展字段：`productLink`（商品链接）、`aiSellingPoints`（AI提炼卖点）、`controlStrategy`（控单策略）
- 状态管理：`status`（上架/下架）、`featured`（推荐标记）
- 销售历史表：关联销售数量、金额、渠道、直播场次

**索引设计：**
```
- idx_product_user_status (user_id, status, featured) - 直播选品查询优化
- idx_product_user_category (user_id, product_category) - 分类搜索
- idx_product_sku / idx_product_barcode - 唯一标识查询
```

### 2. Service业务逻辑

**ProductServiceImpl核心方法：**

| 方法 | 说明 | 特点 |
|------|------|------|
| `search()` | 分页搜索 | 支持keyword(名称/SKU/条形码)、分类、状态、推荐筛选；featured优先排序 |
| `save()` | 新增/编辑 | SKU级联查询防重复；商品链接自动触发异步提取 |
| `updateInventory()` | 库存更新 | 乐优锁捕获`OptimisticLockException`，失败提示刷新 |
| `publish()`/`unpublish()` | 上架/下架 | 直接SQL更新，无事务保护 ⚠️ |
| `setFeatured()` | 推荐标记 | 同样无事务保护 ⚠️ |
| `inferProductType()` | 产品类型推断 | 根据利润率(>30%)、亏损、featured、控单策略组合标签 |

**问题清单：**
- 🔴 `publish/unpublish/setFeatured` 缺 `@Transactional` 注解，虽无复杂逻辑但不符合规范
- 🟡 `updateInventory` 乐观锁异常处理硬编码错误码 `DUPLICATE_SUBMIT`，语义不当

### 3. Controller API

**端点设计（POST为主）：**
```
/api/v1/product/
├── search                    # 商品搜索
├── get                       # 单个商品
├── save                      # 新增/编辑
├── delete                    # 删除
├── publish / unpublish       # 上架/下架
├── set-featured              # 推荐标记
├── update-inventory          # 库存更新
├── infer-product-type        # 产品类型推断（直播选品话术用）
├── extract-from-link         # 从链接提取信息（同步）
├── trigger-extract           # 异步提取（火起来后回调）
├── import-paiping            # Excel导入排品表
└── sales-history/            # 销售历史子模块
    ├── search
    ├── get
    ├── save
    ├── total-sales-amount
    └── total-sales-quantity
```

**特点：**
- ✅ 统一 POST 方法（遵循规范）
- ✅ Bearer Token + DataScopeService 数据隔离
- ✅ `MDC.get("traceId")` 链路追踪
- 🟡 混合 `@RequestParam` 和 `@RequestBody`，风格不统一

### 4. 前端实现

**ProductPage.tsx 特性：**
- 表格展示：商品图片、名称SKU、价格、利润/亏、控单策略、库存、状态、精选标记
- 交互功能：
  - 库存弹窗编辑（计算delta）
  - 图片预览放大
  - 状态/精选快速切换
  - 产品话术管理（独立Dialog）
  - 批量生成话术（SSE进度流）
  - 导入排品表（.xlsx/.xls）
- 链接提取功能：内嵌form dialog，支持从链接自动填充 productName/imageUrl/description/aiSellingPoints
- 分页：10/20/50/100 行数可选

**数据获取流程：**
```tsx
searchProducts() → normalizePageResult() → 自动提取分类列表
                                       → 按featured倒序、createTime倒序展示
```

### 5. 问题清单

| 级别 | 问题 | 影响 | 建议 |
|------|------|------|------|
| 🔴 | Service `publish/unpublish/setFeatured` 缺 `@Transactional` | 不符合规范，潜在并发风险 | 加 `@Transactional(rollbackFor=Exception.class)` |
| 🔴 | `updateInventory` 乐观锁异常映射 `DUPLICATE_SUBMIT`（实际应是版本冲突） | 用户误导 | 改错误码为 `CONFLICT` 或自定义 `INVENTORY_CONFLICT` |
| 🟡 | ProductPage 批量选择UI但无对应后端API | 前端功能不完整 | 补充 `/api/v1/product/batch-update-status` 等批量操作 |
| 🟡 | 链接提取异步任务无错误回调机制 | 用户不知提取成功否 | 补充 WebSocket/SSE 实时反馈，或记录提取日志查询 |
| 🟢 | 销售历史时间范围查询缺失 | 功能完整性 | 前端/后端补充 `saleTimeStart/saleTimeEnd` 筛选 |

---

## 二、DOUYIN 模块（抖音账号管理）

### 1. Entity/表结构设计

**表结构：** `douyin_account` + `douyin_video` + `dy_persona` + `dy_fan_profile`（可选）

**核心表分析：**

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `douyin_account` | 账号主表 | user_id, account_name, account_id(uk), follow_count, fan_count, video_count, total_likes |
| `douyin_video` | 视频表 | account_id, video_id(uk), title, view_count, like_count, share_count, comment_count, download_count |
| `dy_persona` | 人设库 | owner_id, account_id(可空), persona_name, persona_type, tone, target_audience, is_default |
| `dy_fan_profile` | 粉丝画像 | account_id, demographic_segment, behavior_type, preference_tags |

**索引设计：**
```
- uk_douyin_account_id (account_id) WHERE deleted=0 - 账号唯一性
- idx_douyin_account_user_id (user_id) - 用户账号查询
- uk_douyin_video_id (video_id) - 视频唯一性
- idx_douyin_video_account_id (account_id) - 账号视频列表
```

### 2. Service业务逻辑

**DouyinAccountServiceImpl核心方法：**

| 方法 | 实现方式 | 性能特点 |
|------|---------|----------|
| `search()` | JPA Specification + Sort(fanCount/videoCount desc) | 支持模糊匹配 accountName/accountId |
| `getAccountStatistics()` | 聚合查询 + 平均值计算 | 使用 `countByAccountIdAndDeleted`、`sumViewCountByAccountIdAndDeleted` 等自定义 Query |
| `saveAccount()` | 去重查询 `existsByAccountIdAndDeleted` | 新增时防重 |
| `deleteAccount()` | 逻辑删除 | 标准实现 |

**特点：**
- ✅ 统计查询采用 SQL 聚合而非应用层计算，防 OOM
- ✅ 完整的粉丝、视频、获赞统计
- 🟡 缺少账号绑定验证逻辑（实际应调抖音 OpenAPI）
- 🟡 统计结果无缓存，频繁查询会触发多次 SUM 聚合

### 3. Controller API

**端点（POST为主）：**
```
/api/v1/douyin/
├── account/
│   ├── search           # 账号列表（支持名称/ID搜索）
│   ├── get              # 账号详情
│   ├── save             # 新增/编辑
│   ├── delete           # 删除
│   └── statistics       # 统计信息（粉丝/视频/获赞）
├── persona/
│   ├── list             # 人设列表（支持类型筛选）
│   ├── get
│   ├── save
│   ├── delete
│   ├── set-default      # 设置默认人设
│   ├── get-default
│   └── templates        # 人设模板库
└── video/
    └── search           # 视频列表
```

**认证方式：**
- Bearer Token + DataScopeService（用户数据隔离）
- 非管理员仅能查看自己绑定的账号

### 4. 前端实现（缺失分析）

**基于API推断：**
- `searchAccounts()` / `getAccount()` / `saveAccount()` / `deleteAccount()` - 基础CRUD
- `getAccountStatistics(id)` - 统计卡片展示（粉丝数/视频数/获赞数/平均）
- Persona 管理：列表、编辑、设置默认、选择模板

**缺失：**
- 无对应的 React 页面文件（可能在 `/pages/douyin/` 下）
- 视频列表可视化缺失

### 5. 问题清单

| 级别 | 问题 | 影响 | 建议 |
|------|------|------|------|
| 🔴 | 账号 ID 唯一性约束(`uk_douyin_account_id`)但未在 Entity 层声明 `@Unique` | 数据库约束无ORM层对应，异常处理困难 | Entity 加 `@UniqueConstraint` 注解 |
| 🟡 | `getAccountStatistics()` 无缓存机制 | 高频查询 SUM 聚合低效 | 引入 Redis 缓存，TTL 1小时 |
| 🟡 | 账号绑定未验证真实性（缺OpenAPI调用） | 可绑定虚假账号ID | 补充实名验证流程或警告提示 |
| 🟡 | 人设库与账号关联弱（account_id可空） | 复用性好但关系不清 | 补充人设使用统计、推荐人设排序 |
| 🟢 | 粉丝画像表 (`dy_fan_profile`) 读取接口缺失 | 功能不完整 | 补充 `getFanProfile(accountId)` 端点 |

---

## 三、SCRIPT 模块（话术+违规词）

### 1. Entity/表结构设计

**表结构：** `script_library` + `violation_word` + `script_check` + `sc_compliance_word` + `user_violation_word`

| 表名 | 用途 | 关键设计 |
|------|------|---------|
| `script_library` | 话术库 | user_id, title, content, category, source(manual/live/ai), use_count, status |
| `violation_word` | 违规词(全局) | word(uk), level(1=低/2=中/3=高), reason, replacement, status |
| `script_check` | 检测记录 | script_id, check_time, violation_count, violations(JSON), status |
| `sc_compliance_word` | 合规词 | word_type(absolute=可修复/medical=不可修复), word_value, replacement |
| `user_violation_word` | 个人违规词 | user_id, word, level, replacement |

**索引设计（完善）：**
```
- idx_script_user_id (user_id) - 用户话术查询
- uk_violation_word (word) WHERE deleted=0 AND status=1 - 违规词唯一性
- idx_sc_compliance_word_type (word_type) - 合规词分类查询
```

### 2. Service业务逻辑

**ScriptLibraryServiceImpl：**

| 方法 | 特点 |
|------|------|
| `search()` | 支持keyword(标题/内容)、category、source、status 筛选 |
| `incrementUseCount()` | SQL Update 计数，不阻塞查询 |
| `delete()` | 逻辑删除标准实现 |

**ViolationWordServiceImpl（推断）：**
- `checkViolation(text)` - 单文本检测（遍历 violation_word + sc_compliance_word）
- `checkBatch(texts)` - 批量检测（返回结果Map）
- `importViolationWordsCsv()` - CSV导入（后端处理）

**特点：**
- ✅ 话术库与违规库分离，解耦清晰
- ✅ 多层次合规检测（全局违规词 + 合规词库 + 个人自定义词）
- 🟡 合规词库内置（sc_compliance_word），缺少动态更新机制
- 🟡 检测结果无缓存，频繁检测同一文本低效

### 3. Controller API

**端点：**
```
/api/v1/script/
├── list                              # 话术列表
├── get
├── save
├── delete
├── use-count                         # 递增使用计数
├── violation/
│   ├── check                         # 单文本检测
│   ├── check-batch                   # 批量检测
│   ├── public/list                   # 公共违规词列表（只读）
│   └── admin/
│       ├── list                      # 违规词列表（admin）
│       ├── save                      # 新增/编辑违规词
│       ├── delete
│       ├── import                    # CSV导入
│       └── export                    # CSV导出
├── user-violation/
│   ├── search                        # 个人违规词
│   ├── save
│   └── delete
├── template/
│   ├── search                        # 用户话术模板
│   ├── save
│   └── delete
└── admin/template/
    ├── list                          # 系统话术模板
    ├── save
    └── delete
```

### 4. 前端实现

**script.ts API:**
- `listScripts()` / `saveScript()` / `deleteScript()` - 话术CRUD
- `listPublicViolationWords()` / `searchUserViolationWords()` - 违规词搜索
- `checkViolation(text)` / `checkViolationBatch(texts)` - 检测API
- `importViolationWordsCsv()` / `exportViolationWordsCsv()` - CSV操作

### 5. 问题清单

| 级别 | 问题 | 影响 | 建议 |
|------|------|------|------|
| 🔴 | `sc_compliance_word` 与 `violation_word` 关系混淆 | 不清楚两个词库如何组合检测 | 补充详细的检测流程文档 |
| 🔴 | CSV导入/导出缺少权限验证（可能任何user都能操作） | 安全风险 | Controller加 `@PreAuthorize("hasRole('ADMIN')")`  |
| 🟡 | 违规词检测无缓存，频繁检测同一文本低效 | 性能问题 | 引入 Redis 缓存检测结果 |
| 🟡 | `script_check` 表写入记录但无对应查询接口 | 检测历史查询困难 | 补充 `/script/check/history/{scriptId}` 端点 |
| 🟡 | 个人违规词与全局违规词缺合并检测逻辑 | 用户体验割裂 | 统一检测接口，按优先级返回 |
| 🟢 | 话术模板与话术库关系未建立 | 模板功能独立不成体系 | 补充模板应用推荐、快速复用 |

---

## 四、COPY 模块（文案库+审批+模板）

### 1. Entity/表结构设计

**表结构：** `copy_library` + `copy_approval` + `copy_template`

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `copy_library` | 文案库 | user_id, title, content, category, tags, word_count(auto), use_count, rating(1-5), status(0=待审/1=已审) |
| `copy_approval` | 审批表 | copy_id, user_id(审核员), approval_status(1=通过/0=拒绝/2=待审), comments, approval_time |
| `copy_template` | 模板库 | user_id, template_name, template_content(含{变量}), category, status |

**特点：**
- ✅ 文案字数自动计算（Entity.save 时计算 content.length()）
- ✅ 多维度评分（rating 1-5）和使用计数
- ✅ 审核流程（待审 → 已审）
- 🟡 字数计算按汉字个数，未区分中英文
- 🟡 模板变量语法无验证（如 `{变量名}` 格式约定未强制）

### 2. Service业务逻辑

**CopyLibraryServiceImpl：**

| 方法 | 实现 |
|------|------|
| `search()` | Specification支持keyword(标题/内容)、category、status 筛选 |
| `save()` | 自动计算 wordCount = content.length() |
| `updateStatus()` | SQL直接更新，改状态 0→1（待审→已审） |
| `incrementUseCount()` | SQL自增，追踪热度 |

**CopyApprovalServiceImpl（推断）：**
- `search()` - 按状态/时间筛选审批列表
- `approve()` / `reject()` - 审核操作
- `updateStatus()` - 同步库表状态

**特点：**
- ✅ 审批与文案库分离，工作流清晰
- 🟡 无审批权限控制（谁能审批未验证）
- 🟡 审批流缺业务规则（如是否必须经过特定人员、工时要求等）

### 3. Controller API

**端点：**
```
/api/v1/copy/
├── library/
│   ├── search           # 文案搜索
│   ├── get
│   ├── save             # 新增/编辑
│   ├── delete
│   ├── update-status    # 状态更新（待审→已审）
│   └── increment-use-count
├── approval/
│   ├── search           # 审批列表
│   ├── get
│   └── save             # 提交审批
└── template/
    ├── search
    ├── get
    ├── save
    ├── delete
    └── update-status
```

### 4. 前端实现

**copy.ts API：**
- 文案库：`searchCopyLibraries()` / `getCopyLibrary()` / `saveCopyLibrary()` / `deleteCopyLibrary()`
- 审批：`searchCopyApprovals()` / `getCopyApproval()` / `saveCopyApproval()`
- 模板：`searchCopyTemplates()` / `saveCopyTemplate()` / `deleteCopyTemplate()`
- AI生成：`generateCopy({ topic, category, style, keywords, length })` - 调 AI 模块

### 5. 问题清单

| 级别 | 问题 | 影响 | 建议 |
|------|------|------|------|
| 🔴 | 字数计算 `wordCount = content.length()` 精度低 | 中文/英文/标点计数不准 | 改为智能分词计数（HanLP or Jieba） |
| 🔴 | 审批权限无验证 | 任何登录用户可审批 | Controller加 `roleCode=="ADMIN"` 或 `@PreAuthorize` |
| 🔴 | 模板变量格式无验证 | 用户可输入 `{invalid}` 导致应用错误 | 补充正则验证：`\{[a-zA-Z_][a-zA-Z0-9_]*\}` |
| 🟡 | 审批后无联动通知 | 用户不知道审批结果 | 补充消息/邮件通知（调通知模块） |
| 🟡 | 模板应用逻辑缺失 | 模板定义但无快速填充API | 补充 `applyCopyTemplate(templateId, variables)` |
| 🟡 | AI生成调用无失败重试 | 一次失败则生成失败 | 补充重试逻辑、超时设置、降级方案 |
| 🟢 | 文案导出（批量下载）缺失 | 无法批量导出 | 补充 CSV/Excel 导出功能 |

---

## 五、跨模块对比总结

### 架构一致性评分

| 模块 | Entity | Service | Controller | 前端 | 总体评分 |
|------|--------|---------|-----------|------|---------|
| Product | 8/10 | 7/10 | 8/10 | 8/10 | **7.75/10** |
| Douyin | 7/10 | 7/10 | 8/10 | 6/10 | **7/10** |
| Script | 7/10 | 6/10 | 7/10 | 7/10 | **6.75/10** |
| Copy | 6/10 | 6/10 | 6/10 | 6/10 | **6/10** |

### 通用设计债务

| 项目 | Product | Douyin | Script | Copy |
|------|---------|--------|--------|------|
| 🔴 **事务性不足** | publish/unpublish 缺 @Transactional | - | CSV操作权限检查缺失 | 审批权限检查缺失 |
| 🔴 **缓存机制缺失** | 链接提取无进度反馈 | 统计查询无缓存 | 违规词检测无缓存 | - |
| 🔴 **错误码映射不清** | 乐观锁异常→DUPLICATE_SUBMIT | - | - | - |
| 🟡 **权限细粒度不足** | DataScopeService仅支持user级 | 同左 | 违规词admin接口无权限检查 | 审批接口无角色验证 |
| 🟡 **监控告警缺失** | 异步链接提取无日志记录 | 统计查询无性能监控 | 批量检测无超时保护 | AI生成无失败统计 |

---

## 六、升级建议（优先级）

### **P0（极高）**

1. **Product**
   - [ ] 补充 `publish/unpublish/setFeatured` 事务注解
   - [ ] 修正库存异常码（DUPLICATE_SUBMIT → INVENTORY_CONFLICT）
   - [ ] 链接提取异步补充错误回调机制（WebSocket实时推送或数据库记录）

2. **Script**
   - [ ] CSV导入/导出加权限验证（仅ADMIN可操作）
   - [ ] 统一合规词库与违规词库检测逻辑文档

3. **Copy**
   - [ ] 审批接口加角色验证（@PreAuthorize("hasRole('ADMIN')")）
   - [ ] 模板变量格式验证正则：`\{[a-zA-Z_][a-zA-Z0-9_]*\}`

### **P1（高）**

1. **所有模块**
   - [ ] 引入 Redis 缓存：Product销售统计、Douyin账号统计、Script违规词检测结果
   - [ ] 统一权限模型：从 DataScopeService（仅user级）扩展到角色级细粒度控制

2. **Product**
   - [ ] 补充批量操作API：`/batch-update-status`、`/batch-delete`
   - [ ] 销售历史补充时间范围筛选

3. **Douyin**
   - [ ] 补充账号真实性验证（调OpenAPI或人工验证流程）
   - [ ] 粉丝画像表读取接口：`getFanProfile(accountId)`

4. **Script**
   - [ ] 补充检测历史查询：`/script/check/history/{scriptId}`
   - [ ] 个人违规词与全局违规词合并检测

5. **Copy**
   - [ ] 字数计算精度提升（分词计数替代length）
   - [ ] 补充模板应用API：`applyCopyTemplate(templateId, variables)`

### **P2（中）**

1. **Product**
   - [ ] ProductPage 批量生成话术现有但需SSE进度完善
   - [ ] 多风格话术生成补充版本历史回滚

2. **Script**
   - [ ] 话术模板与话术库关联推荐系统

3. **Copy**
   - [ ] AI生成补充重试机制、超时保护、降级方案
   - [ ] 审批后消息/邮件通知

4. **所有模块**
   - [ ] 完善OpenAPI文档中的错误码说明
   - [ ] 补充单元测试覆盖（当前关键路径缺测）

### **P3（低）**

1. [ ] Copy 批量导出 CSV/Excel
2. [ ] Douyin 视频分析可视化（图表展示）
3. [ ] Script 话术库推荐排序（基于使用频率、评分、创建时间）

---

## 七、技术债清单

| 债项 | 模块 | 类型 | 预计工作量 | 风险等级 |
|------|------|------|----------|---------|
| 事务注解缺失 | Product | 规范 | 0.5h | 中 |
| 权限验证缺失 | Script/Copy | 安全 | 2h | 高 |
| 缓存机制缺失 | Product/Douyin/Script | 性能 | 8h | 中 |
| 错误码语义不清 | Product | 接口 | 1h | 低 |
| 异步反馈机制缺失 | Product | 功能 | 4h | 中 |
| 分词精度不足 | Copy | 业务 | 2h | 低 |
| 模板验证缺失 | Copy | 功能 | 1h | 中 |

**总计预计工作量：** ~18.5h（1个开发者约2.5天）

---

**报告生成时间：** 2026-03-05  
**分析深度：** Entity/Service/Controller/前端/SQL 五层完整分析
