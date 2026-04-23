# 文案库模块（copy）

## 模块概述

文案资料管理系统，包含文案库（收藏/管理文案）、文案模板和文案审批流程。

## 后端结构

```
module/copy/
├── controller/
│   ├── CopyLibraryController.java            # 文案库 CRUD
│   ├── CopyTemplateController.java           # 文案模板 CRUD
│   ├── CopyApprovalController.java           # 文案审批
│   └── CopyAiController.java                # AI 文案生成
│
├── entity/
│   ├── CopyLibrary.java                      # 文案（copy_library）
│   ├── CopyTemplate.java                     # 文案模板（copy_template）
│   └── CopyApproval.java                     # 文案审批（copy_approval）
│
├── service/
│   ├── CopyLibraryService / Impl             # 文案管理
│   ├── CopyTemplateService / Impl            # 模板管理
│   └── CopyApprovalService / Impl            # 审批管理
│
└── vo/
    ├── CopyLibrarySaveVO / SearchVO / VO
    ├── CopyTemplateSaveVO / SearchVO / VO
    └── CopyApprovalSaveVO / SearchVO / VO
```

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 文案管理（Tab 页） | `pages/copy/CopyPage.tsx` | `/admin/copy` |
| — 文案库 Tab | CopyLibraryPage | `?tab=library` |
| — 审批 Tab | CopyApprovalPage | `?tab=approval` |
| — 模板 Tab | CopyTemplatePage | `?tab=template` |

## 前端 API

文件：`api/copy.ts`

SQL 文件：`sql/copy/`
