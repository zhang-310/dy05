# 话术库模块（script）

## 模块概述

独立话术库管理系统，提供话术模板、合规检查（违禁词检测）、混合搜索等能力。与直播话术和商品话术模块协同。

## 后端结构

```
module/script/
├── controller/
│   ├── ScriptController.java                 # 话术 CRUD
│   ├── ScriptGenerationController.java       # 话术生成
│   ├── ScriptTemplateController.java         # 话术模板（用户端）
│   ├── ScriptTemplateAdminController.java    # 话术模板（管理端）
│   ├── ComplianceController.java             # 合规检查
│   ├── ComplianceWordAdminController.java    # 违禁词管理（管理端）
│   ├── ViolationWordAdminController.java     # 违禁词管理
│   ├── UserViolationWordController.java      # 用户自定义违禁词
│   └── HybridSearchController.java           # 混合搜索
│
├── entity/（15+ 个）
│   ├── Script.java                           # 话术（script_library）
│   ├── ScriptTemplate.java                   # 话术模板
│   ├── ScriptGeneration.java                 # 生成记录
│   ├── ViolationWord.java                    # 违禁词（violation_word）
│   ├── UserViolationWord.java                # 用户违禁词
│   ├── ComplianceWord.java                   # 合规词库
│   └── ...
│
├── service/（15+ 个）
│   ├── ScriptService.java                    # 话术管理
│   ├── ScriptTemplateService.java            # 模板管理
│   ├── ScriptGenerationService.java          # 话术生成
│   ├── ComplianceCheckService.java           # 合规检查
│   ├── ViolationWordService.java             # 违禁词管理
│   ├── HybridSearchService.java              # 混合搜索（ES + 向量）
│   └── ...
│
└── vo/（20+ 个）
    ├── ScriptSaveVO / SearchVO / VO
    ├── ScriptTemplateSaveVO / SearchVO / VO
    └── ...
```

## 数据库表

| 表名 | 说明 |
|------|------|
| script_library | 话术 |
| sc_template | 话术模板 |
| sc_generation | 生成记录 |
| violation_word | 违禁词 |
| sc_user_violation_word | 用户自定义违禁词 |
| sc_compliance_word | 合规词库 |
| sc_search_suggestion | 搜索建议 |
| sc_search_result | 搜索结果 |
| sc_search_analytics | 搜索分析 |
| script_check | 话术检查 |
| sc_script_vector_embedding | 话术向量嵌入 |

SQL 文件：`sql/script/`

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 话术列表 | `pages/script/AdminScriptPage.tsx` | `/admin/script/list` |
| 违禁词管理 | `pages/script/AdminViolationWordPage.tsx` | `/admin/script/violation` |
| 话术模板 | `pages/script/AdminScriptTemplatePage.tsx` | `/admin/script/template` |
| 违禁词检查 | `pages/script/ViolationCheckPage.tsx` | `/talent/script/check` |
| 违禁词库 | `pages/script/ViolationWordLibraryPage.tsx` | `/talent/script/violation` |
| 混合搜索 | `pages/script/HybridSearchPage.tsx` | `/admin/script/search` |
| 话术生成 | `pages/script/ScriptGenerationPage.tsx` | `/admin/script/generation` |
| 话术优化 | `pages/script/ScriptOptimizationPage.tsx` | `/admin/script/optimization` |

## 前端 API

文件：`api/script.ts`、`api/search.ts`

## 前端组件

| 组件 | 文件 | 说明 |
|------|------|------|
| ScriptEditor | `components/script/ScriptEditor.tsx` | 话术编辑器 |
| HybridSearchBar | `components/HybridSearchBar.tsx` | 混合搜索栏 |
| HybridSearchResults | `components/HybridSearchResults.tsx` | 搜索结果 |
| SearchSuggestions | `components/SearchSuggestions.tsx` | 搜索建议 |
| SearchResultsList | `components/SearchResultsList.tsx` | 搜索结果列表 |
