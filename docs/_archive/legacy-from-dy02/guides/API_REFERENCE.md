# API 完整参考手册

## 目录

- [认证管理](#认证管理)
- [用户管理](#用户管理)
- [资源管理](#资源管理)
- [角色管理](#角色管理)
- [系统日志](#系统日志)
- [系统配置](#系统配置)
- [文件存储](#文件存储)
- [抖音账号](#抖音账号)
- [抖音视频](#抖音视频)
- [直播场次](#直播场次)
- [直播产品](#直播产品)
- [直播监控](#直播监控)
- [直播话术](#直播话术)

---

## 认证管理

### 1. 获取验证码

**端点**：`GET /api/v1/auth/captcha`

**描述**：生成新的图形验证码用于登录

**参数**：无

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "image": "base64编码的图片"
  }
}
```

**权限要求**：无

---

### 2. 用户登录

**端点**：`POST /api/v1/auth/login`

**描述**：使用用户名密码进行登录并获取 JWT 令牌

**请求体**：
```json
{
  "username": "admin",
  "password": "123456",
  "captchaId": "uuid",
  "captchaCode": "1234"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "token": "jwt_token_string"
  }
}
```

**错误码**：
- `401`: 用户名或密码错误
- `403`: 账号被禁用
- `400`: 验证码错误

---

### 3. 发送短信验证码

**端点**：`POST /api/v1/auth/sms/send`

**描述**：发送短信验证码到指定手机号

**请求体**：
```json
{
  "phone": "13800138000"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "发送成功"
}
```

**错误码**：
- `429`: 请求过于频繁

---

### 4. 忘记密码

**端点**：`POST /api/v1/auth/forgot-password`

**描述**：通过手机验证码重置密码

**请求体**：
```json
{
  "phone": "13800138000",
  "code": "123456",
  "newPassword": "newpass123"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "重置成功"
}
```

---

### 5. 获取个人资料

**端点**：`POST /api/v1/auth/profile`

**描述**：获取当前登录用户的个人资料信息

**权限**：需认证

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "admin",
    "realName": "管理员",
    "phone": "13800138000",
    "email": "admin@example.com"
  }
}
```

---

### 6. 更新个人资料

**端点**：`POST /api/v1/auth/profile/update`

**描述**：更新当前登录用户的个人资料信息

**权限**：需认证

**请求体**：
```json
{
  "realName": "新名字",
  "phone": "13900139000",
  "email": "newemail@example.com"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "更新成功"
}
```

---

### 7. 修改密码

**端点**：`POST /api/v1/auth/profile/change-password`

**描述**：修改当前登录用户的登录密码

**权限**：需认证

**请求体**：
```json
{
  "oldPassword": "oldpass123",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "修改成功"
}
```

**错误码**：
- `400`: 原密码错误

---

### 8. 获取权限菜单

**端点**：`POST /api/v1/auth/menu/search`

**描述**：获取当前登录用户有权限的菜单列表

**权限**：需认证

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "抖音运营",
      "path": "/douyin",
      "icon": "icon-douyin",
      "children": []
    }
  ]
}
```

---

### 9. 获取权限资源

**端点**：`POST /api/v1/auth/resource/search`

**描述**：获取当前登录用户有权限的资源编码列表

**权限**：需认证

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "codes": ["auth:user:search", "auth:user:save"]
  }
}
```

---

### 10. 获取 OAuth 授权 URL

**端点**：`GET /api/v1/auth/oauth/authorize?provider=wechat&state=login`

**描述**：获取第三方平台的授权登录 URL

**参数**：
- `provider`: OAuth 提供者（wechat、qq、douyin、volcano）
- `state`: 状态参数（login 或 bind）

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "url": "https://open.weixin.qq.com/connect/..."
  }
}
```

---

### 11. OAuth 回调处理

**端点**：`GET /api/v1/auth/oauth/callback?code=***&state=***`

**描述**：处理第三方平台的 OAuth 回调

**响应**：重定向到前端回调页面

---

### 12. 获取绑定的 OAuth 提供者

**端点**：`POST /api/v1/auth/oauth/bindings`

**描述**：获取当前登录用户已绑定的第三方平台列表

**权限**：需认证

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": ["wechat", "qq"]
}
```

---

### 13. 绑定 OAuth 账号

**端点**：`POST /api/v1/auth/oauth/bind?provider=wechat&code=***`

**描述**：将第三方账号绑定到当前登录用户

**权限**：需认证

**响应**：
```json
{
  "code": 0,
  "message": "绑定成功",
  "data": {
    "nickname": "微信昵称",
    "avatar": "头像URL"
  }
}
```

---

### 14. 解绑 OAuth 账号

**端点**：`POST /api/v1/auth/oauth/unbind?provider=wechat`

**描述**：解除当前登录用户与第三方账号的绑定关系

**权限**：需认证

**响应**：
```json
{
  "code": 0,
  "message": "解绑成功"
}
```

---

### 15. 用户登出

**端点**：`POST /api/v1/auth/logout`

**描述**：登出当前用户并撤销其 JWT 令牌

**权限**：需认证

**响应**：
```json
{
  "code": 0,
  "message": "登出成功"
}
```

---

## 用户管理

### 1. 查询用户列表

**端点**：`POST /api/v1/auth/user/search`

**权限**：需认证 + 管理员

**请求体**：
```json
{
  "username": "admin",
  "page": 1,
  "rows": 10
}
```

**响应**：
```json
{
  "code": 0,
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "rows": 10
  }
}
```

---

### 2. 获取用户详情

**端点**：`POST /api/v1/auth/user/get`

**权限**：需认证 + 管理员

**请求体**：
```json
{
  "id": 1
}
```

---

### 3. 保存用户

**端点**：`POST /api/v1/auth/user/save`

**权限**：需认证 + 管理员

**请求体**：
```json
{
  "id": null,
  "username": "newuser",
  "password": "123456",
  "realName": "新用户",
  "roleIds": [1, 2]
}
```

---

### 4. 禁用/启用用户

**端点**：`POST /api/v1/auth/user/ban?userId=1&ban=true&reason=违规`

**权限**：需认证 + 管理员

---

### 5. 获取用户登录日志

**端点**：`POST /api/v1/auth/user/login-logs`

**权限**：需认证 + 管理员

**请求体**：
```json
{
  "userId": 1,
  "page": 1,
  "size": 10
}
```

---

### 6. 获取在线用户列表

**端点**：`POST /api/v1/auth/user/online`

**权限**：需认证 + 管理员

**请求体**：
```json
{
  "withinMinutes": 30,
  "maxSize": 100
}
```

---

## 资源管理

### 1. 查询资源列表

**端点**：`POST /api/v1/auth/resource/list`

**权限**：需认证 + 管理员

---

### 2. 获取资源树结构

**端点**：`POST /api/v1/auth/resource/tree`

**权限**：需认证 + 管理员

---

### 3. 获取资源详情

**端点**：`POST /api/v1/auth/resource/get`

**权限**：需认证 + 管理员

---

### 4. 保存资源

**端点**：`POST /api/v1/auth/resource/save`

**权限**：需认证 + 管理员

---

### 5. 删除资源

**端点**：`POST /api/v1/auth/resource/delete`

**权限**：需认证 + 管理员

---

## 角色管理

### 1. 查询角色列表

**端点**：`POST /api/v1/auth/role/search`

**权限**：需认证 + 管理员

---

### 2. 获取所有角色

**端点**：`POST /api/v1/auth/role/list`

**权限**：需认证 + 管理员

---

### 3. 获取角色详情

**端点**：`POST /api/v1/auth/role/get`

**权限**：需认证 + 管理员

---

### 4. 保存角色

**端点**：`POST /api/v1/auth/role/save`

**权限**：需认证 + 管理员

---

### 5. 删除角色

**端点**：`POST /api/v1/auth/role/delete`

**权限**：需认证 + 管理员

---

### 6. 获取角色的资源权限

**端点**：`POST /api/v1/auth/role/resources`

**权限**：需认证 + 管理员

---

### 7. 授予角色资源权限

**端点**：`POST /api/v1/auth/role/resources/save`

**权限**：需认证 + 管理员

---

## 系统日志

### 1. 查询操作日志

**端点**：`POST /api/v1/log/operation/page`

**权限**：需认证

---

### 2. 查询系统日志

**端点**：`POST /api/v1/log/system/page`

**权限**：需认证

---

### 3. 导出操作日志 CSV

**端点**：`POST /api/v1/log/operation/export`

**权限**：需认证

**响应**：CSV 文件下载

---

### 4. 导出系统日志 CSV

**端点**：`POST /api/v1/log/system/export`

**权限**：需认证

**响应**：CSV 文件下载

---

## 系统配置

### 1. 查询配置列表

**端点**：`POST /api/v1/config/list`

**权限**：需认证 + 管理员

---

### 2. 按 Key 获取配置

**端点**：`GET /api/v1/config/get?key=storage.bos.bucket`

**权限**：需认证 + 管理员

---

### 3. 保存配置

**端点**：`POST /api/v1/config/save`

**权限**：需认证 + 管理员

---

### 4. 删除配置

**端点**：`POST /api/v1/config/delete`

**权限**：需认证 + 管理员

---

## 文件存储

### 1. 检查 BOS 配置

**端点**：`GET /api/v1/storage/configured`

**权限**：需认证 + 管理员

---

### 2. 列出存储文件

**端点**：`POST /api/v1/storage/list`

**权限**：需认证 + 管理员

**请求体**：
```json
{
  "prefix": "uploads/"
}
```

---

### 3. 上传文件

**端点**：`POST /api/v1/storage/upload`

**权限**：需认证 + 管理员

**参数**：
- `file`: 上传文件
- `prefix`: 存储路径前缀（可选）

**响应**：
```json
{
  "code": 0,
  "data": {
    "key": "uploads/filename.txt",
    "url": "https://cdn.example.com/uploads/filename.txt"
  }
}
```

---

### 4. 删除文件

**端点**：`POST /api/v1/storage/delete`

**权限**：需认证 + 管理员

**请求体**：
```json
{
  "key": "uploads/filename.txt"
}
```

---

### 5. 获取文件 URL

**端点**：`GET /api/v1/storage/url?key=uploads/filename.txt`

**权限**：需认证 + 管理员

---

## 抖音账号

### 1. 查询抖音账号

**端点**：`POST /api/v1/douyin/account/search`

**权限**：需认证

---

### 2. 获取账号详情

**端点**：`POST /api/v1/douyin/account/get?id=1`

**权限**：需认证

---

### 3. 保存账号

**端点**：`POST /api/v1/douyin/account/save`

**权限**：需认证

---

### 4. 删除账号

**端点**：`POST /api/v1/douyin/account/delete?id=1`

**权限**：需认证

---

### 5. 获取账号统计

**端点**：`POST /api/v1/douyin/account/statistics?id=1`

**权限**：需认证

---

## 抖音视频

### 1. 查询视频列表

**端点**：`POST /api/v1/douyin/video/search`

**权限**：需认证

---

### 2. 获取视频详情

**端点**：`POST /api/v1/douyin/video/get?id=1`

**权限**：需认证

---

### 3. 保存视频

**端点**：`POST /api/v1/douyin/video/save`

**权限**：需认证

---

### 4. 同步视频

**端点**：`POST /api/v1/douyin/video/sync?accountId=1`

**权限**：需认证

---

## 直播场次

### 1. 查询场次列表

**端点**：`POST /api/v1/live/session/search`

**权限**：需认证

---

### 2. 获取场次详情

**端点**：`POST /api/v1/live/session/get?id=1`

**权限**：需认证

---

### 3. 保存场次

**端点**：`POST /api/v1/live/session/save`

**权限**：需认证

---

### 4. 删除场次

**端点**：`POST /api/v1/live/session/delete?id=1`

**权限**：需认证

---

### 5. 更新场次状态

**端点**：`POST /api/v1/live/session/status?id=1&status=1`

**权限**：需认证

---

### 6. 更新观看人数

**端点**：`POST /api/v1/live/session/viewers?id=1&viewers=1000`

**权限**：需认证

---

### 7. 更新点赞数

**端点**：`POST /api/v1/live/session/likes?id=1&likes=500`

**权限**：需认证

---

## 直播产品

### 1. 查询产品列表

**端点**：`POST /api/v1/live/product/search`

**权限**：需认证

---

### 2. 获取产品详情

**端点**：`POST /api/v1/live/product/get?id=1`

**权限**：需认证

---

### 3. 保存产品

**端点**：`POST /api/v1/live/product/save`

**权限**：需认证

---

### 4. 删除产品

**端点**：`POST /api/v1/live/product/delete?id=1`

**权限**：需认证

---

### 5. 获取场次的产品

**端点**：`POST /api/v1/live/product/by-session?sessionId=1`

**权限**：需认证

---

## 直播监控

### 1. 查询监控数据

**端点**：`POST /api/v1/live/monitor/search`

**权限**：需认证

---

### 2. 保存监控数据

**端点**：`POST /api/v1/live/monitor/save`

**权限**：需认证

---

### 3. 获取场次的监控数据

**端点**：`POST /api/v1/live/monitor/by-session?sessionId=1`

**权限**：需认证

---

## 直播话术

### 1. 查询话术列表

**端点**：`POST /api/v1/live/script/search`

**权限**：需认证

---

### 2. 获取话术详情

**端点**：`POST /api/v1/live/script/get?id=1`

**权限**：需认证

---

### 3. 保存话术

**端点**：`POST /api/v1/live/script/save`

**权限**：需认证

---

### 4. 删除话术

**端点**：`POST /api/v1/live/script/delete?id=1`

**权限**：需认证

---

### 5. 获取场次的话术

**端点**：`POST /api/v1/live/script/by-session?sessionId=1`

**权限**：需认证

---

### 6. 更新话术执行状态

**端点**：`POST /api/v1/live/script/executed?id=1&executed=1`

**权限**：需认证

---

## 通用响应格式

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "xxx-xxx-xxx"
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null,
  "traceId": "xxx-xxx-xxx"
}
```

### 错误码列表

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证/令牌无效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 认证方式

所有需要认证的 API 都需要在 HTTP 请求头中添加 JWT 令牌：

```
Authorization: Bearer <jwt_token>
```

---

**最后更新**：2026-02-25
**文档版本**：1.0.0
