# script（话术）模块全面审计报告

> 审计日期：2026-03-03 | 对照版本：设计文档 3.0

---

## 1. 模块定位与边界

### 1.1 设计定位

script 模块是话术与违规管理的基础服务，为 live、shortvideo、ai 提供：

| 能力 | 说明 |
|------|------|
| **话术库** | 用户收藏/创建的话术，支持分类、标签、搜索，可在直播和短视频中复用 |
| **违规词库** | 公共违规词（平台级）+ 个人违规词（用户私有） |
| **违规检测** | 文本违规扫描，返回违规词位置和替换建议 |

### 1.2 模块依赖关系

| 依赖方向 | 模块 | 依赖内容 |
|---------|------|----------|
| script 依赖 | auth | 用户认证、owner_id 数据隔离 |
| live 依赖 script | live | 话术库复用、违规检测、保存到话术库 |
| shortvideo 依赖 script | shortvideo | 文案违规检测 |
| ai 依赖 script | ai | AI 替换建议（实际为 script 内部调用 LlmClient） |

### 1.3 实际集成情况

| 消费方 | 集成状态 | 说明 |
|--------|----------|------|
| **live** | ✅ 已集成 | `LiveAiServiceImpl` 调用 `ViolationWordService.check()`；`LiveScriptServiceImpl.saveToLibrary()` 调用 `ScriptLibraryService.save()` |
| **shortvideo** | ❌ 未集成 | 未发现直接调用 script 违规检测 |
| **ai** | ⚠️ 反向依赖 | `ViolationWordServiceImpl.suggestReplacement()` 内部调用 `LlmClient`，属于 script 依赖 ai，非 ai 调用 script |

---

## 2. 后端实现

### 2.1 Controller 清单

| Controller | 路径前缀 | 接口数 | 说明 |
|------------|----------|--------|------|
| ScriptController | `/api/v1/script` | 7 | 话术 CRUD、违规检测、公共违规词列表、AI 替换 |
| ViolationWordAdminController | `/api/v1/script/admin/violation` | 4 | 公共违规词管理（list/save/delete/active） |
| ScriptTemplateController | `/api/v1/script/template` | 6 | 模板 search/get/save/delete/use-count/by-scene |
| UserViolationWordController | `/api/v1/script/user-violation` | 5 | 个人违规词 search/get/save/delete/listActive |

### 2.2 设计 vs 实现 API 对照

| # | 设计路径 | 实际路径 | 状态 |
|---|----------|----------|------|
| 1 | POST /script/list | POST /script/list | ✅ |
| 2 | POST /script/get | POST /script/get | ✅ |
| 3 | POST /script/save | POST /script/save | ✅ |
| 4 | POST /script/delete | POST /script/delete | ✅ |
| 5 | POST /script/violation/check | POST /script/violation/check | ✅ |
| 6 | POST /script/violation/check-batch | — | ❌ 未实现 |
| 7 | POST /script/violation/public/list | POST /script/violation/public/list | ✅ |
| 8 | POST /script/violation/user/list | POST /script/user-violation/search | ⚠️ 路径不同 |
| 9 | POST /script/violation/user/save | POST /script/user-violation/save | ⚠️ 路径不同 |
| 10 | POST /script/violation/user/delete | POST /script/user-violation/delete | ⚠️ 路径不同 |
| 11 | POST /script/template/list | POST /script/template/search | ⚠️ 动作名不同 |
| 12-14 | template/get, save-user, delete-user | 部分在 template 下 | ⚠️ 需核对 |
| 15-17 | admin/violation/list,save,delete | admin/violation/* | ✅ |
| 18 | admin/violation/import | — | ❌ 未实现 |
| 19 | admin/violation/export | — | ❌ 未实现 |
| 20-22 | admin/template/* | — | ❌ 管理端模板接口未实现 |

### 2.3 Entity / VO 完整性

| 资源 | Entity | SearchVO | SaveVO | VO | 备注 |
|------|--------|----------|--------|-----|------|
| 话术库 | ScriptLibrary | ScriptSearchVO | ScriptSaveVO | ScriptVO | ✅ |
| 公共违规词 | ViolationWord | ViolationWordSearchVO | ViolationWordSaveVO | ViolationWordVO | ✅ |
| 个人违规词 | UserViolationWord | UserViolationWordSearchVO | UserViolationWordSaveVO | UserViolationWordVO | ✅ |
| 话术模板 | ScriptTemplate | ScriptTemplateSearchVO | ScriptTemplateSearchVO | ScriptTemplateVO | ✅ |
| 检测记录 | ScriptCheck | — | — | — | 有 Entity 和表，无业务使用 |

### 2.4 额外实现（设计外）

- `ScriptController.use-count`：递增话术使用次数
- `ScriptController.violation/suggest-replacement`：AI 替换建议
- `ViolationWordAdminController.active`：GET 获取所有启用违规词
- `ScriptTemplateController.by-scene`：GET 按场景获取模板

---

## 3. 数据库设计

### 3.1 表结构对照

| 设计表名 | 实际表名 | 状态 |
|----------|----------|------|
| sc_script | script_library | ⚠️ 命名不一致 |
| sc_violation_word | violation_word | ⚠️ 命名不一致 |
| sc_user_violation_word | sc_user_violation_word | ✅ |
| sc_script_template | sc_script_template | ✅ |
| — | script_check | 设计无，实现有（历史遗留） |

### 3.2 字段差异

**script_library vs sc_script（设计）**

| 设计字段 | 实际字段 | 说明 |
|----------|----------|------|
| owner_id | user_id | 命名不同，语义一致 |
| source | — | 缺失（manual/live/ai） |
| source_id | — | 缺失 |
| persona_id | — | 缺失 |
| style | — | 缺失 |
| duration_hint | — | 缺失 |
| — | use_count | 设计无，实现有 |
| — | status | 设计无，实现有 |

**violation_word vs sc_violation_word（设计）**

| 设计字段 | 实际字段 | 说明 |
|----------|----------|------|
| category (ad_law/platform/sensitive/vulgar) | reason (String) | 分类体系不同 |
| level (forbidden/warning/suggest) | level (Integer 1/2/3) | 枚举不同 |
| scope (all/live_only/video_only) | — | 缺失 |
| description | — | 缺失，用 reason 代替 |

**sc_user_violation_word**

| 设计字段 | 实际字段 | 说明 |
|----------|----------|------|
| owner_id | user_id | 命名不同 |
| scope | — | 缺失 |
| description | — | 缺失，有 reason |

### 3.3 索引

| 设计 | 实际 |
|------|------|
| pg_trgm GIN 索引、按 scope/category/level 的部分索引 | violation_word 为 B-Tree，无 pg_trgm，无 scope 相关索引 |

### 3.4 SQL 文件

- `sql/script/schema.sql`：`script_library`、`violation_word`、`script_check`（旧版结构）
- `sql/script/migration-template-uservw.sql`：`sc_script_template`、`sc_user_violation_word`（新版结构）
- 存在两套表结构并存，设计文档与 schema.sql 不一致

---

## 4. 违规检测核心逻辑

### 4.1 设计要点（03-接口设计、04-违规检测系统）

1. 加载公共违规词库（Caffeine 缓存，5 分钟 TTL）
2. 加载用户个人违规词库（无缓存，直接查询）
3. 按 scope 过滤违规词（all / live / video）
4. 合并两个词库（同一词取更高等级）
5. 遍历文本，对每个违规词做**字符串包含匹配（不区分大小写）**
6. 记录匹配到的位置、等级、替换建议、**来源（public/user）**
7. 返回结果

### 4.2 实际实现（ViolationWordServiceImpl.check）

| 能力 | 设计 | 实现 |
|------|------|------|
| 公共库检测 | ✅ | ✅ 使用 violation_word |
| 个人库合并 | ✅ | ❌ **未合并 UserViolationWord** |
| scope 过滤 | ✅ | ❌ 未实现 |
| Caffeine 缓存 | ✅ | ❌ 无缓存 |
| 大小写不敏感 | ✅ | ❌ 使用 `indexOf()`，**区分大小写** |
| 批量检测 check-batch | ✅ | ❌ 无 |
| 来源标记 source | ✅ | ❌ 结果中无 source（public/user） |

### 4.3 调用链

```
直播场次详情 → 话术 Tab → 「违规检测」→ checkViolation(scriptId)
  → POST /live/ai/check-violation
  → LiveAiServiceImpl.checkViolation()
  → ViolationWordService.check(content)
  → ViolationWordServiceImpl.check() 仅查 violation_word，不查 sc_user_violation_word
```

---

## 5. 前端实现

### 5.1 页面与路由

| 设计页面 | 设计路由 | 实际路由 | 实现情况 |
|----------|----------|----------|----------|
| 话术库 | /talent/script/list | /admin/script/list | ⚠️ 在 admin 下 |
| 违规词库 | /talent/script/violation | /admin/script/violation | ⚠️ 在 admin 下 |
| 违规检测 | /talent/script/check | — | ❌ 无独立页面 |
| 机构话术查看 | /org/script/list | — | ❌ 未实现 |
| 公共违规词管理 | /admin/script/violation | /admin/script/violation | ✅ |
| 话术模板管理 | /admin/script/template | /admin/script/template | ✅ |

### 5.2 达人端（talent）

- `/talent/script`：DataTablePage，仅展示话术列表
- 无 `/talent/script/violation`、`/talent/script/check`
- 设计中的「公共库浏览 + 个人库管理」双 Tab 未实现

### 5.3 违规检测入口

- 无独立违规检测页
- 仅在 `LiveSessionDetailPage` 话术 Tab 中，通过 `checkViolation(scriptId)` 调用 live 的 `/live/ai/check-violation` 做单条检测

### 5.4 前端 API 封装（script.ts）

| 已封装 | 未封装 |
|--------|--------|
| listScripts, saveScript, deleteScript | checkViolation（走 live API） |
| listViolationWords, saveViolationWord, deleteViolationWord | 个人违规词 search/save/delete/listActive |
| searchTemplates | 违规检测 /script/violation/check |
| | AI 替换 suggest-replacement |
| | 公共违规词浏览 /script/violation/public/list |

---

## 6. 错误码

### 6.1 设计（docs/04-错误码注册表）

| 错误码 | 常量名 | 说明 |
|--------|--------|------|
| 3401 | SCRIPT_NOT_FOUND | 话术不存在 |
| 3402 | SCRIPT_FORBIDDEN | 无权操作该话术 |
| 3403 | VIOLATION_WORD_NOT_FOUND | 违规词不存在 |
| 3404 | VIOLATION_WORD_EXISTS | 违规词已存在 |
| 3405 | TEMPLATE_NOT_FOUND | 话术模板不存在 |
| 3406-3407 | TEMPLATE_FORBIDDEN/SYSTEM_FORBIDDEN | 模板权限 |
| 3408 | CSV_FORMAT_ERROR | CSV 格式错误 |
| 3409 | CHECK_TEXT_EMPTY | 检测文本为空 |
| 3410 | CHECK_SCOPE_INVALID | scope 参数无效 |
| 3411 | VIOLATION_IMPORT_FAIL | 导入失败 |

### 6.2 实际使用

- 当前实现多用 `ErrorCode.DATA_NOT_FOUND`、`ErrorCode.VALIDATION_FAIL` 等通用码
- `SCRIPT_TEMPLATE_NOT_FOUND` 在 shortvideo 的 SvScriptTemplateServiceImpl 中使用（3205，非 3405）
- script 模块内未按设计使用 34xx 段

---

## 7. Gap 分析汇总

### 7.1 高优先级（P0）

| Gap | 说明 | 影响 |
|-----|------|------|
| **违规检测未合并个人库** | BR-01 未满足，个人违规词不参与检测 | 用户配置的个人违规词无效 |
| **scope 未实现** | BR-07/BR-08/BR-09 未满足，无法区分直播/短视频场景 | 无法按场景过滤违规词 |
| **违规检测大小写敏感** | BR-06/BR-13 要求不区分大小写 | 「最好」可规避「最好的」检测 |
| **表结构与设计文档差异大** | script_library、violation_word 与设计不一致 | 影响后续升级、迁移 |

### 7.2 中优先级（P1）

| Gap | 说明 |
|-----|------|
| 批量违规检测 check-batch | 设计接口 #6 未实现 |
| CSV 导入/导出 | 设计接口 #18、#19 未实现 |
| 管理端话术模板 admin/template/* | 设计接口 #20-22 未实现 |
| 违规检测独立页 | 设计页面 #3 /talent/script/check 未实现 |
| 达人端话术/违规词页 | 设计在 /talent 下，实际在 /admin |
| shortvideo 未集成违规检测 | 设计 shortvideo 依赖 script 做文案检测 |

### 7.3 低优先级（P2）

| Gap | 说明 |
|-----|------|
| 话术 source/source_id | 无法区分 manual/live/ai 来源 |
| Caffeine 缓存 | 设计有，实现无 |
| pg_trgm 索引 | 设计有，实现无 |
| 错误码 34xx | 未按设计使用 |
| 机构话术查看 SC-09 | 未实现 |
| 从模板创建话术 SC-11 | 流程未完整实现 |
| ScriptCheck 表 | 有 Entity 和表，无业务逻辑，历史遗留 |

### 7.4 潜在问题

1. **数据隔离**：ScriptLibrary 使用 `user_id`，与设计中的 `owner_id` 命名不一致，需确认 DataScopeService 机构查看逻辑正确。
2. **权限**：ViolationWordAdminController 仅校验登录，未显式校验 admin 角色。
3. **API 路径**：个人违规词使用 `/user-violation`，与设计 `/violation/user` 不一致，前端需对应调整。
4. **UserViolationWordRepository**：有 `findByUserIdAndStatusAndDeleted`，但 UserViolationWord 表设计为无 status 字段（migration 中有 status），需核对 Entity 与表结构。

---

## 8. 建议优先级

### 8.1 P0（必须修复）

1. **违规检测合并个人库**：`ViolationWordServiceImpl.check()` 注入 `UserViolationWordService`，按 userId 加载个人违规词，与公共库合并后检测。
2. **大小写不敏感**：使用 `String.toLowerCase()` 或 `StringUtils.containsIgnoreCase` 做匹配。
3. **scope 过滤**：为 `check(String text, String scope)` 增加 scope 参数，过滤 violation_word 和 user_violation_word 的 scope 字段（需先补齐表结构）。

### 8.2 P1（建议补齐）

1. 实现 check-batch 接口
2. 实现 CSV 导入/导出
3. 实现管理端模板接口 admin/template/*
4. 新增违规检测独立页 /talent/script/check
5. 调整达人端路由与菜单，与设计对齐
6. shortvideo 模块集成 script 违规检测

### 8.3 P2（可选优化）

1. 统一表结构与设计文档（或更新设计文档以反映现状）
2. 补充 source、source_id、scope 等字段
3. 引入 Caffeine 缓存
4. 规范 34xx 错误码使用
5. 清理或复用 ScriptCheck

---

## 9. 附录：关键文件索引

| 类型 | 路径 |
|------|------|
| 设计文档 | docs/modules/script/00-大纲.md ~ 08-测试与验收.md |
| 后端 Controller | module/script/controller/*.java |
| 后端 Service | module/script/service/impl/*.java |
| 违规检测实现 | ViolationWordServiceImpl.check() |
| 前端 API | frontend-react/src/api/script.ts |
| 前端页面 | frontend-react/src/pages/script/* |
| SQL | sql/script/schema.sql, migration-template-uservw.sql |
| 错误码 | common/constant/ErrorCode.java (3401-3411) |
