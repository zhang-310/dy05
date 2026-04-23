# 商品模块（product）

## 模块概述

商品库管理与商品话术系统。管理直播带货商品信息，为每个商品生成多风格话术，支持版本管理、效果评分、风格预设。

## 后端结构

```
module/product/
├── controller/
│   ├── ProductController.java                # 商品 CRUD、派品导入、卖点提取
│   ├── ProductScriptController.java          # 商品话术管理
│   ├── ProductScriptVersionController.java   # 话术版本管理
│   ├── EffectivenessScoreController.java     # 效果评分
│   ├── ScriptOptimizationController.java     # 话术优化
│   └── StylePresetController.java            # 风格预设
│
├── entity/（15+ 个）
│   ├── DyProduct.java                        # 商品（dy_product）
│   ├── DyProductScript.java                  # 商品话术（dy_product_script）
│   ├── ProductScriptVersion.java             # 话术版本（product_script_version）
│   ├── ScriptVersionHistory.java             # 话术版本历史（script_version_history）
│   ├── ProductScriptUsage.java               # 话术使用记录
│   ├── ProductScriptEffectivenessRecord.java # 效果评分
│   ├── StylePreset.java                      # 风格预设
│   ├── ScriptOptimizationSuggestion.java     # 优化建议
│   ├── ProductScriptComparisonCache.java     # 对比缓存
│   ├── ProductScriptSnapshot.java            # 话术快照
│   ├── ScriptRegeneratedVersion.java         # 话术再生版本
│   ├── ScriptAnalysisResult.java             # 话术分析结果
│   └── DyProductSalesHistory.java            # 销售历史
│
├── service/（20+ 个）
│   ├── ProductService.java                   # 商品管理
│   ├── ProductScriptService.java             # 话术管理
│   ├── ProductScriptVersionService.java      # 版本管理
│   ├── ProductScriptGenerationService.java   # AI 话术生成
│   ├── EffectivenessScoreService.java        # 效果评分
│   ├── StylePresetService.java               # 风格预设
│   ├── ScriptOptimizationService.java        # 优化建议
│   ├── ProductImportService.java             # 派品导入
│   └── ProductExtractService.java            # 卖点提取
│
└── vo/（35+ 个）
    ├── ProductSaveVO / SearchVO / VO
    ├── ProductScriptSaveVO / VO
    ├── StylePresetSaveVO / VO
    └── ...
```

## 数据库表

| 表名 | 说明 |
|------|------|
| dy_product | 商品 |
| dy_product_script | 商品话术 |
| product_script_version | 话术版本 |
| script_version_history | 话术版本历史 |
| dy_product_script_usage | 话术使用记录 |
| product_script_effectiveness_record | 效果评分 |
| style_preset | 风格预设 |
| dy_optimization_suggestion | 优化建议 |
| product_script_comparison_cache | 对比缓存 |
| product_script_snapshot | 话术快照 |
| dy_script_regenerated_version | 话术再生版本 |
| dy_script_analysis_result | 话术分析结果 |
| dy_product_sales_history | 销售历史 |

SQL 文件：`sql/product/`

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 商品列表 | `pages/crud/ProductPage.tsx` | `/admin/product` |
| 销售记录 | `pages/crud/SalesHistoryPage.tsx` | `/admin/product/sales-history` |
| 风格预设 | `pages/product/StylePresetPage.tsx` | `/admin/product/style-preset` |
| 效果评分 | `pages/product/EffectivenessScorePage.tsx` | `/admin/product/effectiveness` |

## 前端 API

文件：`api/product.ts`、`api/product-script-version.ts`、`api/effectiveness.ts`、`api/optimization.ts`

## API 接口清单（非 POST）

| 接口 | 说明 |
|------|------|
| PUT /api/v1/product/script/activate/{scriptId} | [PUT] 激活话术 |
| PUT /api/v1/product/script/update/{scriptId} | [PUT] 更新话术 |
| DELETE /api/v1/product/script/{scriptId} | [DELETE] 删除话术 |
| DELETE /api/v1/product/style-preset/{id} | [DELETE] 删除风格预设 |

## 前端组件

| 组件 | 文件 | 说明 |
|------|------|------|
| BatchScriptGenerateDialog | `components/product/` | 批量话术生成对话框 |
| ProductScriptManageDialog | `components/product/` | 商品话术管理对话框 |
| ProductImageField | `components/product/` | 商品图片字段 |
| ScriptVersionHistoryDialog | `components/product/` | 话术版本历史对话框 |

## 核心功能

- **多风格话术**：每个商品可生成卖点介绍、痛点击中、场景带入、限时促销等多风格话术
- **版本管理**：话术修改自动记录版本，支持回滚和对比
- **效果评分**：根据使用数据评估话术转化效果
- **批量生成**：支持批量为多商品生成话术，SSE 实时进度
- **派品导入**：支持从 Excel 批量导入商品信息
- **卖点提取**：AI 自动从商品描述/链接提取卖点
