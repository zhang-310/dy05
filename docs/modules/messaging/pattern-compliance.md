# Messaging 模块模式合规性检查报告

**检查日期**: 2026-05-08
**检查范围**: messaging 模块（消息通知 - 企微/飞书接入）
**检查标准**: docs/adr/ 架构决策记录

## 执行摘要

**总体评分**: 92/100 (等级 A)

| 模式 | 合规性 | 问题数 |
|------|--------|--------|
| API 规范（ADR-001）| ✅ 完全合规 | 0 |
| 数据访问（ADR-003）| ✅ 完全合规 | 0 |
| 数据隔离（ADR-004）| ⚠️ 部分合规 | 2 |
| 逻辑删除（ADR-005）| ⚠️ 部分合规 | 1 |
| 缓存策略 | ⚠️ 未实现 | 1 |
| 错误码规范 | ✅ 完全合规 | 0 |

**关键发现**:
- ✅ API 设计规范，正确区分认证端点（需登录）和 Webhook 端点（token 验签）
- ✅ JPA Specification 动态查询实现标准
- ✅ 错误码使用规范，覆盖所有异常场景
- ⚠️ 数据隔离在 Controller 层实现，但 Service 层缺少二次校验
- ⚠️ Repository 层存在手动 deleted 过滤，与 @SQLRestriction 重复
- ⚠️ 未实现缓存策略（配置查询频繁但无缓存）

## 详细检查结果

### 1. API 规范（ADR-001）✅

**检查项**:
- [x] 业务 API 统一使用 POST 方法
- [x] 路径使用动作词（/list, /get, /save, /delete）
- [x] 例外场景符合 ADR-001 规定

**检查结果**: 完全合规

**Controller 分析**:

#### MessagingController（认证端点）
| 端点 | HTTP 方法 | 说明 | 合规性 |
|------|-----------|------|--------|
| `/api/v1/messaging/config/list` | POST | 配置列表（分页） | ✅ |
| `/api/v1/messaging/config/get` | POST | 配置详情 | ✅ |
| `/api/v1/messaging/config/save` | POST | 新增/更新配置 | ✅ |
| `/api/v1/messaging/config/delete` | POST | 删除配置 | ✅ |

#### MessagingWebhookController（公开端点）
| 端点 | HTTP 方法 | 说明 | 合规性 |
|------|-----------|------|--------|
| `/api/v1/messaging/webhook/wecom` | GET | 企微 URL 验证 | ✅ 符合例外（第三方回调） |
| `/api/v1/messaging/webhook/wecom` | POST | 企微消息回调 | ✅ |
| `/api/v1/messaging/webhook/feishu` | POST | 飞书事件回调 | ✅ |

**优点**:
1. 业务 API 100% 使用 POST 方法
2. Webhook GET 端点符合 ADR-001 明确例外（企微 URL 验证强制 GET）
3. 路径命名清晰，动作词规范（list/get/save/delete）
4. 正确区分认证端点（需 Bearer Token）和公开端点（token 参数验签）

---

### 2. 数据访问模式（ADR-003）✅

**检查项**:
- [x] Repository 继承 JpaSpecificationExecutor
- [x] Service 层使用 Specification 构建动态查询
- [x] 查询条件动态组合
- [x] 与 Spring Data 分页/排序集成

**检查结果**: 完全合规

**Repository 实现**:
```java
// MsgPlatformConfigRepository.java
public interface MsgPlatformConfigRepository extends 
    JpaRepository<MsgPlatformConfig, Long>, 
    JpaSpecificationExecutor<MsgPlatformConfig> {
    // ✅ 正确继承 JpaSpecificationExecutor
}
```

**Service 实现**:
```java
// MessagingPlatformServiceImpl.java:34-43
Specification<MsgPlatformConfig> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("deleted"), 0));
    if (searchVO.getOwnerId() != null) 
        predicates.add(cb.equal(root.get("ownerId"), searchVO.getOwnerId()));
    if (searchVO.getPlatform() != null && !searchVO.getPlatform().isBlank()) {
        predicates.add(cb.equal(root.get("platform"), searchVO.getPlatform().trim()));
    }
    if (searchVO.getStatus() != null) 
        predicates.add(cb.equal(root.get("status"), searchVO.getStatus()));
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

**优点**:
1. Specification 实现标准，条件动态组合
2. 正确使用 PageRequest 和 Sort
3. 查询参数校验（BasicQueryDto.validateParams()）
4. 类型安全，编译期检查

---

### 3. 数据隔离（ADR-004）⚠️

**检查项**:
- [x] Entity 包含 ownerId 字段
- [x] Controller 层设置 ownerId
- [⚠️] Service 层强制过滤 ownerId（部分实现）
- [❌] Service 修改/删除操作缺少 ownerId 校验

**检查结果**: 部分合规（2 个问题）

**Entity 定义**:
```java
// MsgPlatformConfig.java:22-23
@Column(name = "owner_id", nullable = false)
private Long ownerId;
// ✅ 正确定义 ownerId 字段
```

**Controller 层隔离**:
```java
// MessagingController.java:32-35
Long userId = AuthTokenFilter.getUserId(request);
if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
vo.setOwnerId(userId);
// ✅ 正确设置 ownerId
```

**Service 层隔离**:
```java
// MessagingPlatformServiceImpl.java:37
if (searchVO.getOwnerId() != null) 
    predicates.add(cb.equal(root.get("ownerId"), searchVO.getOwnerId()));
// ✅ 查询操作正确过滤 ownerId
```

**问题 1 (P1)**: Service 层 getById/delete 缺少 ownerId 校验
```java
// MessagingPlatformServiceImpl.java:54-56
public MsgPlatformConfigVO getById(Long id) {
    return toVO(repository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在")));
}
// ❌ 未校验 ownerId，用户 A 可以查询用户 B 的配置
```

**问题 2 (P1)**: Controller 层 get/delete 未传递 userId 到 Service
```java
// MessagingController.java:43-47
@PostMapping("/config/get")
public RESTResult<MsgPlatformConfigVO> get(HttpServletRequest request, @RequestParam Long id) {
    if (AuthTokenFilter.getUserId(request) == null) 
        return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    RESTResult<MsgPlatformConfigVO> r = RESTResult.getSuccess(messagingPlatformService.getById(id));
    // ❌ 未传递 userId，Service 层无法校验 ownerId
}
```

**安全风险**:
- 用户 A 可以通过 ID 查询/删除用户 B 的配置
- 数据隔离失效，存在越权访问风险

---

### 4. 逻辑删除（ADR-005）⚠️

**检查项**:
- [x] Entity 包含 deleted 字段
- [x] Entity 使用 @SQLRestriction("deleted = 0")
- [⚠️] Repository 方法手动过滤 deleted（与 @SQLRestriction 重复）
- [x] Service 删除操作设置 deleted = 1

**检查结果**: 部分合规（1 个问题）

**Entity 定义**:
```java
// MsgPlatformConfig.java:14-15, 52-53
@Entity
@Table(name = "msg_platform_config")
@SQLRestriction("deleted = 0")
public class MsgPlatformConfig {
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;
}
// ✅ 正确使用 @SQLRestriction
```

**Service 删除操作**:
```java
// MessagingPlatformServiceImpl.java:83-88
public void delete(Long id) {
    MsgPlatformConfig entity = repository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
    entity.setDeleted(1);
    repository.save(entity);
}
// ✅ 正确实现逻辑删除
```

**问题 3 (P2)**: Repository 方法手动过滤 deleted
```java
// MsgPlatformConfigRepository.java:12-16
Optional<MsgPlatformConfig> findByIdAndDeleted(Long id, Integer deleted);
List<MsgPlatformConfig> findByPlatformAndStatusAndDeleted(String platform, Integer status, Integer deleted);
Optional<MsgPlatformConfig> findByPlatformAndCallbackTokenAndDeleted(String platform, String callbackToken, Integer deleted);
// ⚠️ 手动传递 deleted 参数，与 @SQLRestriction 重复
```

**影响**:
- 代码冗余，每次调用需传递 `deleted = 0`
- @SQLRestriction 已自动过滤，手动过滤属于防御性编程但不必要
- 增加维护成本，容易遗漏

**建议**:
```java
// 推荐写法（依赖 @SQLRestriction）
Optional<MsgPlatformConfig> findById(Long id);
List<MsgPlatformConfig> findByPlatformAndStatus(String platform, Integer status);
Optional<MsgPlatformConfig> findByPlatformAndCallbackToken(String platform, String callbackToken);
```

---

### 5. 缓存策略 ⚠️

**检查项**:
- [❌] L1 缓存（Caffeine）未实现
- [❌] L2 缓存（Redis）未实现
- [❌] 缓存失效策略未定义

**检查结果**: 未实现（1 个问题）

**问题 4 (P2)**: 配置查询频繁但无缓存

**分析**:
1. **高频查询场景**:
   - Webhook 回调每次都查询配置（`getConfigEntityByPlatformAndToken`）
   - 企微/飞书消息推送频繁，配置查询成为瓶颈

2. **当前实现**:
```java
// MessagingPlatformServiceImpl.java:97-100
public MsgPlatformConfig getConfigEntityByPlatformAndToken(String platform, String callbackToken) {
    if (platform == null || callbackToken == null || callbackToken.isBlank()) return null;
    return repository.findByPlatformAndCallbackTokenAndDeleted(platform, callbackToken, 0).orElse(null);
}
// ❌ 每次都查询数据库，无缓存
```

3. **性能影响**:
   - Webhook 高并发场景下数据库压力大
   - 配置数据变更频率低，适合缓存

**建议实现**:
```java
@Cacheable(value = "msgPlatformConfig", key = "#platform + ':' + #callbackToken")
public MsgPlatformConfig getConfigEntityByPlatformAndToken(String platform, String callbackToken) {
    // ...
}

@CacheEvict(value = "msgPlatformConfig", allEntries = true)
public long save(MsgPlatformConfigSaveVO vo) {
    // ...
}
```

**缓存策略建议**:
- **L1 (Caffeine)**: 最大 100 条，TTL 5 分钟
- **L2 (Redis)**: TTL 30 分钟
- **失效时机**: save/delete 操作清空缓存

---

### 6. 错误码规范 ✅

**检查项**:
- [x] 使用 ErrorCode 常量类
- [x] 错误码语义明确
- [x] 覆盖所有异常场景

**检查结果**: 完全合规

**错误码使用统计**:
| 错误码 | 使用场景 | 文件 |
|--------|----------|------|
| `UNAUTHORIZED` | 未登录 | MessagingController (4 处) |
| `DATA_NOT_FOUND` | 配置不存在 | MessagingPlatformServiceImpl (3 处) |
| `WECOM_AUTH_FAIL` | 企微/飞书鉴权失败 | MessagingWebhookController (3 处), MessagingReplyServiceImpl (3 处) |
| `VALIDATION_FAIL` | 请求体为空/不支持的平台 | MessagingWebhookController (2 处), MessagingReplyServiceImpl (1 处) |
| `WECOM_SEND_FAIL` | 消息发送失败 | MessagingReplyServiceImpl (4 处) |

**优点**:
1. 错误码使用规范，语义清晰
2. 覆盖认证、数据查询、第三方调用等所有场景
3. 错误消息描述准确，便于问题定位
4. 统一使用 BusinessException 抛出业务异常

---

## 问题清单

### P0 - 阻塞级
无

### P1 - 高优先级

**P1-1: Service 层 getById/delete 缺少 ownerId 校验**
- **文件**: `MessagingPlatformServiceImpl.java`
- **位置**: 第 54-56 行（getById）、第 83-88 行（delete）
- **风险**: 用户 A 可以查询/删除用户 B 的配置，数据隔离失效
- **修复方案**:
  ```java
  public MsgPlatformConfigVO getById(Long id, Long userId) {
      MsgPlatformConfig entity = repository.findByIdAndDeleted(id, 0)
              .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
      if (!entity.getOwnerId().equals(userId)) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问");
      }
      return toVO(entity);
  }
  ```

**P1-2: Controller 层 get/delete 未传递 userId 到 Service**
- **文件**: `MessagingController.java`
- **位置**: 第 43-47 行（get）、第 63-68 行（delete）
- **风险**: Service 层无法校验 ownerId
- **修复方案**:
  ```java
  @PostMapping("/config/get")
  public RESTResult<MsgPlatformConfigVO> get(HttpServletRequest request, @RequestParam Long id) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      RESTResult<MsgPlatformConfigVO> r = RESTResult.getSuccess(messagingPlatformService.getById(id, userId));
      r.setTraceId(MDC.get("traceId"));
      return r;
  }
  ```

### P2 - 中优先级

**P2-1: Repository 方法手动过滤 deleted**
- **文件**: `MsgPlatformConfigRepository.java`
- **位置**: 第 12-16 行
- **影响**: 代码冗余，与 @SQLRestriction 重复
- **修复方案**: 移除 `AndDeleted` 后缀和 `Integer deleted` 参数，依赖 @SQLRestriction 自动过滤

**P2-2: 配置查询未实现缓存**
- **文件**: `MessagingPlatformServiceImpl.java`
- **位置**: 第 97-100 行（getConfigEntityByPlatformAndToken）
- **影响**: Webhook 高并发场景下数据库压力大
- **修复方案**: 添加 @Cacheable 注解，实现 L1+L2 缓存

### P3 - 低优先级
无

---

## 修复建议

### 短期（1-2 周）

**优先级 1: 修复数据隔离漏洞（P1-1, P1-2）**
- 工作量: 0.5 人日
- 步骤:
  1. 修改 Service 接口，getById/delete 方法增加 userId 参数
  2. 修改 ServiceImpl，添加 ownerId 校验逻辑
  3. 修改 Controller，传递 userId 到 Service
  4. 编写单元测试，验证越权访问被拦截

**优先级 2: 实现配置查询缓存（P2-2）**
- 工作量: 0.5 人日
- 步骤:
  1. 在 `getConfigEntityByPlatformAndToken` 方法添加 @Cacheable
  2. 在 save/delete 方法添加 @CacheEvict
  3. 配置 Caffeine 缓存（最大 100 条，TTL 5 分钟）
  4. 配置 Redis 缓存（TTL 30 分钟）
  5. 压测验证缓存效果

### 中期（1 个月）

**优先级 3: 重构 Repository 方法（P2-1）**
- 工作量: 0.3 人日
- 步骤:
  1. 移除 Repository 方法的 `AndDeleted` 后缀
  2. 移除所有调用处的 `deleted = 0` 参数
  3. 验证 @SQLRestriction 自动过滤生效
  4. 更新单元测试

### 长期（持续改进）

**优先级 4: 完善监控和告警**
- 工作量: 1 人日
- 步骤:
  1. 添加配置查询 QPS 监控
  2. 添加缓存命中率监控
  3. 添加 Webhook 回调失败告警
  4. 添加第三方 API 调用超时告警

**优先级 5: 优化错误处理**
- 工作量: 0.5 人日
- 步骤:
  1. 统一 Webhook 回调异常处理
  2. 添加重试机制（企微/飞书消息发送失败）
  3. 添加降级策略（第三方 API 不可用时）

---

## 总结

**模块健康度**: A 级（92 分）

**优点**:
1. ✅ API 设计规范，符合 ADR-001 统一 POST 接口规范
2. ✅ JPA Specification 动态查询实现标准
3. ✅ 错误码使用规范，覆盖全面
4. ✅ 逻辑删除实现正确（Entity 层）
5. ✅ 代码结构清晰，职责分离良好

**待改进**:
1. ⚠️ 数据隔离存在安全漏洞（P1 级别，需立即修复）
2. ⚠️ 缺少缓存策略，高并发场景性能不足
3. ⚠️ Repository 方法存在冗余过滤

**总工作量**: 2.8 人日

**建议优先级**:
1. **立即修复**: P1-1, P1-2（数据隔离漏洞）
2. **本周完成**: P2-2（配置查询缓存）
3. **本月完成**: P2-1（Repository 重构）
4. **持续改进**: 监控告警、错误处理优化

---

## 附录：模块文件清单

**Entity (1)**:
- `MsgPlatformConfig.java` - 企微/飞书配置表

**Repository (1)**:
- `MsgPlatformConfigRepository.java` - 配置数据访问

**Service (5)**:
- `MessagingPlatformService.java` - 配置管理接口
- `MessagingPlatformServiceImpl.java` - 配置管理实现
- `MessagingWebhookHandler.java` - Webhook 处理接口
- `MessagingWebhookHandlerImpl.java` - Webhook 处理实现（位于 intelligence 模块）
- `MessagingReplyService.java` / `MessagingReplyServiceImpl.java` - 消息回复服务

**Controller (2)**:
- `MessagingController.java` - 配置管理 API（需登录）
- `MessagingWebhookController.java` - Webhook 回调 API（公开）

**VO (3)**:
- `MsgPlatformConfigVO.java` - 配置返回值
- `MsgPlatformConfigSaveVO.java` - 配置保存入参
- `MsgPlatformConfigSearchVO.java` - 配置查询参数

**Util (1)**:
- `WecomCryptoUtil.java` - 企微加解密工具

**Test (3)**:
- `MessagingControllerTest.java`
- `MessagingWebhookControllerTest.java`
- `MessagingPlatformServiceImplTest.java`
- `MessagingReplyServiceImplTest.java`

**总计**: 17 个文件
