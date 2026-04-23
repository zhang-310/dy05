# A/B 测试模块（abtest）

## 模块概述

话术效果 A/B 测试框架，支持创建实验、分配变体、记录事件、统计分析（含卡方检验）。

## 后端结构

```
module/abtest/
├── controller/
│   └── AbTestController.java                 # /api/v1/abtest — 实验、变体、事件、统计
├── entity/
│   ├── AbExperiment.java                     # 实验（ab_experiment）
│   ├── AbVariant.java                        # 变体（ab_variant）
│   └── AbEvent.java                          # 事件（ab_event）
├── service/
│   ├── AbTestService / impl/                 # 实验 CRUD、统计、卡方检验、设置赢家
│   └── ScriptStyleAbService / impl/          # 话术风格 A/B 分配与转化记录
└── vo/
    ├── AbExperimentSaveVO / SearchVO / VO
    ├── AbVariantSaveVO / VO
    ├── AbEventSaveVO / AbSetWinnerVO
    ├── AbExperimentStatisticsVO              # 实验统计
    ├── AbDailyTrendVO                        # 日趋势
    ├── AbStatisticalTestVO                   # 卡方检验结果
    ├── AbVariantStatsVO                      # 变体统计
    ├── ScriptStyleAssignRequest / VO         # 话术风格分配
    └── ScriptStyleConversionRequest          # 转化记录
```

SQL 文件：`sql/abtest/`

## 前端页面

| 路由 | 页面 |
|------|------|
| `/admin/abtest` | DataTablePage（通用表格） |

## 前端 API

文件：`api/abtest.ts`

| 函数 | 说明 |
|------|------|
| listExperiments | 实验列表 |
| assignScriptStyle | 话术风格分配 |
| recordScriptStyleConversion | 记录转化 |
