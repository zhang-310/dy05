## 监控系统集成示例

### 在 Controller 中使用 BusinessMetrics

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private BusinessMetrics businessMetrics;

    @PostMapping("/login")
    public RESTResult<LoginResult> login(@RequestBody LoginParams params) {
        try {
            LoginResult result = authService.login(params);
            // 记录成功登录
            businessMetrics.recordLogin("password", true);
            return RESTResult.success(result);
        } catch (Exception e) {
            // 记录登录失败
            businessMetrics.recordLogin("password", false);
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "AuthController");
            throw e;
        }
    }

    @PostMapping("/oauth/callback")
    public RESTResult<LoginResult> oauthCallback(@RequestParam String code, @RequestParam String provider) {
        try {
            LoginResult result = authService.oauthLogin(code, provider);
            businessMetrics.recordLogin(provider, true);
            return RESTResult.success(result);
        } catch (Exception e) {
            businessMetrics.recordLogin(provider, false);
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "AuthController");
            throw e;
        }
    }
}
```

### 在 Service 中使用 BusinessMetrics

```java
@Service
public class AuthUserService {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private BusinessMetrics businessMetrics;

    public void save(AuthUser entity) {
        long startTime = System.currentTimeMillis();
        try {
            authUserRepository.save(entity);
            long duration = System.currentTimeMillis() - startTime;
            businessMetrics.recordDataOperation("save", "AuthUser", duration);
        } catch (Exception e) {
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "AuthUserService");
            throw e;
        }
    }

    public AuthUser findById(Long id) {
        long startTime = System.currentTimeMillis();
        try {
            AuthUser user = authUserRepository.findById(id).orElse(null);
            long duration = System.currentTimeMillis() - startTime;
            businessMetrics.recordDataOperation("query", "AuthUser", duration);
            return user;
        } catch (Exception e) {
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "AuthUserService");
            throw e;
        }
    }

    public void update(AuthUser entity) {
        long startTime = System.currentTimeMillis();
        try {
            authUserRepository.save(entity);
            long duration = System.currentTimeMillis() - startTime;
            businessMetrics.recordDataOperation("update", "AuthUser", duration);
        } catch (Exception e) {
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "AuthUserService");
            throw e;
        }
    }

    public void delete(Long id) {
        long startTime = System.currentTimeMillis();
        try {
            authUserRepository.deleteById(id);
            long duration = System.currentTimeMillis() - startTime;
            businessMetrics.recordDataOperation("delete", "AuthUser", duration);
        } catch (Exception e) {
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "AuthUserService");
            throw e;
        }
    }
}
```

### 在缓存操作中使用 BusinessMetrics

```java
@Component
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BusinessMetrics businessMetrics;

    public Object get(String key) {
        long startTime = System.currentTimeMillis();
        try {
            Object value = redisTemplate.opsForValue().get(key);
            long duration = System.currentTimeMillis() - startTime;

            if (value != null) {
                businessMetrics.recordCacheOperation("hit", "redis", duration);
            } else {
                businessMetrics.recordCacheOperation("miss", "redis", duration);
            }
            return value;
        } catch (Exception e) {
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "CacheService");
            throw e;
        }
    }

    public void put(String key, Object value, long timeout, TimeUnit unit) {
        long startTime = System.currentTimeMillis();
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            long duration = System.currentTimeMillis() - startTime;
            businessMetrics.recordCacheOperation("put", "redis", duration);
        } catch (Exception e) {
            businessMetrics.recordBusinessError(e.getClass().getSimpleName(), "CacheService");
            throw e;
        }
    }
}
```

### 在 API 拦截器中记录请求指标

```java
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    @Autowired
    private BusinessMetrics businessMetrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        long startTime = (long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        String endpoint = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        businessMetrics.recordApiRequest(endpoint, method, status, duration);
    }
}
```

### 在数据库连接池监控中使用

```java
@Component
public class DatabasePoolMonitor {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private BusinessMetrics businessMetrics;

    @Scheduled(fixedRate = 30000) // 每 30 秒检查一次
    public void monitorPoolStatus() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
            int maxConnections = hikariDataSource.getMaximumPoolSize();

            businessMetrics.recordDatabasePoolStatus(activeConnections, maxConnections);
        }
    }
}
```

### Prometheus 查询示例

在 Prometheus 中可以使用以下查询：

```promql
# API 请求速率（每秒请求数）
rate(api_requests_total[5m])

# API 响应时间 p95
histogram_quantile(0.95, rate(api_response_time_seconds_bucket[5m]))

# 错误率
rate(business_errors_total[5m])

# 缓存命中率
cache_hit_rate

# 数据库连接池使用率
database_pool_active / database_pool_max

# JVM 堆内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# 用户登录成功率
rate(user_login_attempts_total{success="true"}[5m]) / rate(user_login_attempts_total[5m])
```

### Grafana 仪表板自定义

1. 登录 Grafana: http://localhost:3000
2. 使用默认凭证 (admin/admin)
3. 导航到 Dashboards > Douyin Operations 监控仪表板
4. 点击 Edit 编辑仪表板
5. 添加新面板或修改现有面板
6. 使用上述 Prometheus 查询创建自定义面板

### 告警配置

告警规则已在 `prometheus/alerts.yml` 中配置，包括：

- **HighErrorRate**: 错误率 > 5% 持续 5 分钟
- **SlowApiResponse**: API p95 响应时间 > 1 秒持续 5 分钟
- **LowCacheHitRate**: 缓存命中率 < 50% 持续 10 分钟
- **HighDatabasePoolUsage**: 连接池使用率 > 80% 持续 5 分钟
- **HighJvmMemoryUsage**: JVM 堆内存使用率 > 85% 持续 5 分钟
- **SlowDataOperation**: 数据操作 p95 耗时 > 500ms 持续 5 分钟
- **HighLoginFailureRate**: 登录失败率 > 10% 持续 5 分钟
- **BackendDown**: 后端应用不可用超过 1 分钟

### 性能优化建议

1. **调整抓取间隔**: 根据需求调整 `prometheus.yml` 中的 `scrape_interval`
2. **数据保留策略**: 修改 `docker-compose.yml` 中的 `--storage.tsdb.retention.time`
3. **告警阈值**: 根据实际业务调整 `prometheus/alerts.yml` 中的阈值
4. **仪表板刷新率**: 在 Grafana 中调整仪表板的刷新频率
