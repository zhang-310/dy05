# SMS 模块架构审查报告

**审查日期**: 2026-05-08
**审查范围**: sms 模块（短信服务）
**审查标准**: 架构设计原则、SOLID、DDD、微服务最佳实践

## 执行摘要

**总体评分**: 82/100 (等级 B+)

**关键发现**:
- ✅ 分层架构清晰，遵循标准 MVC 模式
- ✅ 数据模型设计合理，支持多服务商配置
- ✅ 验证码机制完善，包含防暴力破解保护
- ⚠️ 缺少实际短信发送实现（仅有数据模型）
- ⚠️ 缺少前端集成（无 API 调用）
- ⚠️ 敏感信息（API Key/Secret）未加密存储
- ❌ 缺少服务商适配器抽象层
- ❌ 缺少异步发送机制

**主要建议**:
1. 实现服务商适配器模式，支持腾讯云/阿里云/自定义服务商
2. 添加敏感信息加密存储（API Key/Secret）
3. 实现异步发送队列（RabbitMQ）
4. 完善前端页面和 API 集成
5. 添加发送限流和配额管理

## 架构概览

### 模块职责

SMS 模块负责短信服务的统一管理，包括：
- **服务商配置管理**：支持多服务商（腾讯云/阿里云/自定义），配置 API Key/Secret
- **短信模板管理**：模板 CRUD、状态管理、占位符支持
- **短信发送日志**：记录所有发送请求、状态、成本
- **验证码服务**：生成、发送、验证 OTP 验证码，防暴力破解

### 技术栈

| 层级 | 技术 |
|------|------|
| 持久层 | Spring Data JPA + PostgreSQL |
| 业务层 | Spring Service + Specification 动态查询 |
| 控制层 | Spring MVC + RESTful API |
| 缓存 | Spring Cache (Caffeine L1 + Redis L2) |
| 校验 | Jakarta Validation |
| 测试 | JUnit 5 + Mockito + AssertJ |

### 依赖关系

```
sms (douyin-operations-integration)
├── 依赖 common 模块
│   ├── ErrorCode（错误码）
│   ├── RESTResult（统一响应）
│   ├── PageResultVO（分页结果）
│   ├── BasicQueryDto（分页基类）
│   └── AuthTokenFilter（认证过滤器）
└── 无跨模块业务依赖（独立模块）
```

**模块位置**: `douyin-operations-integration/src/main/java/.../module/sms/`

## 详细分析

### 1. 分层架构 (18/20分)

**优点**:
- ✅ 严格遵循四层架构：Controller → Service → Repository → Entity
- ✅ 每层职责清晰，无跨层调用
- ✅ VO 层完整：SearchVO、SaveVO、VO 分离
- ✅ Repository 使用 JpaSpecificationExecutor 支持动态查询

**问题**:
- ⚠️ **缺少服务商适配器层**：直接在 Service 中生成验证码，未抽象短信发送接口
- ⚠️ **缺少领域服务层**：验证码生成、过期检查等领域逻辑散落在 ServiceImpl 中

**建议**:
```java
// 建议增加适配器层
public interface SmsProviderAdapter {
    SendResult send(String phone, String templateId, Map<String, String> params);
    String getProviderCode();
}

// 具体实现
public class AliyunSmsAdapter implements SmsProviderAdapter { ... }
public class TencentSmsAdapter implements SmsProviderAdapter { ... }
```

**扣分**: -2 分（缺少适配器抽象）

### 2. 模块化与解耦 (16/20分)

**优点**:
- ✅ 模块独立性强，无跨模块业务依赖
- ✅ 使用接口定义服务契约（SmsService 接口）
- ✅ Repository 方法命名规范，遵循 Spring Data JPA 约定

**问题**:
- ⚠️ **服务商耦合**：providerCode 字符串硬编码（"tencent"/"aliyun"），未使用枚举
- ⚠️ **缺少事件发布**：短信发送成功/失败未发布领域事件，其他模块无法感知
- ❌ **缺少实际发送实现**：SmsServiceImpl.sendVerificationCode() 仅保存数据库，未调用服务商 API

**建议**:
```java
// 1. 使用枚举管理服务商
public enum SmsProviderType {
    ALIYUN("aliyun", "阿里云"),
    TENCENT("tencent", "腾讯云"),
    CUSTOMIZE("customize", "自定义");
}

// 2. 发布领域事件
applicationEventPublisher.publishEvent(new SmsSentEvent(logId, phoneNumber, status));
```

**扣分**: -4 分（缺少实际发送实现 -2，服务商耦合 -2）

### 3. 数据模型设计 (14/15分)

**优点**:
- ✅ 四张表设计合理：sms_provider_config、sms_template、sms_send_log、sms_verification_code
- ✅ 逻辑删除正确实现（provider/template 有 deleted 字段，log/code 无需逻辑删除）
- ✅ 时间戳字段完整：create_time/update_time，使用 @PrePersist/@PreUpdate 自动维护
- ✅ 数据隔离：所有表含 owner_id，Service 层强制过滤
- ✅ 验证码表设计完善：
  - 防暴力破解：attempt_count/max_attempts
  - 过期机制：expires_at
  - IP 追踪：created_ip/verified_ip
  - 状态管理：is_verified/verified_time

**问题**:
- ❌ **敏感信息明文存储**：SmsProviderConfig.apiKey/apiSecret 未加密，存在安全风险

**建议**:
```java
// 使用 JPA AttributeConverter 加密敏感字段
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "api_key", nullable = false, length = 256)
private String apiKey;
```

**扣分**: -1 分（敏感信息未加密）

### 4. API 设计 (13/15分)

**优点**:
- ✅ RESTful 风格统一：所有接口使用 POST 方法（符合项目规范）
- ✅ 路径规范：`/api/v1/sms/{resource}/{action}`
- ✅ 统一响应格式：RESTResult<T> + traceId
- ✅ 参数校验完整：@Valid + Jakarta Validation
- ✅ 认证检查：所有接口检查 userId，未登录返回 401

**API 清单**（18 个接口）:
```
服务商配置（6）: /provider/list, /provider/get, /provider/save, 
                /provider/delete, /provider/update-status, /provider/set-default
短信模板（5）:   /template/list, /template/get, /template/save, 
                /template/delete, /template/update-status
发送日志（2）:   /log/list, /log/get
验证码（3）:     /code/send, /code/verify, /code/get-latest
```

**问题**:
- ⚠️ **缺少批量操作**：无批量删除、批量启用/禁用接口
- ⚠️ **缺少统计接口**：无发送量统计、成功率统计、成本统计

**建议**:
```java
@PostMapping("/log/stats")
public RESTResult<SmsStatsVO> getStats(@RequestParam String startDate, @RequestParam String endDate);

@PostMapping("/provider/batch-delete")
public RESTResult<Void> batchDelete(@RequestBody List<Long> ids);
```

**扣分**: -2 分（缺少统计和批量操作）

### 5. 错误处理 (9/10分)

**优点**:
- ✅ 使用 BusinessException 统一异常处理
- ✅ 错误码规范：ErrorCode.SMS_TEMPLATE_NOT_FOUND、SMS_CODE_INVALID 等
- ✅ 验证码错误场景完整：
  - 不存在/已过期 → SMS_CODE_EXPIRED
  - 已使用 → SMS_CODE_INVALID
  - 超过尝试次数 → SMS_CODE_ATTEMPT_LIMIT
  - 验证码错误 → SMS_CODE_INVALID

**问题**:
- ⚠️ **部分错误码复用**：SMS_CODE_INVALID 同时表示"不存在"和"验证码错误"，语义不清

**建议**:
```java
// 细化错误码
ErrorCode.SMS_CODE_NOT_FOUND  // 验证码不存在
ErrorCode.SMS_CODE_INCORRECT  // 验证码错误
ErrorCode.SMS_CODE_INVALID    // 验证码无效（已使用）
```

**扣分**: -1 分（错误码语义不清）

### 6. 可扩展性 (7/10分)

**优点**:
- ✅ 支持多服务商配置（providerCode 字段）
- ✅ 模板支持占位符（{{variable}}）
- ✅ 日志记录完整（bizId/bizType 关联业务）

**问题**:
- ❌ **缺少服务商适配器**：新增服务商需修改 Service 代码，违反开闭原则
- ❌ **缺少异步发送**：同步发送阻塞请求，无法应对高并发
- ⚠️ **缺少重试机制**：发送失败无自动重试
- ⚠️ **缺少限流保护**：无每分钟/每小时发送限制

**建议**:
```java
// 1. 适配器工厂模式
public class SmsProviderFactory {
    private Map<String, SmsProviderAdapter> adapters;
    public SmsProviderAdapter getAdapter(String providerCode) { ... }
}

// 2. 异步发送（RabbitMQ）
@RabbitListener(queues = "sms.send.queue")
public void handleSmsSend(SmsSendMessage message) { ... }

// 3. 限流注解
@RateLimiter(key = "sms:send:#{phoneNumber}", rate = 1, per = 60)
public void sendVerificationCode(SmsVerificationCodeSendVO vo) { ... }
```

**扣分**: -3 分（缺少适配器 -1，缺少异步 -1，缺少限流 -1）

### 7. 可测试性 (10/10分)

**优点**:
- ✅ 单元测试覆盖完整：SmsServiceImplTest 548 行，35 个测试用例
- ✅ 测试场景全面：
  - 正常流程：CRUD、分页查询、状态更新
  - 异常流程：不存在、已过期、已使用、超过尝试次数
  - 边界条件：验证码错误增加尝试次数
- ✅ 使用 Mockito 隔离依赖，测试独立性强
- ✅ 使用 AssertJ 断言，可读性好
- ✅ 测试命名规范：`methodName_scenario_expectedResult`

**测试覆盖率**:
- 服务商配置：8 个测试
- 短信模板：6 个测试
- 发送日志：2 个测试
- 验证码：10 个测试

**无扣分**

## 架构风险评估

| 风险 | 严重程度 | 影响范围 | 缓解措施 |
|------|----------|----------|----------|
| 敏感信息明文存储 | 高 | 数据安全 | 使用 JPA Converter 加密 API Key/Secret |
| 缺少实际发送实现 | 高 | 功能完整性 | 实现服务商适配器，集成腾讯云/阿里云 SDK |
| 同步发送阻塞请求 | 中 | 性能 | 引入 RabbitMQ 异步发送队列 |
| 无限流保护 | 中 | 安全/成本 | 添加 Redis 限流（每分钟/每小时/每天） |
| 无重试机制 | 中 | 可靠性 | 发送失败自动重试 3 次，指数退避 |
| 缺少前端集成 | 低 | 用户体验 | 开发前端页面和 API 调用 |

## 改进建议

### 短期（1-2 周）

**P0 - 阻塞级**:
1. **实现服务商适配器**（3 人日）
   - 定义 SmsProviderAdapter 接口
   - 实现 AliyunSmsAdapter（集成阿里云 SDK）
   - 实现 TencentSmsAdapter（集成腾讯云 SDK）
   - 实现 SmsProviderFactory 工厂类

2. **敏感信息加密**（1 人日）
   - 实现 EncryptedStringConverter
   - 迁移现有数据（加密 API Key/Secret）
   - 更新配置文档

**P1 - 高优先级**:
3. **异步发送队列**（2 人日）
   - 定义 RabbitMQ 队列：sms.send.queue
   - 实现消息生产者（Controller 发送消息）
   - 实现消息消费者（调用服务商 API）
   - 更新发送日志状态（pending → sending → success/failed）

### 中期（1 个月）

**P1 - 高优先级**:
4. **限流保护**（2 人日）
   - 实现 Redis 限流：每分钟 1 条/手机号，每小时 5 条/手机号
   - 实现配额管理：daily_quota/daily_sent_count 自动重置
   - 添加限流异常：ErrorCode.SMS_RATE_LIMIT_EXCEEDED

5. **重试机制**（1 人日）
   - 发送失败自动重试 3 次
   - 指数退避：1s、2s、4s
   - 记录重试次数到日志

**P2 - 中优先级**:
6. **前端集成**（3 人日）
   - 开发服务商配置页面（CRUD）
   - 开发短信模板页面（CRUD）
   - 开发发送日志页面（查询、导出）
   - 添加 API 调用层（front/src/api/sms.ts）

7. **统计功能**（2 人日）
   - 发送量统计（按日/周/月）
   - 成功率统计（按服务商/模板）
   - 成本统计（按服务商）
   - 可视化图表（ECharts）

### 长期（持续改进）

**P2 - 中优先级**:
8. **服务商回调**（2 人日）
   - 实现回调接口：/api/v1/sms/callback/{providerCode}
   - 更新送达状态：delivered_time
   - 处理失败回调：error_code/error_message

9. **模板变量校验**（1 人日）
   - 解析模板占位符：{{code}}、{{name}}
   - 发送时校验参数完整性
   - 返回友好错误提示

**P3 - 低优先级**:
10. **国际化支持**（1 人日）
    - 支持国际手机号（+86、+1 等）
    - 支持多语言模板
    - 地区路由（region 字段）

## 问题清单

### P0 - 阻塞级

1. **缺少服务商适配器实现**
   - 文件：SmsServiceImpl.java
   - 问题：sendVerificationCode() 仅保存数据库，未调用服务商 API
   - 影响：验证码无法实际发送，功能不可用
   - 修复：实现 SmsProviderAdapter 接口 + 集成腾讯云/阿里云 SDK

2. **敏感信息明文存储**
   - 文件：SmsProviderConfig.java
   - 问题：apiKey/apiSecret 明文存储数据库
   - 影响：数据库泄露导致服务商账号被盗用
   - 修复：使用 JPA Converter 加密存储

### P1 - 高优先级

3. **缺少异步发送机制**
   - 文件：SmsServiceImpl.java
   - 问题：同步调用服务商 API，阻塞请求
   - 影响：高并发时响应慢，用户体验差
   - 修复：引入 RabbitMQ 异步队列

4. **缺少限流保护**
   - 文件：SmsController.java
   - 问题：无发送频率限制
   - 影响：恶意刷短信，成本失控
   - 修复：添加 Redis 限流（每分钟/每小时/每天）

5. **缺少重试机制**
   - 文件：SmsServiceImpl.java
   - 问题：发送失败无自动重试
   - 影响：网络抖动导致发送失败，可靠性差
   - 修复：实现指数退避重试（3 次）

### P2 - 中优先级

6. **服务商代码硬编码**
   - 文件：SmsProviderConfig.java
   - 问题：providerCode 使用字符串，未使用枚举
   - 影响：拼写错误、类型不安全
   - 修复：定义 SmsProviderType 枚举

7. **错误码语义不清**
   - 文件：SmsServiceImpl.java (line 262, 282)
   - 问题：SMS_CODE_INVALID 同时表示"不存在"和"验证码错误"
   - 影响：前端无法区分错误类型
   - 修复：细化错误码（SMS_CODE_NOT_FOUND、SMS_CODE_INCORRECT）

8. **缺少前端集成**
   - 文件：front/src/api/sms.ts（不存在）
   - 问题：无前端页面和 API 调用
   - 影响：功能无法使用
   - 修复：开发前端页面 + API 调用层

9. **缺少统计接口**
   - 文件：SmsController.java
   - 问题：无发送量、成功率、成本统计
   - 影响：无法监控短信使用情况
   - 修复：添加统计接口 + 可视化图表

### P3 - 低优先级

10. **缺少批量操作**
    - 文件：SmsController.java
    - 问题：无批量删除、批量启用/禁用
    - 影响：操作效率低
    - 修复：添加批量操作接口

11. **缺少服务商回调**
    - 文件：SmsController.java
    - 问题：无回调接口处理送达状态
    - 影响：无法获取实际送达时间
    - 修复：实现回调接口 /callback/{providerCode}

12. **缺少模板变量校验**
    - 文件：SmsServiceImpl.java
    - 问题：发送时未校验模板占位符参数
    - 影响：参数缺失导致发送失败
    - 修复：解析模板 + 校验参数完整性

## 总结

SMS 模块整体架构清晰，分层合理，测试覆盖完整，但**缺少核心功能实现**（服务商适配器、异步发送、限流保护）。数据模型设计优秀，验证码机制完善，但存在**安全隐患**（敏感信息明文存储）。

**核心问题**：
1. 缺少服务商适配器实现（P0）
2. 敏感信息明文存储（P0）
3. 缺少异步发送机制（P1）
4. 缺少限流保护（P1）

**优先级排序**：
1. P0：实现服务商适配器 + 敏感信息加密（4 人日）
2. P1：异步发送队列 + 限流保护 + 重试机制（5 人日）
3. P2：前端集成 + 统计功能（5 人日）
4. P3：批量操作 + 回调处理 + 模板校验（4 人日）

**总工作量**: 18 人日

**生产就绪度**: ⚠️ 60%（核心功能未实现，需完成 P0+P1 才能上线）
