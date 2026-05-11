# Workflow 模块模式合规性分析报告

**生成日期**: 2026-05-09  
**分析范围**: workflow 模块（douyin-operations-content + douyin-operations-live）  
**合规标准**: dy05 项目架构规范（CLAUDE.md + docs/adr/）

---

## 执行摘要

| 维度 | 评分 | 等级 | 说明 |
|------|------|------|------|
| **总体合规性** | 58/100 | Grade D+ | 基础架构清晰但存在多个关键合规问题 |
| RESTful API 规范 | 85/100 | B+ | 遵循统一 POST 规范，响应格式正确 |
| 数据访问层规范 | 40/100 | F | 未使用 Specification 动态查询 |
| 缓存策略 | 0/100 | F | 完全未实现缓存（L1/L2） |
| 数据隔离 | 0/100 | F | 缺少 owner_id 字段，存在严重安全漏洞 |
| 错误处理 | 75/100 | B- | 使用 BusinessException，但信息不够详细 |

**关键发现**:
- ✅ API 接口遵循统一 POST 规范，返回 RESTResult
- ✅ 逻辑删除实现正确（@SQLRestriction）
- ✅ 事务管理使用 @Transactional
- ❌ **P0**: 缺少 owner_id 数据隔离（CVSS 9.1 严重安全漏洞）
- ❌ **P0**: 未使用 JPA Specification 动态查询
- ❌ **P0**: 完全未实现缓存策略（L1 Caffeine + L2 Redis）
- ⚠️ **P1**: Controller 使用 Map 而非 VO 类
- ⚠️ **P1**: 事务边界过大，包含外部 AI 调用

---

## 1. RESTful API 规范合规性

### 1.1 统一 POST 规范（ADR-001）

**标准要求**:
- 所有业务 API 使用 POST 方法
- 路径模式: `/api/v1/<模块>/<资源>/<动作>`
- 统一响应体: `RESTResult<T>`

**实际情况**:

**WorkflowController** (1 个接口):
- ✅ `/api/v1/workflow/execute` - POST

**代码示例**:
```java
@PostMapping("/execute")
@Operation(summary = "执行工作流")
public RESTResult<WorkflowExecutor.WorkflowExecuteResult> execute(
    HttpServletRequest request,
    @RequestBody Map<String, Object> body) {
    // ...
    RESTResult<WorkflowExecutor.WorkflowExecuteResult> r = RESTResult.getSuccess(result);
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

**优点**:
- ✅ 使用 POST 方法
- ✅ 返回 RESTResult 统一响应体
- ✅ 设置 traceId 用于分布式追踪
- ✅ 使用 @Operation 注解提供 API 文档

**问题**:
- ⚠️ **P1**: Controller 接收 `Map<String, Object>` 而非强类型 VO
- ⚠️ **P2**: 仅 1 个接口，缺少 CRUD 操作（list/get/save/delete）

**合规性评分**: 85/100

---

### 1.2 响应格式规范

**标准要求**:
- 使用 `RESTResult.success(data)` / `RESTResult.error(code, msg)`
- 包含 traceId 字段
- 错误码使用 ErrorCode 常量

**实际情况**:

**正确示例**:
```java
// WorkflowController.java
if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
if (workflowCode == null || workflowCode.isBlank()) {
    return RESTResult.error(ErrorCode.VALIDATION_FAIL, "workflowCode 不能为空");
}
RESTResult<WorkflowExecutor.WorkflowExecuteResult> r = RESTResult.getSuccess(result);
r.setTraceId(MDC.get("traceId"));
return r;
```

**优点**:
- ✅ 使用 ErrorCode 常量
- ✅ 设置 traceId
- ✅ 错误信息清晰

**合规性评分**: 90/100

---

## 2. 数据访问层规范

### 2.1 JPA Specification 动态查询（ADR-003）

**标准要求**:
- Repository 继承 `JpaRepository + JpaSpecificationExecutor`
- Service 使用 Specification 构建动态查询
- SearchVO 继承 `BasicQueryDto`

**实际情况**:

**Repository 层** ❌:
```java
// WorkflowDefinitionRepository.java
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    Optional<WorkflowDefinition> findByWorkflowCodeAndDeleted(String workflowCode, Integer deleted);
}

// WorkflowStepRepository.java
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findByDefinitionIdAndDeletedOrderBySequenceNoAsc(Long definitionId, Integer deleted);
}
```

**问题**:
- ❌ **P0**: Repository 未继承 `JpaSpecificationExecutor`
- ❌ **P0**: 使用方法命名查询而非 Specification
- ❌ **P0**: 缺少 SearchVO 类（无分页查询接口）

**应该如何实现**:
```java
// WorkflowDefinitionRepository.java - 正确实现
public interface WorkflowDefinitionRepository extends 
    JpaRepository<WorkflowDefinition, Long>, 
    JpaSpecificationExecutor<WorkflowDefinition> {
    // 移除方法命名查询
}

// WorkflowDefinitionServiceImpl.java - 使用 Specification
public WorkflowDefinition findByCode(String workflowCode) {
    Specification<WorkflowDefinition> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("workflowCode"), workflowCode));
        predicates.add(cb.equal(root.get("deleted"), 0));
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    return repository.findOne(spec).orElseThrow(...);
}
```

**合规性评分**: 40/100

---

### 2.2 逻辑删除规范

**标准要求**:
- Entity 使用 `@SQLRestriction("deleted = 0")`
- 表含 `deleted INTEGER DEFAULT 0` 字段
- 删除操作设置 `deleted = 1`

**实际情况**:

**Entity 层** ✅:
```java
// WorkflowDefinition.java
@Entity
@Table(name = "workflow_definition")
@SQLRestriction("deleted = 0")
public class WorkflowDefinition {
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;
}

// WorkflowStep.java
@Entity
@Table(name = "workflow_step")
@SQLRestriction("deleted = 0")
public class WorkflowStep {
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;
}
```

**数据库层** ✅:
```sql
-- workflow_definition
deleted INTEGER NOT NULL DEFAULT 0

-- workflow_step
deleted INTEGER NOT NULL DEFAULT 0
```

**优点**:
- ✅ 所有 Entity 正确使用 @SQLRestriction
- ✅ 数据库字段定义正确
- ✅ 查询自动过滤已删除记录

**合规性评分**: 100/100

---

## 3. 缓存策略规范

### 3.1 两级缓存架构

**标准要求**:
- L1: Caffeine 本地缓存（5 分钟 TTL）
- L2: Redis 分布式缓存（@Cacheable）
- 查询顺序: L1 → L2 → DB

**实际情况**: ❌ 完全未实现

**问题分析**:
```java
// WorkflowExecutorImpl.java - 无缓存
@Override
@Transactional(rollbackFor = Exception.class)
public WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params) {
    // 每次都查询数据库
    WorkflowDefinition def = definitionRepository.findByWorkflowCodeAndDeleted(workflowCode, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流不存在: " + workflowCode));
    List<WorkflowStep> steps = stepRepository.findByDefinitionIdAndDeletedOrderBySequenceNoAsc(def.getId(), 0);
    // ...
}
```

**影响**:
- ❌ 每次执行工作流都查询数据库（WorkflowDefinition + WorkflowStep）
- ❌ 工作流定义是静态配置，应该缓存
- ❌ 高频调用场景下数据库压力大

**应该如何实现**:
```java
// WorkflowDefinitionServiceImpl.java - 添加缓存
@Cacheable(value = "workflow:definition", key = "#workflowCode")
public WorkflowDefinition findByCode(String workflowCode) {
    return repository.findOne(spec).orElseThrow(...);
}

@Cacheable(value = "workflow:steps", key = "#definitionId")
public List<WorkflowStep> findSteps(Long definitionId) {
    return stepRepository.findAll(spec);
}
```

**合规性评分**: 0/100

---

## 4. 数据隔离规范

### 4.1 多租户数据隔离

**标准要求**:
- 用户私有表含 `owner_id` 字段
- Service 层强制过滤: `predicates.add(cb.equal(root.get("ownerId"), userId))`
- 所有查询/更新/删除操作验证 ownerId

**实际情况**: ❌ 完全未实现

**Entity 层** ❌:
```java
// WorkflowDefinition.java - 缺少 owner_id
@Entity
@Table(name = "workflow_definition")
public class WorkflowDefinition {
    private Long id;
    private String workflowCode;
    private String workflowName;
    // ❌ 缺少 ownerId 字段
}

// WorkflowStep.java - 缺少 owner_id
@Entity
@Table(name = "workflow_step")
public class WorkflowStep {
    private Long id;
    private Long definitionId;
    // ❌ 缺少 ownerId 字段
}
```

**数据库层** ❌:
```sql
-- workflow_definition - 缺少 owner_id
CREATE TABLE workflow_definition (
    id BIGSERIAL PRIMARY KEY,
    workflow_code VARCHAR(64) NOT NULL,
    -- ❌ 缺少 owner_id BIGINT NOT NULL
);

-- workflow_step - 缺少 owner_id
CREATE TABLE workflow_step (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    -- ❌ 缺少 owner_id BIGINT NOT NULL
);
```

**安全漏洞**:
- ❌ **CVSS 9.1 严重**: 任何用户可以执行任意工作流
- ❌ **CVSS 8.5 高危**: 用户 A 可以查看/修改用户 B 的工作流定义
- ❌ 缺少数据隔离导致多租户数据泄露

**应该如何实现**:
```java
// 1. 添加数据库字段
ALTER TABLE workflow_definition ADD COLUMN owner_id BIGINT NOT NULL;
ALTER TABLE workflow_step ADD COLUMN owner_id BIGINT NOT NULL;

// 2. Entity 添加字段
@Entity
public class WorkflowDefinition {
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
}

// 3. Service 层强制过滤
public WorkflowDefinition findByCode(String workflowCode, Long userId) {
    Specification<WorkflowDefinition> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("ownerId"), userId)); // 强制过滤
        predicates.add(cb.equal(root.get("workflowCode"), workflowCode));
        predicates.add(cb.equal(root.get("deleted"), 0));
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    return repository.findOne(spec).orElseThrow(...);
}

// 4. Controller 验证权限
@PostMapping("/execute")
public RESTResult<WorkflowExecuteResult> execute(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    Long userId = AuthTokenFilter.getUserId(request);
    // 验证工作流归属
    WorkflowDefinition def = workflowService.findByCode(workflowCode, userId);
    // ...
}
```

**合规性评分**: 0/100

---

## 5. 错误处理规范

### 5.1 统一异常处理

**标准要求**:
- 使用 `BusinessException` 抛出业务异常
- 错误码使用 `ErrorCode` 常量
- GlobalExceptionHandler 统一捕获

**实际情况**: ✅ 基本合规

**正确示例**:
```java
// WorkflowExecutorImpl.java
if (workflowCode == null || workflowCode.isBlank()) {
    throw new BusinessException(ErrorCode.VALIDATION_FAIL, "workflowCode 不能为空");
}
WorkflowDefinition def = definitionRepository.findByWorkflowCodeAndDeleted(workflowCode, 0)
    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流不存在: " + workflowCode));
```

**优点**:
- ✅ 使用 BusinessException
- ✅ 使用 ErrorCode 常量
- ✅ 错误信息包含上下文

**问题**:
- ⚠️ **P2**: 异常信息不够详细（缺少 workflowCode、userId 等关键参数）
- ⚠️ **P2**: 缺少日志记录（仅在 catch 块中记录）

**合规性评分**: 75/100

---

## 6. 问题清单

### P0 - 阻塞级（必须修复）

| 编号 | 问题 | 文件 | 工作量 | CVSS |
|------|------|------|--------|------|
| P0-1 | 缺少 owner_id 数据隔离 | WorkflowDefinition.java, WorkflowStep.java, schema.sql | 2 人日 | 9.1 |
| P0-2 | 未使用 Specification 动态查询 | WorkflowDefinitionRepository.java, WorkflowStepRepository.java | 1 人日 | - |
| P0-3 | 完全未实现缓存策略 | WorkflowExecutorImpl.java | 1.5 人日 | - |

---

### P1 - 高优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P1-1 | Controller 使用 Map 而非 VO | WorkflowController.java | 0.5 人日 |
| P1-2 | 事务边界过大（含外部 AI 调用）| WorkflowExecutorImpl.java | 1 人日 |
| P1-3 | 缺少 CRUD 接口（list/get/save/delete）| WorkflowController.java | 1 人日 |
| P1-4 | 缺少 SearchVO 和分页查询 | 新增 WorkflowDefinitionSearchVO.java | 0.5 人日 |

---

### P2 - 中优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P2-1 | 异常信息不够详细 | WorkflowExecutorImpl.java | 0.3 人日 |
| P2-2 | 缺少执行历史记录表 | 新增 workflow_execution_log 表 | 1 人日 |
| P2-3 | 缺少失败重试机制 | WorkflowExecutorImpl.java | 1.5 人日 |
| P2-4 | 硬编码步骤逻辑（switch-case）| WorkflowExecutorImpl.java | 2 人日 |

---

### P3 - 低优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P3-1 | 缺少 JavaDoc 注释 | 所有类 | 0.5 人日 |
| P3-2 | 测试覆盖率低（<20%）| 新增测试类 | 1 人日 |
| P3-3 | 缺少前端集成 | 新增前端页面 | 2 人日 |

---

## 7. 修复建议

### 7.1 立即修复（P0）

#### P0-1: 添加 owner_id 数据隔离

**步骤 1: 修改数据库表**
```sql
-- 1. 添加字段
ALTER TABLE workflow_definition ADD COLUMN owner_id BIGINT;
ALTER TABLE workflow_step ADD COLUMN owner_id BIGINT;

-- 2. 迁移现有数据（假设归属于管理员用户 ID=1）
UPDATE workflow_definition SET owner_id = 1 WHERE owner_id IS NULL;
UPDATE workflow_step SET owner_id = 1 WHERE owner_id IS NULL;

-- 3. 设置非空约束
ALTER TABLE workflow_definition ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE workflow_step ALTER COLUMN owner_id SET NOT NULL;

-- 4. 添加索引
CREATE INDEX idx_workflow_definition_owner ON workflow_definition(owner_id) WHERE deleted = 0;
CREATE INDEX idx_workflow_step_owner ON workflow_step(owner_id) WHERE deleted = 0;
```

**步骤 2: 修改 Entity**
```java
// WorkflowDefinition.java
@Entity
@Table(name = "workflow_definition")
@SQLRestriction("deleted = 0")
public class WorkflowDefinition {
    // ... 现有字段
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;  // 新增
}

// WorkflowStep.java
@Entity
@Table(name = "workflow_step")
@SQLRestriction("deleted = 0")
public class WorkflowStep {
    // ... 现有字段
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;  // 新增
}
```

**步骤 3: 修改 Service 层**
```java
// WorkflowDefinitionServiceImpl.java - 新增
@Service
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {
    
    @Resource
    private WorkflowDefinitionRepository repository;
    
    @Override
    public WorkflowDefinition findByCode(String workflowCode, Long userId) {
        Specification<WorkflowDefinition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), userId)); // 强制过滤
            predicates.add(cb.equal(root.get("workflowCode"), workflowCode));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findOne(spec)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, 
                "工作流不存在或无权访问: " + workflowCode));
    }
}

// WorkflowStepServiceImpl.java - 新增
@Service
public class WorkflowStepServiceImpl implements WorkflowStepService {
    
    @Resource
    private WorkflowStepRepository repository;
    
    @Override
    public List<WorkflowStep> findByDefinitionId(Long definitionId, Long userId) {
        Specification<WorkflowStep> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), userId)); // 强制过滤
            predicates.add(cb.equal(root.get("definitionId"), definitionId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, Sort.by("sequenceNo").ascending());
    }
}
```

**步骤 4: 修改 WorkflowExecutorImpl**
```java
@Service
public class WorkflowExecutorImpl implements WorkflowExecutor {
    
    @Resource
    private WorkflowDefinitionService definitionService;  // 改用 Service
    
    @Resource
    private WorkflowStepService stepService;  // 改用 Service
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params) {
        Long userId = paramLong(params, "userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "缺少 userId");
        }
        
        // 使用 Service 层，自动过滤 ownerId
        WorkflowDefinition def = definitionService.findByCode(workflowCode, userId);
        List<WorkflowStep> steps = stepService.findByDefinitionId(def.getId(), userId);
        // ...
    }
}
```

**工作量**: 2 人日

---

#### P0-2: 使用 Specification 动态查询

**步骤 1: 修改 Repository**
```java
// WorkflowDefinitionRepository.java
public interface WorkflowDefinitionRepository extends 
    JpaRepository<WorkflowDefinition, Long>, 
    JpaSpecificationExecutor<WorkflowDefinition> {
    // 移除方法命名查询
}

// WorkflowStepRepository.java
public interface WorkflowStepRepository extends 
    JpaRepository<WorkflowStep, Long>, 
    JpaSpecificationExecutor<WorkflowStep> {
    // 移除方法命名查询
}
```

**步骤 2: 创建 SearchVO**
```java
// WorkflowDefinitionSearchVO.java
@Data
public class WorkflowDefinitionSearchVO extends BasicQueryDto {
    private String workflowCode;
    private String workflowName;
    private Integer status;
    private String keyword;  // 模糊搜索
}
```

**工作量**: 1 人日

---

#### P0-3: 实现两级缓存

**步骤 1: 配置 Caffeine 缓存**
```java
// CacheConfig.java - 添加配置
@Bean
public Cache<String, WorkflowDefinition> workflowDefinitionCache() {
    return Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .recordStats()
        .build();
}
```

**步骤 2: Service 层添加缓存**
```java
// WorkflowDefinitionServiceImpl.java
@Service
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {
    
    @Resource
    private WorkflowDefinitionRepository repository;
    
    @Resource
    private Cache<String, WorkflowDefinition> workflowDefinitionCache;
    
    @Override
    @Cacheable(value = "workflow:definition", key = "#workflowCode + ':' + #userId")
    public WorkflowDefinition findByCode(String workflowCode, Long userId) {
        // L1 缓存
        String cacheKey = workflowCode + ":" + userId;
        WorkflowDefinition cached = workflowDefinitionCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // L2 缓存 + DB（由 @Cacheable 处理）
        Specification<WorkflowDefinition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), userId));
            predicates.add(cb.equal(root.get("workflowCode"), workflowCode));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        WorkflowDefinition result = repository.findOne(spec)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, 
                "工作流不存在或无权访问: " + workflowCode));
        
        // 写入 L1 缓存
        workflowDefinitionCache.put(cacheKey, result);
        return result;
    }
    
    @Override
    @CacheEvict(value = "workflow:definition", key = "#workflowCode + ':' + #userId")
    public void evictCache(String workflowCode, Long userId) {
        String cacheKey = workflowCode + ":" + userId;
        workflowDefinitionCache.invalidate(cacheKey);
    }
}
```

**工作量**: 1.5 人日

---

### 7.2 短期修复（P1）

#### P1-1: 使用强类型 VO 替代 Map

**步骤 1: 创建 VO 类**
```java
// WorkflowExecuteVO.java
@Data
public class WorkflowExecuteVO {
    @NotBlank(message = "workflowCode 不能为空")
    private String workflowCode;
    
    private Long sessionId;
    private Long personaId;
    private Long modelId;
    private String iterateInstruction;
    private Long iterateScriptId;
    
    // 其他参数
    private Map<String, Object> extraParams;
}
```

**步骤 2: 修改 Controller**
```java
// WorkflowController.java
@PostMapping("/execute")
@Operation(summary = "执行工作流")
public RESTResult<WorkflowExecutor.WorkflowExecuteResult> execute(
        HttpServletRequest request,
        @Valid @RequestBody WorkflowExecuteVO vo) {  // 改用强类型 VO
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    
    // 转换为 Map（保持向后兼容）
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    params.put("sessionId", vo.getSessionId());
    params.put("personaId", vo.getPersonaId());
    params.put("modelId", vo.getModelId());
    params.put("iterateInstruction", vo.getIterateInstruction());
    params.put("iterateScriptId", vo.getIterateScriptId());
    if (vo.getExtraParams() != null) {
        params.putAll(vo.getExtraParams());
    }
    
    WorkflowExecutor.WorkflowExecuteResult result = workflowExecutor.execute(vo.getWorkflowCode(), params);
    RESTResult<WorkflowExecutor.WorkflowExecuteResult> r = RESTResult.getSuccess(result);
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

**工作量**: 0.5 人日

---

#### P1-2: 缩小事务边界

**问题**: 当前事务包含外部 AI 调用，可能导致长事务和死锁

**步骤 1: 拆分事务**
```java
// WorkflowExecutorImpl.java
@Service
public class WorkflowExecutorImpl implements WorkflowExecutor {
    
    @Override
    public WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params) {
        // 1. 查询工作流定义（只读事务）
        WorkflowDefinition def = queryWorkflowDefinition(workflowCode, userId);
        List<WorkflowStep> steps = queryWorkflowSteps(def.getId(), userId);
        
        // 2. 执行步骤（无事务，外部调用）
        Map<String, Object> outputs = executeSteps(steps, params);
        
        // 3. 保存结果（写事务）
        saveExecutionResult(def.getId(), outputs, userId);
        
        return new WorkflowExecuteResult(true, "执行成功", outputs, null);
    }
    
    @Transactional(readOnly = true)
    private WorkflowDefinition queryWorkflowDefinition(String workflowCode, Long userId) {
        return definitionService.findByCode(workflowCode, userId);
    }
    
    // 无事务，允许长时间运行
    private Map<String, Object> executeSteps(List<WorkflowStep> steps, Map<String, Object> params) {
        // AI 调用等耗时操作
    }
    
    @Transactional(rollbackFor = Exception.class)
    private void saveExecutionResult(Long definitionId, Map<String, Object> outputs, Long userId) {
        // 仅保存结果到数据库
    }
}
```

**工作量**: 1 人日

---

### 7.3 中期优化（P2）

#### P2-2: 添加执行历史记录表

**步骤 1: 创建数据库表**
```sql
CREATE TABLE IF NOT EXISTS workflow_execution_log (
    id                  BIGSERIAL       PRIMARY KEY,
    definition_id       BIGINT          NOT NULL,
    owner_id            BIGINT          NOT NULL,
    workflow_code       VARCHAR(64)     NOT NULL,
    input_params        TEXT,                           -- JSON
    output_results      TEXT,                           -- JSON
    status              VARCHAR(32)     NOT NULL,       -- SUCCESS/FAILED/RUNNING
    failed_step         VARCHAR(64),
    error_message       TEXT,
    start_time          TIMESTAMP       NOT NULL,
    end_time            TIMESTAMP,
    duration_ms         BIGINT,
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workflow_execution_owner ON workflow_execution_log(owner_id) WHERE deleted = 0;
CREATE INDEX idx_workflow_execution_def ON workflow_execution_log(definition_id) WHERE deleted = 0;
CREATE INDEX idx_workflow_execution_status ON workflow_execution_log(status) WHERE deleted = 0;
```

**步骤 2: 创建 Entity**
```java
// WorkflowExecutionLog.java
@Data
@Entity
@Table(name = "workflow_execution_log")
@SQLRestriction("deleted = 0")
public class WorkflowExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "definition_id", nullable = false)
    private Long definitionId;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(name = "workflow_code", nullable = false)
    private String workflowCode;
    
    @Column(name = "input_params", columnDefinition = "TEXT")
    private String inputParams;
    
    @Column(name = "output_results", columnDefinition = "TEXT")
    private String outputResults;
    
    @Column(name = "status", nullable = false)
    private String status;  // SUCCESS/FAILED/RUNNING
    
    @Column(name = "failed_step")
    private String failedStep;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "start_time", nullable = false)
    private Timestamp startTime;
    
    @Column(name = "end_time")
    private Timestamp endTime;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;
    
    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;
    
    @Column(name = "update_time", nullable = false)
    private Timestamp updateTime;
    
    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }
    
    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
```

**步骤 3: 修改 WorkflowExecutorImpl 记录日志**
```java
@Override
public WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params) {
    Long userId = paramLong(params, "userId");
    WorkflowDefinition def = definitionService.findByCode(workflowCode, userId);
    
    // 创建执行日志
    WorkflowExecutionLog log = new WorkflowExecutionLog();
    log.setDefinitionId(def.getId());
    log.setOwnerId(userId);
    log.setWorkflowCode(workflowCode);
    log.setInputParams(objectMapper.writeValueAsString(params));
    log.setStatus("RUNNING");
    log.setStartTime(new Timestamp(System.currentTimeMillis()));
    log = executionLogRepository.save(log);
    
    try {
        // 执行工作流
        Map<String, Object> outputs = executeSteps(steps, params);
        
        // 更新日志为成功
        log.setStatus("SUCCESS");
        log.setOutputResults(objectMapper.writeValueAsString(outputs));
        log.setEndTime(new Timestamp(System.currentTimeMillis()));
        log.setDurationMs(log.getEndTime().getTime() - log.getStartTime().getTime());
        executionLogRepository.save(log);
        
        return new WorkflowExecuteResult(true, "执行成功", outputs, null);
    } catch (Exception e) {
        // 更新日志为失败
        log.setStatus("FAILED");
        log.setErrorMessage(e.getMessage());
        log.setEndTime(new Timestamp(System.currentTimeMillis()));
        log.setDurationMs(log.getEndTime().getTime() - log.getStartTime().getTime());
        executionLogRepository.save(log);
        throw e;
    }
}
```

**工作量**: 1 人日

---

#### P2-4: 解耦硬编码步骤逻辑

**问题**: 当前使用 switch-case 硬编码步骤逻辑，不可扩展

**步骤 1: 定义步骤执行器接口**
```java
// WorkflowStepExecutor.java
public interface WorkflowStepExecutor {
    String getStepCode();
    Map<String, Object> execute(Map<String, Object> params, Map<String, Object> context);
}
```

**步骤 2: 实现具体步骤执行器**
```java
// GenerateStepExecutor.java
@Component
public class GenerateStepExecutor implements WorkflowStepExecutor {
    
    @Resource
    private LiveAiService liveAiService;
    
    @Override
    public String getStepCode() {
        return "generate";
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> params, Map<String, Object> context) {
        Long sessionId = (Long) params.get("sessionId");
        Long personaId = (Long) params.get("personaId");
        Long modelId = (Long) params.get("modelId");
        
        LiveAiGenerateVO vo = new LiveAiGenerateVO();
        vo.setSessionId(sessionId);
        vo.setPersonaId(personaId);
        vo.setModelId(modelId);
        
        LiveAiFullResultVO full = liveAiService.generateFull(vo);
        
        List<Long> scriptIds = new ArrayList<>();
        if (full != null && full.getScriptIdsForAttribution() != null) {
            scriptIds.addAll(full.getScriptIdsForAttribution());
        }
        
        // 保存到上下文供后续步骤使用
        context.put("scriptIds", scriptIds);
        
        return Map.of(
            "scriptIds", scriptIds,
            "results", full != null && full.getResults() != null ? full.getResults() : List.of()
        );
    }
}

// IterateStepExecutor.java
@Component
public class IterateStepExecutor implements WorkflowStepExecutor {
    @Resource
    private LiveAiService liveAiService;
    
    @Override
    public String getStepCode() {
        return "iterate";
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> params, Map<String, Object> context) {
        // 实现迭代逻辑
    }
}

// ViolationCheckStepExecutor.java, SaveStepExecutor.java 类似
```

**步骤 3: 重构 WorkflowExecutorImpl**
```java
@Service
public class WorkflowExecutorImpl implements WorkflowExecutor {
    
    @Resource
    private WorkflowDefinitionService definitionService;
    
    @Resource
    private WorkflowStepService stepService;
    
    // 自动注入所有步骤执行器
    private final Map<String, WorkflowStepExecutor> executorMap;
    
    public WorkflowExecutorImpl(List<WorkflowStepExecutor> executors) {
        this.executorMap = executors.stream()
            .collect(Collectors.toMap(WorkflowStepExecutor::getStepCode, e -> e));
    }
    
    @Override
    public WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params) {
        Long userId = paramLong(params, "userId");
        WorkflowDefinition def = definitionService.findByCode(workflowCode, userId);
        List<WorkflowStep> steps = stepService.findByDefinitionId(def.getId(), userId);
        
        Map<String, Object> outputs = new LinkedHashMap<>();
        Map<String, Object> context = new HashMap<>();  // 跨步骤上下文
        
        try {
            for (WorkflowStep step : steps) {
                String stepCode = step.getStepCode();
                
                // 查找步骤执行器
                WorkflowStepExecutor executor = executorMap.get(stepCode);
                if (executor == null) {
                    log.warn("未找到步骤执行器: {}", stepCode);
                    continue;
                }
                
                // 执行步骤
                Map<String, Object> stepOutput = executor.execute(params, context);
                outputs.put(stepCode, stepOutput);
            }
            
            return new WorkflowExecuteResult(true, "执行成功", outputs, null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("工作流执行失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.WORKFLOW_EXECUTION_FAILED, "工作流执行失败: " + e.getMessage());
        }
    }
}
```

**优点**:
- ✅ 解耦步骤逻辑，每个步骤独立实现
- ✅ 易于扩展，新增步骤只需实现 WorkflowStepExecutor 接口
- ✅ 支持跨步骤上下文传递
- ✅ 符合开闭原则（对扩展开放，对修改关闭）

**工作量**: 2 人日

---

### 7.4 长期改进（P3）

#### P3-1: 添加 JavaDoc 注释

**示例**:
```java
/**
 * 工作流执行器实现类
 * 
 * <p>负责按步骤顺序执行工作流，支持：
 * <ul>
 *   <li>动态步骤编排</li>
 *   <li>跨步骤上下文传递</li>
 *   <li>执行历史记录</li>
 *   <li>失败重试机制</li>
 * </ul>
 * 
 * @author dy05
 * @since 1.0.0
 */
@Service
public class WorkflowExecutorImpl implements WorkflowExecutor {
    
    /**
     * 执行工作流
     * 
     * @param workflowCode 工作流编码，如 live_script_full
     * @param params 执行参数，必须包含 userId
     * @return 执行结果，包含各步骤输出和最终状态
     * @throws BusinessException 当工作流不存在或执行失败时
     */
    @Override
    public WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params) {
        // ...
    }
}
```

**工作量**: 0.5 人日

---

#### P3-2: 提升测试覆盖率

**当前状态**: 仅 1 个测试文件（WorkflowControllerTest.java），覆盖率 <20%

**目标**: 覆盖率 ≥80%

**需要添加的测试**:
```java
// WorkflowExecutorImplTest.java - 单元测试
@SpringBootTest
class WorkflowExecutorImplTest {
    
    @Test
    void execute_success_shouldReturnResult() {
        // 测试正常执行流程
    }
    
    @Test
    void execute_workflowNotFound_shouldThrowException() {
        // 测试工作流不存在
    }
    
    @Test
    void execute_noPermission_shouldThrowException() {
        // 测试无权限访问
    }
    
    @Test
    void execute_stepFailed_shouldRollback() {
        // 测试步骤失败回滚
    }
}

// WorkflowDefinitionServiceImplTest.java - 单元测试
@SpringBootTest
class WorkflowDefinitionServiceImplTest {
    
    @Test
    void findByCode_withCache_shouldReturnCached() {
        // 测试缓存命中
    }
    
    @Test
    void findByCode_differentUser_shouldReturnDifferent() {
        // 测试数据隔离
    }
}

// WorkflowIntegrationTest.java - 集成测试
@SpringBootTest
@Transactional
class WorkflowIntegrationTest {
    
    @Test
    void fullWorkflow_liveScriptGeneration_shouldSuccess() {
        // 测试完整的直播话术生成流程
    }
}
```

**工作量**: 1 人日

---

## 8. 总结

**总体评估**: 58/100 (Grade D+)

**关键指标**:
- P0 问题: 3 个（数据隔离、Specification 查询、缓存策略）
- P1 问题: 4 个（VO 类型、事务边界、CRUD 接口、分页查询）
- P2 问题: 4 个（异常信息、执行日志、重试机制、硬编码逻辑）
- P3 问题: 3 个（JavaDoc、测试覆盖率、前端集成）
- 总工作量: 16.8 人日

**优先级建议**:
1. **P0 问题必须在 1 周内修复**（4.5 人日）
   - 数据隔离是严重安全漏洞（CVSS 9.1）
   - Specification 查询是架构规范要求
   - 缓存策略影响性能
2. **P1 问题建议在 2 周内修复**（3 人日）
   - 提升代码质量和可维护性
   - 完善 API 功能
3. **P2/P3 问题可纳入技术债务管理**（9.3 人日）
   - 逐步优化，不影响核心功能

**合规性改进路径**:
```
当前: 58/100 (D+)
  ↓ 修复 P0 问题
阶段 1: 75/100 (B)
  ↓ 修复 P1 问题
阶段 2: 85/100 (A-)
  ↓ 修复 P2/P3 问题
目标: 95/100 (A+)
```

**架构演进建议**:
1. **短期**（1-2 周）: 修复 P0 安全漏洞和架构合规问题
2. **中期**（1 个月）: 完善功能（CRUD、分页、执行日志）
3. **长期**（2-3 个月）: 重构为通用工作流引擎，支持更多场景

---

**报告生成**: Claude Code (Opus 4.6)  
**审查状态**: 待人工复核  
**下一步行动**: 创建 P0 问题修复任务，分配给开发团队
