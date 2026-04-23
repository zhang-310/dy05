# script 模块升级指南

> 升级日期：2026-03-03

---

## 升级前必做：执行迁移

在部署前**必须**执行以下 SQL：

```bash
# 1. 违规词 scope 字段
psql -U your_user -d your_db -f sql/script/migration-scope.sql

# 2. 话术库 source/source_id 字段
psql -U your_user -d your_db -f sql/script/migration-script-source.sql
```

或通过 Docker：`Get-Content sql/script/migration-scope.sql | docker exec -i dy-postgres psql -U postgres -d douyin_operations`

---

## 升级内容概览

### P0（必须）

| 项目 | 说明 |
|------|------|
| 合并个人违规词检测 | 违规检测同时使用公共库 + 个人库 |
| 大小写不敏感 | 匹配时使用 `toLowerCase()` |
| scope 过滤 | 支持 all / live / video 场景过滤 |

### P1

| 项目 | 说明 |
|------|------|
| check-batch | `POST /api/v1/script/violation/check-batch` 批量检测 |
| CSV 导入 | `POST /api/v1/script/admin/violation/import` |
| CSV 导出 | `POST /api/v1/script/admin/violation/export` |
| 违规检测独立页 | `/admin/script/check`、`/talent/script/check` |
| 达人端路由 | 达人端增加「违规检测」菜单 |

### P2

| 项目 | 说明 |
|------|------|
| Caffeine 缓存 | 公共违规词 5 分钟 TTL，save/delete/import 时失效 |
| 错误码 | 使用 VIOLATION_WORD_NOT_FOUND(3403)、VIOLATION_WORD_EXISTS(3404) |

### 全面升级（2026-03-03 追加）

| 项目 | 说明 |
|------|------|
| 管理端话术模板 | admin/template/list、save、delete |
| shortvideo 违规检测 | POST /short-video/ai/check-violation（scope=video） |
| 话术库 source/source_id | 区分 manual/live/ai 来源 |
| 违规词库双 Tab 页 | 达人端：公共库浏览 + 个人库管理 |
| 达人端违规词库 | /talent/script/violation |

---

## API 变更

### 违规检测接口

- **请求**：`ViolationCheckVO` 增加可选 `scope`（默认 all）
- **响应**：`ViolationHitVO` 增加 `source`（public/user）

### Live 模块

- `LiveAiService.checkViolation(Long scriptId)` → `checkViolation(Long userId, Long scriptId)`
- 内部调用 `ViolationWordService.check(text, scope, userId)` 时传入 scope="live"

---

## 前端变更

- 新增 `ViolationCheckPage`：违规检测独立页
- 新增 `checkViolation`、`checkViolationBatch` API 封装
- Admin 话术菜单增加「违规检测」
- Talent 菜单增加「违规检测」
