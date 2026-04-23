# 抖音直播数据 API 对接配置指南

> 需在抖音开放平台申请「直播数据」能力，配置完成后可调用本接口同步直播汇总数据与分产品数据。

---

## 一、前置条件

1. **抖音开放平台**：https://developer.open-douyin.com/
2. **应用创建**：需已创建应用并获取 `client_key`、`client_secret`
3. **能力申请**：申请「直播数据」相关能力（需企业认证）
4. **OAuth 授权**：用户需完成抖音账号授权，获取 `access_token`

---

## 二、配置项

在 `application.yml` 或 `application-dev.yml` 中配置：

```yaml
douyin:
  api:
    base-url: https://open.douyin.com
    client-key: ${DOUYIN_CLIENT_KEY}      # 应用 client_key
    client-secret: ${DOUYIN_CLIENT_SECRET} # 应用 client_secret
    callback-url: ${DOUYIN_CALLBACK_URL}   # OAuth 回调地址
```

环境变量建议使用 `DOUYIN_CLIENT_KEY`、`DOUYIN_CLIENT_SECRET` 避免硬编码。

---

## 三、数据同步接口

### 3.1 从抖音 API 同步

```
POST /api/v1/live/data/session/sync-from-douyin
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | Long | 是 | 场次 ID |
| roomId | String | 是 | 直播间 room_id（抖音返回） |
| accessToken | String | 是 | 账号 access_token（抖音 OAuth 获取） |

**说明：** 同步成功后写入 `live_session_data`、`live_product_data`。

### 3.2 从 LiveMonitor 聚合（无需抖音 API）

```
POST /api/v1/live/data/session/sync?sessionId={id}
```

当未配置抖音 API 或暂无 access_token 时，可使用此接口从 `live_monitor` 时序数据聚合。

---

## 四、room_id 获取

- 直播进行中：`room_id` 可从抖音直播间 URL 或开放平台返回的直播间信息中获取
- 需在 `live_session` 中存储 `room_id` 或从 `live_url` 解析（具体格式以抖音文档为准）

---

## 五、access_token 获取

1. 用户通过 OAuth 授权绑定抖音账号
2. 系统将 `access_token` 存入 `oauth_token` 表
3. 调用同步接口时，可从当前登录用户关联的账号获取 token（需额外对接）

**当前实现**：接口需手动传入 `accessToken`，待 OAuth 与账号关联完善后可自动获取。

---

## 六、错误码

| 错误码 | 说明 |
|--------|------|
| 3311 | LIVE_DATA_SYNC_FAIL | 抖音 API 同步失败 |

---

## 七、参考文档

- 抖音开放平台：https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/openapi/
- 直播数据 API：需申请能力后查看具体接口文档
