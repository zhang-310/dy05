# douyin 模块 — 账号管理与 OAuth2 系统

> 版本：3.0 | 更新日期：2026-02-26

---

## 4.1 OAuth2 完整授权流程

### 4.1.1 流程概览

```
用户                    本平台                     抖音开放平台
 │                        │                           │
 │  1. 点击"绑定抖音号"    │                           │
 │ ──────────────────────→│                           │
 │                        │  2. 生成 state，存缓存      │
 │                        │  3. 构建授权 URL            │
 │  4. 302 重定向          │                           │
 │ ←──────────────────────│                           │
 │                        │                           │
 │  5. 用户在抖音页面授权   │                           │
 │ ──────────────────────────────────────────────────→│
 │                        │                           │
 │                        │  6. 回调 callback?code&state│
 │                        │ ←─────────────────────────│
 │                        │                           │
 │                        │  7. 验证 state              │
 │                        │  8. code 换 access_token    │
 │                        │ ──────────────────────────→│
 │                        │  9. 返回 token + open_id    │
 │                        │ ←─────────────────────────│
 │                        │                           │
 │                        │  10. 获取用户信息           │
 │                        │ ──────────────────────────→│
 │                        │  11. 返回昵称/头像/粉丝数   │
 │                        │ ←─────────────────────────│
 │                        │                           │
 │                        │  12. 创建/更新 dy_account   │
 │                        │  13. 写入 dy_account_data   │
 │                        │                           │
 │  14. 重定向到绑定成功页  │                           │
 │ ←──────────────────────│                           │
```

### 4.1.2 发起授权（步骤 1-4）

```
GET /api/v1/douyin/oauth/authorize
```

**处理逻辑：**

1. 从 AuthTokenFilter 获取当前 userId
2. 检查绑定数量限制（BR-01）：
   - 查询 `SELECT COUNT(*) FROM dy_account WHERE owner_id=? AND deleted=0 AND auth_status!=2`
   - 从 sys_config 读取套餐上限
   - 超限则返回错误 3104
3. 生成 state = UUID.randomUUID()
4. 缓存 state → userId 映射（Redis/内存，TTL=10分钟）
5. 构建授权 URL：

```
https://open.douyin.com/platform/oauth/connect
  ?client_key={DOUYIN_APP_ID}
  &response_type=code
  &scope=user_info
  &redirect_uri={DOUYIN_REDIRECT_URI}
  &state={state}
```

6. 返回 302 重定向

### 4.1.3 回调处理（步骤 6-14）

```
GET /api/v1/douyin/oauth/callback?code={code}&state={state}
```

**处理逻辑：**

1. **验证 state**：从缓存中查找 state 对应的 userId，找不到则重定向到失败页
2. **code 换 token**：

```
POST https://open.douyin.com/oauth/access_token/
Content-Type: application/json

{
  "client_key": "{DOUYIN_APP_ID}",
  "client_secret": "{DOUYIN_APP_SECRET}",
  "code": "{code}",
  "grant_type": "authorization_code"
}
```

响应字段：
| 字段 | 说明 |
|------|------|
| access_token | 访问令牌 |
| expires_in | 过期秒数（通常 86400 = 24h） |
| refresh_token | 刷新令牌 |
| refresh_expires_in | 刷新令牌过期秒数（通常 2592000 = 30天） |
| open_id | 用户唯一标识 |

3. **获取用户信息**：

```
GET https://open.douyin.com/oauth/userinfo/
  ?access_token={access_token}
  &open_id={open_id}
```

4. **创建/更新账号**：
   - 通过 `owner_id + open_id` 查找已有记录
   - 存在则更新 token 和用户信息
   - 不存在则创建新记录
5. **同步数据快照**：写入当天的 dy_account_data
6. **清理 state 缓存**
7. **重定向到前端**：`/talent/douyin/bind?result=success&accountId={id}`

---

## 4.2 Token 管理

### 4.2.1 Token 生命周期

```
┌─────────────────────────────────────────────────────────────┐
│                    Token 生命周期                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  access_token                                               │
│  ├── 有效期：24 小时（抖音默认）                               │
│  ├── 用途：调用抖音 API（获取用户信息、视频数据等）              │
│  ├── 存储：dy_account.access_token                           │
│  └── 过期时间：dy_account.token_expire_time                  │
│                                                             │
│  refresh_token                                              │
│  ├── 有效期：30 天（抖音默认）                                │
│  ├── 用途：刷新 access_token                                 │
│  ├── 存储：dy_account.refresh_token                          │
│  ├── 过期时间：dy_account.refresh_expire_time                │
│  └── 注意：每次刷新后 refresh_token 也会更新                  │
│                                                             │
│  时间线：                                                    │
│  ├── T+0h    授权成功，获取 access_token + refresh_token     │
│  ├── T+23h   定时任务检测到即将过期，自动刷新                  │
│  ├── T+24h   旧 access_token 过期（已被刷新，不影响）         │
│  ├── T+30d   refresh_token 过期                              │
│  └── T+30d+  需要用户重新 OAuth2 授权                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2.2 Token 存储安全

| 措施 | 说明 |
|------|------|
| 数据库存储 | access_token / refresh_token 存储在 dy_account 表中 |
| 接口不返回 | 任何查询接口不返回 token 原文（VO 中不包含 token 字段） |
| 传输加密 | 全站 HTTPS |
| 后续增强 | 可考虑 AES 加密存储（application.yml 配置加密密钥） |

---

## 4.3 Token 自动刷新定时任务

### 4.3.1 定时任务设计

```java
@Scheduled(cron = "${DOUYIN_TOKEN_REFRESH_CRON:0 0 * * * ?}")
public void refreshExpiredTokens() {
    // 1. 查询即将过期的账号（1小时内过期 + auth_status=1）
    // 2. 逐个刷新 access_token
    // 3. 刷新失败的标记 auth_status=0
}
```

**查询条件：**

```sql
SELECT * FROM dy_account
WHERE deleted = 0
  AND auth_status = 1
  AND token_expire_time < NOW() + INTERVAL '1 hour'
  AND refresh_expire_time > NOW()
```

### 4.3.2 刷新流程

```
定时任务（每小时）
    │
    ├── 查询即将过期的账号列表
    │
    ├── 遍历每个账号：
    │   │
    │   ├── refresh_token 未过期？
    │   │   ├── 是 → 调用抖音刷新 API
    │   │   │       ├── 成功 → 更新 access_token + token_expire_time
    │   │   │       │         更新 refresh_token + refresh_expire_time
    │   │   │       └── 失败 → 重试 1 次，仍失败则标记 auth_status=0
    │   │   │
    │   │   └── 否 → 标记 auth_status=0（过期）
    │   │
    │   └── 记录刷新日志
    │
    └── 完成
```

### 4.3.3 刷新 API

```
POST https://open.douyin.com/oauth/refresh_token/
Content-Type: application/json

{
  "client_key": "{DOUYIN_APP_ID}",
  "grant_type": "refresh_token",
  "refresh_token": "{refresh_token}"
}
```

**响应：** 返回新的 access_token / refresh_token / expires_in / refresh_expires_in

---

## 4.4 账号数据同步机制

### 4.4.1 同步策略

| 触发方式 | 时机 | 说明 |
|---------|------|------|
| 自动同步 | 每天凌晨 2:30 | 定时任务，同步所有 auth_status=1 的账号 |
| 手动同步 | 用户点击"同步数据" | 接口 6：/api/v1/douyin/account/sync |
| 绑定时同步 | OAuth2 回调成功后 | 首次绑定立即同步 |

### 4.4.2 同步内容

```
抖音 API → dy_account（更新基本信息）
         → dy_account_data（写入当日快照）
```

| 同步字段 | 来源 API | 写入表 |
|---------|---------|--------|
| nickname | /oauth/userinfo/ | dy_account.nickname |
| avatar | /oauth/userinfo/ | dy_account.avatar |
| follower_count | /oauth/userinfo/ | dy_account_data.follower_count |
| following_count | /oauth/userinfo/ | dy_account_data.following_count |
| total_favorited | /oauth/userinfo/ | dy_account_data.total_favorited |
| video_count | /oauth/userinfo/ | dy_account_data.video_count |

### 4.4.3 数据快照

每日同步时写入 dy_account_data 一条记录（snapshot_date = 当天日期），用于数据趋势分析：

```sql
-- 查询最近 30 天粉丝趋势
SELECT snapshot_date, follower_count
FROM dy_account_data
WHERE account_id = ?
  AND snapshot_date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY snapshot_date ASC
```

---

## 4.5 账号解绑处理

### 4.5.1 解绑流程

```
用户点击"解除绑定"
    │
    ├── 确认弹窗（二次确认）
    │
    ├── 调用 POST /api/v1/douyin/account/unbind
    │
    ├── 后端处理：
    │   ├── 校验账号属于当前用户
    │   ├── 设置 auth_status = 2（已解绑）
    │   ├── 清空 access_token / refresh_token
    │   ├── 清空 token_expire_time / refresh_expire_time
    │   └── 更新 update_time
    │
    ├── 不删除的数据（BR-07）：
    │   ├── dy_account 记录保留（标记已解绑）
    │   ├── dy_account_data 历史快照保留
    │   ├── dy_persona 人设保留
    │   ├── dy_product 产品保留
    │   ├── dy_product_script 话术保留
    │   ├── sv_video 短视频数据保留
    │   └── live_session 直播数据保留
    │
    └── 解绑后影响：
        ├── 无法同步新数据
        ├── 无法调用抖音 API
        ├── 已有数据仍可查看
        └── 可重新授权绑定（同一 open_id 会更新已有记录）
```

### 4.5.2 重新绑定

用户解绑后可重新发起 OAuth2 授权。如果 open_id 相同，回调处理时会更新已有的 dy_account 记录（而非创建新记录），auth_status 恢复为 1。

---

## 4.6 授权状态管理

### 4.6.1 状态定义

| auth_status | 含义 | 触发条件 | 可用操作 |
|-------------|------|---------|---------|
| 1 | 正常 | OAuth2 授权成功 / Token 刷新成功 | 全部功能可用 |
| 0 | 过期 | refresh_token 过期 / 刷新失败 | 查看历史数据，无法同步新数据 |
| 2 | 已解绑 | 用户主动解除绑定 | 查看历史数据，需重新授权 |

### 4.6.2 状态流转

```
                    OAuth2 授权成功
                         │
                         ▼
                    ┌─────────┐
          ┌────────│  正常(1)  │←──────────┐
          │        └─────────┘            │
          │             │                 │
   用户主动解绑    refresh_token 过期    重新授权成功
          │        或刷新失败              │
          ▼             │                 │
    ┌──────────┐   ┌─────────┐           │
    │ 已解绑(2) │   │ 过期(0)  │───────────┘
    └──────────┘   └─────────┘
          │                               │
          └───────── 重新授权成功 ──────────┘
```

### 4.6.3 前端状态展示

| auth_status | 展示 | 操作按钮 |
|-------------|------|---------|
| 1（正常） | 绿色标签"授权正常" | [刷新授权] [同步数据] [解除绑定] |
| 0（过期） | 红色标签"授权过期" | [重新授权] [解除绑定] |
| 2（已解绑） | 灰色标签"已解绑" | [重新授权] |

---

## 4.7 抖音开放平台 API 对接说明

### 4.7.1 使用的 API 列表

| API | 用途 | 调用时机 |
|-----|------|---------|
| `/platform/oauth/connect` | 发起授权 | 用户点击绑定 |
| `/oauth/access_token/` | code 换 token | OAuth2 回调 |
| `/oauth/refresh_token/` | 刷新 token | 定时任务 / 手动刷新 |
| `/oauth/userinfo/` | 获取用户信息 | 绑定时 / 数据同步 |

### 4.7.2 错误处理

| 抖音错误码 | 含义 | 本平台处理 |
|-----------|------|-----------|
| 0 | 成功 | 正常处理 |
| 10002 | 参数错误 | 记录日志，返回 3103 |
| 10003 | access_token 无效 | 尝试刷新 token |
| 10006 | code 已使用 | 提示用户重新授权 |
| 10008 | refresh_token 无效 | 标记 auth_status=0 |
| 10010 | 应用未审核通过 | 记录日志，通知管理员 |

### 4.7.3 配置项

```yaml
# application.yml
douyin:
  app-id: ${DOUYIN_APP_ID}
  app-secret: ${DOUYIN_APP_SECRET}
  redirect-uri: ${DOUYIN_REDIRECT_URI}
  api-base-url: https://open.douyin.com
  token-refresh-cron: ${DOUYIN_TOKEN_REFRESH_CRON:0 0 * * * ?}
  data-sync-cron: ${DOUYIN_DATA_SYNC_CRON:0 30 2 * * ?}
```
