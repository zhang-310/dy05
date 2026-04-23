# log 模块 — AOP 日志系统

> 版本：3.0 | 更新日期：2026-02-26

---

## 1. 整体架构

```
┌──────────────────────────────────────────────────────────────┐
│                    AOP 日志记录流程                            │
│                                                              │
│  业务 Controller                                             │
│       │                                                      │
│       ▼                                                      │
│  @OperationLog(module="auth", operation="封禁用户")            │
│       │                                                      │
│       ▼                                                      │
│  OperationLogAspect（@Around）                                │
│       │                                                      │
│       ├── 1. 前置：记录开始时间、解析注解参数                    │
│       │                                                      │
│       ├── 2. 执行：proceed() 执行目标方法                      │
│       │                                                      │
│       ├── 3. 后置：计算耗时、获取响应状态                       │
│       │   └── 异常：捕获异常信息，重新抛出                      │
│       │                                                      │
│       ├── 4. 参数脱敏：密码/Token/密钥等字段替换为 ***          │
│       │                                                      │
│       └── 5. 异步写入：@Async 写入 sys_operation_log           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. @OperationLog 注解设计

### 2.1 注解定义

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块名称
     * 取值：auth / douyin / live / shortvideo / ai / storage / system / config / script
     */
    String module();

    /**
     * 操作描述
     * 如："用户登录"、"创建直播场次"、"上传文件"
     */
    String operation();

    /**
     * 是否记录请求参数，默认 true
     * 文件上传等场景设为 false，避免记录大量二进制数据
     */
    boolean recordParams() default true;
}
```

### 2.2 注解属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| module | String | 必填 | 模块标识，对应 sys_operation_log.module |
| operation | String | 必填 | 操作描述，对应 sys_operation_log.operation |
| recordParams | boolean | true | 是否序列化请求参数到 request_params 字段 |

---

## 3. AOP 拦截器实现

### 3.1 OperationLogAspect

```java
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {

        // 1. 前置：记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 获取当前用户信息（从 SecurityContext）
        Long userId = SecurityContext.getUserId();
        String username = SecurityContext.getUsername();

        // 3. 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        String ip = getClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        String method = request != null
            ? request.getMethod() + " " + request.getRequestURI()
            : joinPoint.getSignature().toShortString();

        // 4. 序列化请求参数（脱敏）
        String requestParams = null;
        if (operationLog.recordParams()) {
            requestParams = sanitizeParams(joinPoint.getArgs());
        }

        // 5. 执行目标方法
        Object result = null;
        Integer responseStatus = 200;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            // 从返回值提取 status（如果是 RESTResult）
            responseStatus = extractStatus(result);
        } catch (Throwable e) {
            responseStatus = -1;
            errorMessage = extractErrorMessage(e);
            throw e;  // 重新抛出，不影响业务异常处理
        } finally {
            // 6. 计算耗时
            long durationMs = System.currentTimeMillis() - startTime;

            // 7. 异步写入日志
            operationLogService.asyncSaveLog(
                userId, username,
                operationLog.module(), operationLog.operation(),
                method, requestParams,
                responseStatus, ip, userAgent,
                durationMs, errorMessage
            );
        }

        return result;
    }
}
```

### 3.2 关键方法说明

| 方法 | 说明 |
|------|------|
| `getHttpServletRequest()` | 从 RequestContextHolder 获取当前 HTTP 请求 |
| `getClientIp(request)` | 优先从 X-Forwarded-For / X-Real-IP 获取真实 IP |
| `sanitizeParams(args)` | 序列化参数为 JSON，并对敏感字段脱敏 |
| `extractStatus(result)` | 如果返回值是 RESTResult，提取 status 字段 |
| `extractErrorMessage(e)` | 提取异常类名 + message，截取前 500 字符 |

---

## 4. 日志记录流程

```
Controller 方法被调用
       │
       ▼
  AOP @Around 拦截
       │
       ├── [前置] 记录 startTime
       │         获取 userId / username（SecurityContext）
       │         获取 ip / userAgent（HttpServletRequest）
       │         获取 method（请求方法 + URI）
       │         序列化 requestParams（如 recordParams=true）
       │
       ├── [执行] joinPoint.proceed()
       │     │
       │     ├── 成功 → 提取 responseStatus
       │     │
       │     └── 异常 → 记录 errorMessage → 重新 throw
       │
       └── [后置 / finally]
             计算 durationMs
             异步调用 operationLogService.asyncSaveLog(...)
```

---

## 5. 敏感参数脱敏策略

### 5.1 脱敏字段

| 字段名（不区分大小写） | 脱敏方式 | 示例 |
|----------------------|---------|------|
| password / oldPassword / newPassword | 替换为 `***` | `"password":"***"` |
| passwordHash | 替换为 `***` | `"passwordHash":"***"` |
| token | 替换为 `***` | `"token":"***"` |
| secret / appSecret | 替换为 `***` | `"appSecret":"***"` |
| captchaCode | 替换为 `***` | `"captchaCode":"***"` |
| code（验证码） | 替换为 `***` | `"code":"***"` |

### 5.2 脱敏实现

```java
private static final Set<String> SENSITIVE_FIELDS = Set.of(
    "password", "oldpassword", "newpassword", "passwordhash",
    "token", "secret", "appsecret", "captchacode", "code"
);

private String sanitizeParams(Object[] args) {
    // 1. 将参数序列化为 JSON
    // 2. 遍历 JSON 字段，匹配 SENSITIVE_FIELDS（忽略大小写）
    // 3. 匹配的字段值替换为 "***"
    // 4. 跳过 MultipartFile / InputStream 等不可序列化类型
    // 5. 截取前 2000 字符（避免超大参数）
}
```

### 5.3 特殊参数处理

| 参数类型 | 处理方式 |
|---------|---------|
| MultipartFile | 记录文件名和大小，不记录内容 |
| InputStream / byte[] | 跳过，记录 `[binary]` |
| 超长字符串（>2000） | 截取前 2000 字符 + `...` |
| null 参数 | 跳过 |

---

## 6. 异步记录

### 6.1 异步写入

```java
@Service
public class OperationLogService {

    @Autowired
    private SysOperationLogRepository repository;

    /**
     * 异步保存操作日志，避免影响业务方法性能
     */
    @Async
    public void asyncSaveLog(Long userId, String username,
                             String module, String operation,
                             String method, String requestParams,
                             Integer responseStatus, String ip, String userAgent,
                             Long durationMs, String errorMessage) {
        try {
            SysOperationLog log = new SysOperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setModule(module);
            log.setOperation(operation);
            log.setMethod(method);
            log.setRequestParams(requestParams);
            log.setResponseStatus(responseStatus);
            log.setIp(ip);
            log.setUserAgent(userAgent);
            log.setDurationMs(durationMs);
            log.setErrorMessage(errorMessage);
            repository.save(log);
        } catch (Exception e) {
            // 日志记录失败不应影响业务，仅打印错误日志
            log.error("操作日志保存失败: {}", e.getMessage(), e);
        }
    }
}
```

### 6.2 异步配置

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("log-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

> 使用 `CallerRunsPolicy` 拒绝策略：队列满时由调用线程执行，保证日志不丢失。

---

## 7. 日志保留策略

| 策略 | 说明 |
|------|------|
| 保留期限 | 永久保留（BR-01：只记不删） |
| 数据增长 | 预估日均 200-20,000 条（取决于用户量） |
| 性能优化 | 索引覆盖常用查询；超大数据量时考虑按月分区 |
| 归档方案 | 后续可通过定时任务将 6 个月前的日志归档到冷存储 |

---

## 8. 各模块关键操作清单

| 模块 | 操作 | recordParams | 说明 |
|------|------|-------------|------|
| auth | 用户登录 | true | 记录登录方式（不记录密码） |
| auth | 修改密码 | false | 不记录密码参数 |
| auth | 封禁/解封用户 | true | 记录 userId 和 ban 状态 |
| auth | 角色授权变更 | true | 记录 roleId 和 resourceIds |
| auth | 保存用户 | true | 记录用户信息（密码脱敏） |
| douyin | 绑定/解绑账号 | true | 记录账号信息 |
| douyin | 保存人设 | true | 记录人设配置 |
| douyin | 保存产品 | true | 记录产品信息 |
| live | 创建直播场次 | true | 记录场次配置 |
| live | 生成直播话术 | true | 记录生成参数 |
| shortvideo | 发起视频同步 | true | 记录同步参数 |
| ai | 创建 AI 任务 | true | 记录任务参数 |
| storage | 上传文件 | false | 不记录文件二进制内容 |
| storage | 删除文件 | true | 记录文件 ID |
| config | 修改系统配置 | true | 记录配置变更 |
