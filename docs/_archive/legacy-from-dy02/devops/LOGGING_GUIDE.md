# 日志系统使用指南

## 概述

本项目采用完整的日志系统，包括：
- **Logback**：日志框架，支持按日期和文件大小滚动
- **Micrometer**：应用监控和分布式追踪
- **审计日志**：记录所有创建/编辑/删除操作
- **性能日志**：记录接口响应时间、数据库查询时间等

## 日志配置文件

### logback-spring.xml

位置：`src/main/resources/logback-spring.xml`

#### 开发环境（dev/local）配置

```
日志输出目录：logs/
日志文件：
  - application.log           （应用日志）
  - audit.log                 （审计日志）
  - performance.log           （性能日志）

滚动策略：
  - 文件大小：10MB
  - 历史保留：30天
  - 输出目标：Console + File
```

#### 生产环境（prod）配置

```
日志输出目录：logs/
日志文件：
  - application.log           （应用日志）
  - application-json.log      （JSON格式日志，支持ELK）
  - audit.log                 （审计日志 - JSON）
  - performance.log           （性能日志 - JSON）
  - archive/                  （日志归档）

滚动策略：
  - 文件大小：10MB
  - 历史保留：30天
  - 总大小限制：2GB
  - 输出目标：File only（无Console输出）
```

## 日志使用

### 1. 在代码中使用日志

使用 Lombok 的 `@Slf4j` 注解：

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyService {
    public void doSomething() {
        log.debug("Debug message");
        log.info("Info message");
        log.warn("Warning message");
        log.error("Error message", exception);
    }
}
```

### 2. 日志级别

```
DEBUG   - 详细的调试信息
INFO    - 一般信息消息
WARN    - 警告消息
ERROR   - 错误消息（不中断程序）
```

### 3. 审计日志

审计日志通过 AOP 切面自动记录，无需手动处理。

#### 自动记录的操作

- **CREATE**：所有 `*Service.save*` 方法
- **UPDATE**：所有 `*Service.update*` 方法
- **DELETE**：所有 `*Service.delete*` 方法

#### 审计日志字段

```json
{
  "timestamp": "2024-01-15 10:30:45.123",
  "operationType": "CREATE",
  "username": "admin",
  "className": "UserService",
  "methodName": "saveUser",
  "args": "[UserDTO{...}]",
  "result": "User{id=1, username='john'}",
  "status": "SUCCESS",
  "ip": "192.168.1.1"
}
```

### 4. 性能日志

性能日志通过 AOP 切面自动记录。

#### 覆盖的层

- **Controller**：所有控制器方法
- **Service**：所有服务方法
- **Repository**：所有数据库查询

#### 性能日志字段

```json
{
  "method": "UserService.findById",
  "category": "service",
  "duration_ms": 45,
  "request_url": "/api/users/1",
  "status": "SUCCESS"
}
```

#### 慢查询警告

- 执行时间 > 500ms 时会输出 WARN 级别日志

### 5. 自定义使用 @Timed 注解（Micrometer）

```java
import io.micrometer.core.annotation.Timed;

@Timed(value = "custom.operation", description = "Custom operation timing")
public void customMethod() {
    // your code
}
```

## 日志文件位置

### 开发环境
```
logs/
├── application.log
├── audit.log
└── performance.log
```

### 生产环境
```
logs/
├── application.log
├── application-json.log
├── audit.log
├── performance.log
└── archive/
    ├── application-2024-01-15.1.log
    ├── application-2024-01-14.1.log
    └── ...
```

## 日志查看

### 实时查看（开发环境）

```bash
# 查看应用日志
tail -f logs/application.log

# 查看审计日志
tail -f logs/audit.log

# 查看性能日志
tail -f logs/performance.log

# 搜索特定用户的操作
grep "admin" logs/audit.log

# 搜索慢查询（>500ms）
grep "SLOW_QUERY" logs/performance.log
```

### 日志分析（生产环境）

生产环境使用 JSON 格式日志，可集成到 ELK Stack：

```bash
# 查看 JSON 日志
cat logs/application-json.log | jq '.'

# 过滤特定错误
cat logs/application-json.log | jq 'select(.level == "ERROR")'

# 导入到 Logstash
cat logs/application-json.log | logstash -f config/logstash.conf
```

## 日志配置说明

### 异步日志处理

为了提升性能，大部分日志使用异步 Appender：

```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>           <!-- 异步队列大小 -->
    <discardingThreshold>0</discardingThreshold>  <!-- 不丢弃日志 -->
    <appender-ref ref="FILE"/>
</appender>
```

### 日志滚动策略

采用 SizeAndTimeBasedRollingPolicy：

```xml
<fileNamePattern>${LOG_DIR}/application-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
<maxFileSize>10MB</maxFileSize>         <!-- 单文件最大大小 -->
<maxHistory>30</maxHistory>             <!-- 保留最近30天 -->
<totalSizeCap>2GB</totalSizeCap>        <!-- 总大小限制 -->
```

## 常见问题

### Q1：如何关闭某个模块的调试日志？

在 `logback-spring.xml` 中调整日志级别：

```xml
<logger name="org.springframework.web" level="INFO"/>
```

### Q2：如何实现日志远程传输？

在 logback-spring.xml 中添加 Socket Appender：

```xml
<appender name="SOCKET" class="ch.qos.logback.classic.net.SocketAppender">
    <remoteHost>logserver.example.com</remoteHost>
    <port>4560</port>
</appender>
```

### Q3：审计日志是否写入数据库？

目前审计日志写入文件。如需持久化到数据库，可使用 AuditLogRepository 进行异步持久化。

### Q4：性能日志的百分位是什么意思？

- p50：50% 的请求在此时间内完成
- p95：95% 的请求在此时间内完成
- p99：99% 的请求在此时间内完成

## 集成 ELK Stack（可选）

### Logstash 配置示例

```
input {
  file {
    path => "/var/log/douyin-operations/application-json.log"
    codec => json
    start_position => "beginning"
  }
}

filter {
  # 可添加过滤和处理规则
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "douyin-ops-%{+YYYY.MM.dd}"
  }
}
```

## 最佳实践

1. **不要在生产环境使用DEBUG级别**：会产生大量日志
2. **定期清理日志**：生产环境配置日志自动轮转
3. **敏感信息过滤**：不要记录密码、令牌等敏感信息
4. **统一日志格式**：使用 JSON 格式便于分析
5. **合理使用异步**：避免日志输出阻塞主业务

## 依赖版本

```
Logback: 1.4.x (Spring Boot 内置)
Micrometer: 1.12.x
Logstash Encoder: 7.4
OpenTelemetry: 1.25.x
```

## 相关文件

- 配置文件：`src/main/resources/logback-spring.xml`
- 审计日志：`src/main/java/.../aspect/AuditLogAspect.java`
- 性能日志：`src/main/java/.../aspect/PerformanceLogAspect.java`
- Micrometer 配置：`src/main/java/.../config/MicrometerConfig.java`
- 审计日志实体：`src/main/java/.../entity/AuditLog.java`
