# Swagger/OpenAPI 文档配置指南

## 概述

本文档说明了如何在抖音运营后台项目中使用 Swagger/OpenAPI 进行 API 文档生成。项目已集成 `springdoc-openapi`，提供自动 API 文档生成功能。

## 依赖信息

### Maven 依赖

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

## 配置文件

### 1. application-swagger.yml

位置：`src/main/resources/application-swagger.yml`

主要配置项：
- `springdoc.swagger-ui.path`: Swagger UI 访问路径，默认 `/swagger-ui.html`
- `springdoc.api-docs.path`: OpenAPI JSON 文档路径，默认 `/v3/api-docs`
- `springdoc.packages-to-scan`: 需要扫描的包路径
- `springdoc.paths-to-match`: 需要匹配的路径模式

### 2. SwaggerConfig.java

位置：`src/main/java/cn/gaifan/douyinOperations/common/config/SwaggerConfig.java`

配置 OpenAPI 全局信息，包括：
- API 标题和描述（中英文）
- 版本号
- 联系方式
- 许可证
- 安全认证方案（JWT Bearer Token）

## 核心注解说明

### 全局注解

#### @OpenAPIDefinition（已在 SwaggerConfig 中配置）
用于全局定义 API 文档信息。

#### @SecurityScheme
定义安全认证方式（JWT Bearer Token）。

### Controller 级注解

#### @Tag
为 Controller 分类和标记。

```java
@Tag(name = "认证管理 / Authentication", description = "用户认证、个人信息、菜单和权限资源")
```

### 方法级注解

#### @Operation
描述 API 操作的摘要和详细信息。

```java
@Operation(
    summary = "获取验证码 / Get Captcha",
    description = "生成新的图形验证码用于登录 / Generate a new image captcha for login"
)
```

#### @Parameter
描述路径参数、查询参数的含义和类型。

```java
@Parameter(
    description = "OAuth 提供者 / OAuth provider",
    required = true
)
@RequestParam String provider
```

#### @ApiResponses 和 @ApiResponse
描述 API 可能返回的响应状态码和含义。

```java
@ApiResponses(value = {
    @ApiResponse(responseCode = "0", description = "成功 / Success"),
    @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
    @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
})
```

#### @RequestBody (io.swagger.v3.oas.annotations.parameters)
描述请求体的含义和示例。

```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "登录信息 / Login credentials",
    required = true,
    content = @Content(schema = @Schema(implementation = LoginVO.class))
)
@RequestBody LoginVO vo
```

### 数据模型注解

#### @Schema
描述数据模型的字段信息。

```java
@Schema(name = "用户信息", description = "用户资料")
public class UserVO {
    @Schema(description = "用户 ID / User ID")
    private Long id;

    @Schema(description = "用户名 / Username")
    private String username;
}
```

## 模块覆盖情况

### 已覆盖模块（13 个 Controllers，82+ 个 API 端点）

1. **Auth 模块**（4 个 Controllers，34 个端点）
   - AuthController：15 个端点
   - AuthUserController：6 个端点
   - AuthResourceController：5 个端点
   - AuthRoleController：8 个端点

2. **Log 模块**（1 个 Controller，4 个端点）
   - LogController：4 个端点

3. **Config 模块**（1 个 Controller，4 个端点）
   - ConfigController：4 个端点

4. **Storage 模块**（1 个 Controller，5 个端点）
   - StorageController：5 个端点

5. **Douyin 模块**（2 个 Controllers，9 个端点）
   - DouyinAccountController：5 个端点
   - DouyinVideoController：4 个端点

6. **Live 模块**（4 个 Controllers，26 个端点）
   - LiveSessionController：7 个端点
   - LiveProductController：6 个端点
   - LiveMonitorController：3 个端点
   - LiveScriptController：8 个端点

## 访问 Swagger UI

### 本地开发

启动应用后，访问以下 URL：

- **Swagger UI 界面**：`http://localhost:8188/swagger-ui.html`
- **OpenAPI JSON**：`http://localhost:8188/v3/api-docs`
- **按标签分组**：`http://localhost:8188/v3/api-docs.yaml`

### 功能特性

- **API 分类**：按 @Tag 分组展示
- **参数说明**：显示所有参数的类型和说明
- **请求示例**：可直接在界面上测试 API
- **响应示例**：显示各状态码的响应结构
- **认证支持**：支持 JWT Bearer Token 认证

## 中英文双语支持

所有注解都采用 "中文 / English" 的格式，便于国际化使用。

示例：
```java
@Tag(name = "认证管理 / Authentication", description = "用户认证、个人信息、菜单和权限资源")
@Operation(summary = "用户登录 / User Login", description = "使用用户名密码进行登录...")
```

## 最佳实践

### 1. 命名规范

- Controller 类名：`*Controller`（如 AuthController）
- 方法名：清晰表达操作意图（如 search, get, save, delete）
- 标签名：模块 + 功能 + 语言（如 "认证管理 / Authentication"）

### 2. 描述规范

- 摘要（Summary）：简洁清晰，不超过 50 字符
- 描述（Description）：详细说明功能、输入、输出
- 采用中英文对照格式

### 3. 参数说明

- 必需参数：`required = true`
- 可选参数：`required = false`
- 提供默认值说明
- 示例值参考

### 4. 响应设计

- 成功响应：`responseCode = "0"`
- 客户端错误：`responseCode = "400-499"`
- 服务器错误：`responseCode = "500-599"`
- 认证相关：`responseCode = "401"`, `"403"`

## 验收标准

✅ **完成项目**：

- [x] 所有 13 个 Controller 都有 @Tag 注解
- [x] 所有 82+ 个方法都有 @Operation 注解
- [x] 所有参数都有 @Parameter 注解
- [x] 所有响应都有 @ApiResponse 注解
- [x] 所有请求体都有 @RequestBody 注解说明
- [x] SwaggerConfig.java 配置完成
- [x] application-swagger.yml 配置完成
- [x] mvn compile 通过（无新的编译错误）
- [x] Swagger UI 可访问且完整显示
- [x] /v3/api-docs 返回有效的 OpenAPI JSON

## 常见问题

### Q: 如何自定义 API 分组？

A: 在 SwaggerConfig 中配置 `groupdocs` 属性，或使用 @GroupedOpenApi 注解。

### Q: 如何隐藏某个 API？

A: 在方法上添加 `@Hidden` 注解。

### Q: 如何上传 API 文档？

A: 可将 `/v3/api-docs` 返回的 JSON 上传到 Swagger Hub 或其他文档平台。

## 参考资源

- [springdoc-openapi 官网](https://springdoc.org/)
- [OpenAPI 规范](https://spec.openapis.org/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [Swagger 注解说明](https://swagger.io/specification/)

## 后续改进方向

1. **分组管理**：将 API 按业务域进一步分组
2. **示例数据**：为响应体添加完整的示例数据
3. **数据模型文档**：为所有 VO 类添加 @Schema 注解
4. **错误码文档**：统一维护错误码文档
5. **认证方案**：支持多种认证方式说明
6. **API 版本管理**：支持多个 API 版本的文档管理

---

**最后更新**：2026-02-25
**文档版本**：1.0.0
**项目版本**：0.0.1-SNAPSHOT
