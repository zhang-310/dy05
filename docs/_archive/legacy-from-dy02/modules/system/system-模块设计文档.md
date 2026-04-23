> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# system 模块设计文档

> 版本：1.0 | 更新日期：2026-02-24 | 阶段：P0

---

## 一、需求分析

### 1.1 模块定位

system 模块负责系统级监控和审计，提供 API 调用日志、数据同步记录和系统健康检查。与 log 模块的区别：log 记录业务操作日志，system 记录系统级技术日志。

### 1.2 功能清单

```
system 模块
├── API 调用日志
│   ├── 记录所有外部 API 调用（抖音 API / AI 模型 API）
│   ├── 调用日志查询（按模块/状态/时间筛选）
│   └── 调用统计（成功率、平均耗时、按接口分组）
│
├── 数据同步日志
│   ├── 记录数据同步任务（视频同步/直播数据同步/账号数据同步）
│   ├── 同步状态追踪（成功/失败/进行中）
│   └── 同步日志查询
│
└── 系统监控
    ├── 系统健康检查（数据库连接/AI 服务/存储服务）
    └── 系统信息（JVM 内存/线程数/运行时长）
```

### 1.3 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 自动记录 | 外部 API 调用通过拦截器自动记录 |
| BR-02 | 日志保留 | API 调用日志保留 30 天（可配置），到期自动清理 |
| BR-03 | 敏感信息脱敏 | API Key、Token 等在日志中脱敏显示 |

---

## 二、数据库设计

#### sys_api_call_log — API 调用日志表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| module | VARCHAR(64) | NOT NULL | — | 模块：douyin / ai / live |
| api_name | VARCHAR(256) | NOT NULL | — | API 名称/路径 |
| request_url | VARCHAR(512) | | — | 请求 URL |
| request_method | VARCHAR(16) | | — | 请求方法 |
| request_params | TEXT | | — | 请求参数（脱敏） |
| response_status | INTEGER | | — | 响应状态码 |
| response_body | TEXT | | — | 响应内容（截取，最长 2000 字） |
| status | INTEGER | NOT NULL | 1 | 1=成功 0=失败 |
| error_message | VARCHAR(512) | | — | 错误信息 |
| duration_ms | BIGINT | | — | 耗时（毫秒） |
| user_id | BIGINT | | — | 触发用户 ID |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(module, create_time DESC)`、`(status, create_time DESC)`

#### sys_sync_log — 数据同步日志表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| sync_type | VARCHAR(64) | NOT NULL | — | 同步类型：video_sync / live_data_sync / account_data_sync |
| user_id | BIGINT | | — | 关联用户 ID |
| account_id | BIGINT | | — | 关联账号 ID |
| status | VARCHAR(16) | NOT NULL | 'running' | running / success / failed |
| total_count | INTEGER | | 0 | 总数据量 |
| success_count | INTEGER | | 0 | 成功数量 |
| fail_count | INTEGER | | 0 | 失败数量 |
| error_message | TEXT | | — | 错误信息 |
| start_time | TIMESTAMP | | — | 开始时间 |
| end_time | TIMESTAMP | | — | 结束时间 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(sync_type, create_time DESC)`、`(user_id, create_time DESC)`

---

## 三、接口设计

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 1 | POST | /api/v1/system/api-log/list | 管理员 | API 调用日志列表 |
| 2 | POST | /api/v1/system/api-log/stats | 管理员 | API 调用统计 |
| 3 | POST | /api/v1/system/sync-log/list | 管理员 | 同步日志列表 |
| 4 | POST | /api/v1/system/health | 管理员 | 系统健康检查 |
| 5 | POST | /api/v1/system/info | 管理员 | 系统运行信息 |

### 健康检查响应

```json
{
  "status": 200,
  "data": {
    "database": { "status": "UP", "latency": "5ms" },
    "ollama": { "status": "UP", "latency": "200ms" },
    "milvus": { "status": "UP", "latency": "30ms" },
    "elasticsearch": { "status": "UP", "latency": "15ms" },
    "redis": { "status": "UP", "latency": "2ms" },
    "rabbitmq": { "status": "UP", "latency": "5ms" },
    "storage": { "status": "UP", "provider": "bos" }
  }
}
```

---

## 四、页面设计

| # | 页面 | 路径 | 说明 |
|---|------|------|------|
| 1 | API 调用日志 | /admin/system/api-log.html | 调用日志查询+统计 |
| 2 | 数据同步日志 | /admin/system/sync-log.html | 同步记录查询 |
| 3 | 系统状态 | /admin/system/health.html | 健康检查+系统信息 |

---

## 五、开发任务拆解

| # | 任务 | 依赖 |
|---|------|------|
| B1 | SQL schema.sql | 无 |
| B2 | Entity + Repository | B1 |
| B3 | ApiCallLogService（含拦截器） | B2 |
| B4 | SyncLogService | B2 |
| B5 | SystemHealthService | 无 |
| B6 | SystemController | B3, B4, B5 |
| F1 | API 调用日志页 | B6 |
| F2 | 同步日志页 | B6 |
| F3 | 系统状态页 | B6 |
