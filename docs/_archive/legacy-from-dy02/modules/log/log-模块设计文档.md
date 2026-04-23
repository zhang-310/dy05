> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# log 模块设计文档

> 版本：1.0 | 更新日期：2026-02-24 | 阶段：P0

---

## 一、需求分析

### 1.1 模块定位

log 模块负责操作日志记录，为系统提供审计追踪能力。记录用户的关键业务操作（谁、在什么时间、做了什么），供管理员查询和审计。

### 1.2 功能清单

```
log 模块
├── 操作日志
│   ├── 自动记录关键操作（通过 AOP 或事件机制）
│   ├── 日志查询（按用户/时间/模块/操作类型筛选）
│   ├── 日志详情
│   └── 日志导出
│
└── 系统异常日志
    ├── 记录未捕获异常
    └── 异常日志查询
```

### 1.3 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 只记不删 | 操作日志只追加，不允许删除和修改 |
| BR-02 | 自动记录 | 通过 @OperationLog 注解自动记录 |
| BR-03 | 关键操作 | 登录/注册/修改密码/角色变更/资源变更/封禁等记录 |

---

## 二、数据库设计

#### sys_operation_log — 操作日志表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| user_id | BIGINT | | — | 操作用户 ID |
| username | VARCHAR(64) | | — | 用户名（冗余） |
| module | VARCHAR(64) | NOT NULL | — | 模块：auth / douyin / live ... |
| operation | VARCHAR(128) | NOT NULL | — | 操作描述（如"用户登录""创建直播场次"） |
| method | VARCHAR(256) | | — | 请求方法（如 POST /api/v1/auth/login） |
| request_params | TEXT | | — | 请求参数（脱敏后，不含密码） |
| response_status | INTEGER | | — | 响应状态码 |
| ip | VARCHAR(64) | | — | 操作 IP |
| user_agent | VARCHAR(512) | | — | 浏览器 UA |
| duration_ms | BIGINT | | — | 耗时（毫秒） |
| error_message | TEXT | | — | 异常信息（如有） |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | 操作时间 |

索引：`(user_id, create_time DESC)`、`(module, create_time DESC)`

---

## 三、接口设计

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 1 | POST | /api/v1/log/operation/list | 管理员 | 操作日志列表（分页、筛选） |
| 2 | POST | /api/v1/log/operation/get | 管理员 | 日志详情 |
| 3 | POST | /api/v1/log/operation/export | 管理员 | 导出日志（CSV） |

---

## 四、页面设计

| # | 页面 | 路径 | 说明 |
|---|------|------|------|
| 1 | 操作日志 | /admin/log/operation-list.html | 日志查询列表 |

---

## 五、开发任务拆解

| # | 任务 | 依赖 |
|---|------|------|
| B1 | SQL schema.sql | 无 |
| B2 | Entity + Repository | B1 |
| B3 | OperationLog 注解 + AOP 拦截器 | B2 |
| B4 | LogService + Controller | B3 |
| F1 | 操作日志页面 | B4 |
