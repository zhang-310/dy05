# Phase 1 — 安全加固（CRITICAL）

## 目标

消除 5 个 CRITICAL 级安全漏洞 + 3 个 HIGH 级安全问题。完成后系统达到生产级安全基线。

---

## 任务 1.1：移除硬编码密钥

**问题**：API Key 和 JWT Secret 明文写死在配置文件中，任何有仓库访问权限的人可直接获取。

**涉及文件**：
- `src/main/resources/application.yml:116` — TIANAPI_API_KEY 硬编码默认值
- `src/main/resources/application.yml:227` — APP_TOKEN_SECRET 硬编码默认值
- `.env.example:127-129` — 讯飞凭证硬编码
- `.env.example:141` — 天行 API Key 硬编码

**修复方案**：

1. `application.yml` 中所有密钥移除默认值，改为必填环境变量：

```yaml
# 修改前
tianapi:
  api-key: ${TIANAPI_API_KEY:b617a43d965f558c7d0923dc663853b0}

app:
  token:
    secret: ${APP_TOKEN_SECRET:dev-only-change-in-production-2025!@#$%}

# 修改后
tianapi:
  api-key: ${TIANAPI_API_KEY}

app:
  token:
    secret: ${APP_TOKEN_SECRET}
```

2. `.env.example` 中所有真实凭证替换为占位符：

```env
TIANAPI_API_KEY=your-tianapi-key-here
IFLYTEK_APP_ID=your-iflytek-app-id
IFLYTEK_API_KEY=your-iflytek-api-key
IFLYTEK_API_SECRET=your-iflytek-api-secret
APP_TOKEN_SECRET=generate-a-strong-random-secret-min-32-chars
```

3. 新增启动校验类 `SecretValidationConfig.java`，在 `@PostConstruct` 中检查关键密钥非空且非默认值，否则启动失败（仅 prod profile）。

**验收**：
- [ ] `application.yml` 中无任何明文密钥
- [ ] `.env.example` 中无任何真实凭证
- [ ] `git log --all -p | grep -i "b617a43d"` 无结果（历史清理可后续处理）
- [ ] prod profile 启动时缺少密钥会报错退出

---

## 任务 1.2：修复 Spring Security 授权配置

**问题**：`SecurityConfig.java:61` 对 `/api/v1/**` 设置 `permitAll()`，Spring Security 授权层形同虚设，认证完全依赖 `AuthTokenFilter`。如果 filter 被绕过（如路径遍历），所有端点裸奔。

**涉及文件**：
- `src/main/java/cn/gaifan/douyinOperations/common/config/SecurityConfig.java:57-62`

**修复方案**：

```java
// 修改前
http
    .csrf(csrf -> csrf.disable())
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(WHITELIST).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/v1/**")).permitAll()
        .anyRequest().permitAll()
    );

// 修改后
http
    .csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        .ignoringRequestMatchers(
            // SSE 和 OAuth 回调不需要 CSRF
            new AntPathRequestMatcher("/api/v1/**/stream/**"),
            new AntPathRequestMatcher("/api/v1/auth/oauth/**"),
            new AntPathRequestMatcher("/actuator/**")
        )
    )
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(WHITELIST).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/login")).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/register")).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/captcha/**")).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/sms/send")).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/oauth/**")).permitAll()
        .anyRequest().authenticated()
    );
```

**注意**：
- CSRF 采用 Double Submit Cookie 模式，适合 SPA 架构
- 前端 `request.ts` 需要从 cookie 读取 CSRF token 并附加到请求头（见任务 1.2b）
- SSE、OAuth 回调、actuator 健康检查豁免 CSRF
- `AuthTokenFilter` 保留作为 JWT 解析层，Spring Security 作为授权兜底

**前端配套修改**（`frontend-react/src/utils/request.ts`）：

```typescript
// 在 request interceptor 中添加 CSRF token
instance.interceptors.request.use((config) => {
  // 现有 Bearer token 逻辑保留...

  // 添加 CSRF token（从 cookie 读取）
  const csrfToken = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='))
    ?.split('=')[1];
  if (csrfToken) {
    config.headers['X-XSRF-TOKEN'] = decodeURIComponent(csrfToken);
  }
  return config;
});
```

**验收**：
- [ ] 未登录访问 `/api/v1/product/list` 返回 401（而非空数据）
- [ ] 登录后正常访问所有业务接口
- [ ] SSE 流式接口正常工作
- [ ] 无 CSRF token 的 POST 请求返回 403

---

## 任务 1.3：Elasticsearch 安全加固

**问题**：`docker-compose.yml:101` 的 `xpack.security.enabled` 默认 false，集群完全开放。

**涉及文件**：
- `docker/docker-compose.yml:101`
- `docker/.env.example`
- `src/main/resources/application.yml`（ES 连接配置）

**修复方案**：

```yaml
# docker-compose.yml — 修改默认值
environment:
  - xpack.security.enabled=${ES_SECURITY_ENABLED:-true}  # 改为默认 true
  - ELASTIC_PASSWORD=${ES_PASSWORD:-changeme}

# docker/.env.example — 添加
ES_SECURITY_ENABLED=true
ES_PASSWORD=your-es-password-here
```

```yaml
# application.yml — 添加 ES 认证配置
spring:
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD:}
```

**验收**：
- [ ] ES 启动后无认证访问 `curl localhost:9200` 返回 401
- [ ] 后端通过配置的用户名密码正常连接 ES
- [ ] 知识库搜索功能正常

---

## 任务 1.4：移除 Token Query Parameter 回退

**问题**：`AuthTokenFilter.java:130` 允许从 URL `?token=xxx` 获取认证 token，token 会泄露到访问日志、浏览器历史、代理日志。

**涉及文件**：
- `src/main/java/cn/gaifan/douyinOperations/common/config/AuthTokenFilter.java:126-130`

**修复方案**：

```java
// 修改前
private String extractToken(HttpServletRequest req) {
    String bearer = req.getHeader("Authorization");
    if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
        return bearer.substring(7);
    }
    return req.getParameter("token");  // 删除此行
}

// 修改后
private String extractToken(HttpServletRequest req) {
    String bearer = req.getHeader("Authorization");
    if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
        return bearer.substring(7);
    }
    return null;
}
```

**注意**：检查前端是否有通过 query parameter 传 token 的场景（如 SSE EventSource）。如果有，需要改为：
- GET SSE 端点在 SecurityConfig 中单独配置认证豁免或使用 cookie 认证
- 或改用 POST SSE（已有 `ssePost` 实现）

**验收**：
- [ ] `?token=xxx` 方式访问 API 返回 401
- [ ] Authorization header 方式正常
- [ ] SSE 流式接口正常（确认不依赖 query token）

---

## 任务 1.5：Auth 端点补充限流

**问题**：登录有 5次/分钟限制，但验证码和短信发送端点无限流。

**涉及文件**：
- `src/main/java/cn/gaifan/douyinOperations/common/config/RateLimiterConfiguration.java`
- `src/main/java/cn/gaifan/douyinOperations/common/interceptor/RateLimitInterceptor.java`

**修复方案**：

在 `RateLimiterConfiguration` 中添加：

```java
// 验证码：每 IP 10次/分钟
registry.rateLimiter("captcha", RateLimiterConfig.custom()
    .limitForPeriod(10)
    .limitRefreshPeriod(Duration.ofMinutes(1))
    .timeoutDuration(Duration.ZERO)
    .build());

// 短信发送：每手机号 3次/分钟，每 IP 10次/分钟
registry.rateLimiter("sms-send", RateLimiterConfig.custom()
    .limitForPeriod(3)
    .limitRefreshPeriod(Duration.ofMinutes(1))
    .timeoutDuration(Duration.ZERO)
    .build());
```

在对应 Controller 方法上添加 `@RateLimiter` 注解或在 `RateLimitInterceptor` 中配置路径映射。

**验收**：
- [ ] 验证码接口超过 10次/分钟返回 429
- [ ] 短信发送接口超过 3次/分钟返回 429
- [ ] 正常使用不受影响

---

## 任务 1.6：CORS Header 收紧

**问题**：`SecurityConfig.java:81` 的 `setAllowedHeaders(List.of("*"))` 允许所有请求头。

**涉及文件**：
- `src/main/java/cn/gaifan/douyinOperations/common/config/SecurityConfig.java:81`

**修复方案**：

```java
// 修改前
config.setAllowedHeaders(List.of("*"));

// 修改后
config.setAllowedHeaders(List.of(
    "Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN",
    "X-Requested-With", "Cache-Control"
));
```

**验收**：
- [ ] 前端所有功能正常（登录、CRUD、SSE、文件上传）
- [ ] 非白名单 header 的跨域请求被拒绝

---

## Phase 1 完成标准

```bash
# 全部通过即为 Phase 1 完成
mvn compile                                    # 编译通过
mvn test                                       # 测试通过
cd frontend-react && npm run type-check        # 类型检查通过
cd frontend-react && npm run test              # 前端测试通过

# 手动验证
curl -X POST http://localhost:8080/api/v1/product/list  # 返回 401（未认证）
curl -H "Authorization: Bearer <valid-token>" -X POST http://localhost:8080/api/v1/product/list  # 返回 200
```
