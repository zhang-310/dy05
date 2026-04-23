# 归因分析模块（attribution）

## 模块概述

直播效果归因分析，追踪话术 → 商品 → GMV 的转化链路，计算每段话术和每个产品的贡献度。

## 后端结构

```
module/attribution/
├── controller/
│   └── AttributionController.java            # /api/v1/attribution
│       POST /trigger       → 触发归因计算
│       POST /session       → 按场次查询归因
│       POST /summary       → 归因摘要
│       POST /get           → 获取单条归因
│       POST /delete        → 按场次删除
├── entity/
│   └── Attribution.java                      # 归因记录
│       字段：session_id, attribution_type, script_id, product_id,
│             contributed_gmv, effect_score, analysis
└── vo/
    └── AttributionTriggerVO                  # 触发参数
```

## 前端 API

文件：`api/attribution.ts`

| 函数 | 说明 |
|------|------|
| triggerAttribution | 触发归因计算 |
| getBySession | 按场次查询 |
| getSummary | 归因摘要 |
| getById | 获取单条 |
| deleteBySession | 按场次删除 |

## API 接口清单（非 POST）

| 接口 | 说明 |
|------|------|
| DELETE /api/v1/attribution/session/{sessionId} | [DELETE] 删除场次归因 |

SQL 文件：`sql/attribution/`
