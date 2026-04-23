# Swagger/OpenAPI 快速参考

## 快速开始

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 访问 Swagger UI
```
http://localhost:8188/swagger-ui.html
```

### 3. 查看 OpenAPI 文档
```
http://localhost:8188/v3/api-docs
http://localhost:8188/v3/api-docs.yaml
```

---

## 核心注解速查表

### Controller 级

```java
@Tag(
    name = "模块名 / Module Name",
    description = "模块描述 / Module description"
)
public class UserController { }
```

### 方法级

```java
@Operation(
    summary = "短描述 / Short description",
    description = "详细描述 / Detailed description"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "0", description = "成功 / Success"),
    @ApiResponse(responseCode = "401", description = "未认证 / Unauthorized"),
    @ApiResponse(responseCode = "500", description = "错误 / Error")
})
public RESTResult<UserVO> getUser(
    @Parameter(description = "用户 ID / User ID", required = true)
    @RequestParam Long id
) { }
```

### 请求体

```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "描述 / Description",
    required = true,
    content = @Content(schema = @Schema(implementation = UserVO.class))
)
@RequestBody UserVO vo
```

---

## 常见响应码

| 代码 | 含义 |
|------|------|
| 0 | 成功 Success |
| 400 | 请求参数错误 Bad Request |
| 401 | 未认证 Unauthorized |
| 403 | 禁止访问 Forbidden |
| 404 | 不存在 Not Found |
| 500 | 服务器错误 Server Error |

---

## 权限标记

- **无权限标记**：不需要认证
- **需认证**：需要 JWT Token
- **需认证 + 管理员**：需要 JWT Token 且必须是管理员

---

## 模块 API 路径

| 模块 | 基础路径 |
|------|---------|
| 认证 | /api/v1/auth |
| 用户 | /api/v1/auth/user |
| 资源 | /api/v1/auth/resource |
| 角色 | /api/v1/auth/role |
| 日志 | /api/v1/log |
| 配置 | /api/v1/config |
| 存储 | /api/v1/storage |
| 抖音账号 | /api/v1/douyin/account |
| 抖音视频 | /api/v1/douyin/video |
| 直播场次 | /api/v1/live/session |
| 直播产品 | /api/v1/live/product |
| 直播监控 | /api/v1/live/monitor |
| 直播话术 | /api/v1/live/script |

---

## 认证方式

在 Swagger UI 中添加 JWT Token：

1. 点击右上角的 "Authorize" 按钮
2. 输入 Bearer Token：`Bearer <your_jwt_token>`
3. 所有后续请求都会自动带上认证信息

或在 HTTP 请求头中：
```
Authorization: Bearer <jwt_token>
```

---

## 测试流程

### 1. 获取验证码
```
GET /api/v1/auth/captcha
```

### 2. 登录
```
POST /api/v1/auth/login
Body: {
  "username": "admin",
  "password": "123456",
  "captchaId": "...",
  "captchaCode": "..."
}
```

### 3. 复制获得的 token

### 4. 点击 Authorize，输入 token

### 5. 开始测试其他 API

---

## 常用 API 列表

### 必知的五个 API

1. **登录**
   ```
   POST /api/v1/auth/login
   ```

2. **获取个人资料**
   ```
   POST /api/v1/auth/profile
   ```

3. **获取菜单权限**
   ```
   POST /api/v1/auth/menu/search
   ```

4. **获取资源权限**
   ```
   POST /api/v1/auth/resource/search
   ```

5. **登出**
   ```
   POST /api/v1/auth/logout
   ```

---

## 故障排查

### 问题 1: Swagger UI 不显示

**解决方案**：
1. 检查应用是否成功启动
2. 确认端口号是 8188
3. 清理浏览器缓存
4. 检查 application-swagger.yml 配置

### 问题 2: 无法测试 API

**解决方案**：
1. 确认已进行 Authorize
2. 检查 token 是否过期
3. 查看响应错误信息
4. 检查请求参数是否正确

### 问题 3: 编译错误

**解决方案**：
1. 检查注解包导入是否正确
2. 确认使用的是 io.swagger.v3 而不是 springfox
3. 运行 `mvn clean compile`

---

## 文档链接

- 📖 [详细使用指南](./SWAGGER_DOCUMENTATION.md)
- 📚 [API 参考手册](./API_REFERENCE.md)
- ✅ [完成报告](./P1_PHASE2_COMPLETION_REPORT.md)

---

**最后更新**：2026-02-25
