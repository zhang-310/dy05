# Config 模块模式合规性检查报告

**检查日期**: 2026-05-08
**检查范围**: config 模块（系统配置 + 行业分类）
**检查标准**: docs/adr/ 架构决策记录

## 执行摘要

**总体评分**: 75/100 (等级 B)

| 模式 | 合规性 | 问题数 |
|------|--------|--------|
| API 规范（ADR-001）| ✅ 完全合规 | 0 |
| 数据访问（ADR-003）| ✅ 完全合规 | 0 |
| 数据隔离（ADR-004）| ❌ 不适用 | 3 |
| 逻辑删除（ADR-005）| ✅ 完全合规 | 0 |
| 缓存策略 | ⚠️ 部分合规 | 1 |
| 错误码规范 | ✅ 完全合规 | 0 |

**关键发现**:
- ✅ API 接口完全遵循统一 POST 规范
- ✅ 使用 JPA Specification 动态查询
- ✅ 逻辑删除实现正确（@SQLRestriction）
- ✅ 错误码使用规范
- ⚠️ 缓存策略部分合规（仅 L2 Redis，无 L1 Caffeine）
- ❌ **核心问题**：config 模块是全局配置，不需要 ownerId 数据隔离（设计合理，但与标准模式不同）

## 详细检查结果

### 1. API 规范（ADR-001）

**标准**: 统一 POST 接口，路径 /api/v1/<模块>/<资源>/<动作>

**检查结果**: ✅ 完全合规

**分析**:
- ✅ 所有接口使用 POST 方法
- ✅ 路径符合规范：`/api/v1/config/{list|get|save|delete}`
- ✅ 使用 `@RequestBody` 接收参数
- ✅ 返回 `RESTResult<T>` 统一响应体

**接口清单**:
| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/config/list` | POST | 分页查询配置列表 |
| `/api/v1/config/get` | POST | 按 key 获取配置 |
| `/api/v1/config/save` | POST | 保存配置（新增或更新）|
| `/api/v1/config/delete` | POST | 删除配置 |

**代码示例**:
```java
@PostMapping("/list")
public RESTResult<PageResultVO<ConfigVO>> list(
    HttpServletRequest request, 
    @RequestBody(required = false) ConfigSearchVO vo) {
    // ...
}
```

### 2. 数据访问模式（ADR-003）

**标准**: JPA Specification 动态查询

**检查结果**: ✅ 完全合规

**分析**:
- ✅ Repository 继承 `JpaSpecificationExecutor<SysConfig>`
- ✅ Service 层使用 Specification 构建动态查询
- ✅ 支持多条件组合（configKey、configGroup、keyword）
- ✅ 与 Spring Data 分页/排序无缝集成

**代码示例**:
```java
Specification<SysConfig> spec = (root, query, cb) -> {
    List<Predicate> list = new ArrayList<>();
    list.add(cb.equal(root.get("deleted"), 0));
    if (q.getConfigKey() != null && !q.getConfigKey().trim().isEmpty()) {
        list.add(cb.like(root.get("configKey"), "%" + q.getConfigKey().trim() + "%"));
    }
    if (q.getConfigGroup() != null && !q.getConfigGroup().trim().isEmpty()) {
        list.add(cb.equal(root.get("configGroup"), q.getConfigGroup().trim()));
    }
    if (q.getKeyword() != null && !q.getKeyword().trim().isEmpty()) {
        String k = "%" + q.getKeyword().trim() + "%";
        list.add(cb.or(
            cb.like(root.get("configKey"), k),
            cb.like(root.get("configValue"), k),
            cb.like(root.get("remark"), k)
        ));
    }
    return cb.and(list.toArray(new Predicate[0]));
};
```

**优点**:
- 灵活组合查询条件
- 类型安全，编译期检查
- 支持模糊查询（LIKE）和精确匹配（EQUAL）

### 3. 数据隔离（ADR-004）

**标准**: ownerId 强制过滤

**检查结果**: ❌ 不适用（设计合理）

**分析**:
- ❌ `sys_config` 表无 `owner_id` 字段
- ❌ `sys_industry` 表无 `owner_id` 字段
- ❌ `sys_config_group` 表无 `owner_id` 字段
- ✅ **设计合理**：config 模块是全局系统配置，不属于任何用户，不需要数据隔离
- ✅ 通过角色权限控制访问（仅管理员可操作）

**权限控制**:
```java
private boolean isAdmin(HttpServletRequest request) {
    return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
}

// 所有接口都检查管理员权限
if (!isAdmin(request)) {
    return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
}
```

**设计决策**:
- config 模块是**平台级配置**，不是用户级数据
- 使用**角色权限**（admin）而非数据隔离（ownerId）
- 符合业务语义：系统配置应该全局唯一，不应该每个用户一份

**对比其他模块**:
| 模块 | 数据隔离 | 原因 |
|------|----------|------|
| live | ✅ ownerId | 用户私有数据（直播场次） |
| shortvideo | ✅ ownerId | 用户私有数据（短视频项目） |
| config | ❌ 无 ownerId | 全局系统配置 |
| auth | ❌ 无 ownerId | 用户账号本身 |

### 4. 逻辑删除（ADR-005）

**标准**: @SQLRestriction("deleted = 0")

**检查结果**: ✅ 完全合规

**分析**:
- ✅ 所有 Entity 使用 `@SQLRestriction("deleted = 0")`
- ✅ 数据库表有 `deleted` 字段（默认 0）
- ✅ 删除操作设置 `deleted = 1`，不物理删除
- ✅ 查询自动过滤已删除记录

**Entity 示例**:
```java
@Entity
@Table(name = "sys_config")
@SQLRestriction("deleted = 0")
public class SysConfig {
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;
}
```

**删除操作**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
@CacheEvict(value = "config", allEntries = true)
public void deleteById(Long id) {
    if (id == null) return;
    Optional<SysConfig> opt = sysConfigRepository.findById(id);
    if (!opt.isPresent()) return;
    SysConfig r = opt.get();
    r.setDeleted(1);  // 逻辑删除
    sysConfigRepository.save(r);
}
```

**数据库约束**:
```sql
-- 配置键唯一（排除已删除记录）
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_key 
ON sys_config (config_key) WHERE deleted = 0;
```

### 5. 缓存策略

**标准**: L1 Caffeine + L2 Redis

**检查结果**: ⚠️ 部分合规

**分析**:
- ✅ 使用 Spring Cache 抽象（`@Cacheable`、`@CacheEvict`）
- ✅ 缓存名称：`config`
- ✅ 缓存键：`#key`（配置键）
- ✅ 写操作清除缓存（`@CacheEvict(allEntries = true)`）
- ⚠️ **仅 L2 Redis**，未配置 L1 Caffeine 本地缓存
- ⚠️ `getRawValueByKey()` 故意不加缓存（避免 Redis 未启动时失败）

**缓存实现**:
```java
@Override
@Cacheable(value = "config", key = "#key")
public ConfigVO getByKey(String key) {
    if (key == null || key.trim().isEmpty()) return null;
    return sysConfigRepository.findByConfigKeyAndDeleted(key.trim(), 0)
        .map(this::toVO).orElse(null);
}

@Override
@Transactional(rollbackFor = Exception.class)
@CacheEvict(value = "config", allEntries = true)
public long save(ConfigSaveVO vo, Long operatorId) {
    // ...
}
```

**特殊设计**:
```java
/**
 * 原始配置值（不脱敏）。故意不加 @Cacheable：与 {@link #getByKey} 共用 Redis 缓存时，
 * 若 Redis 未启动，缓存切面会在查库前失败，导致定时任务等频繁 ERROR。
 * 读库成本低，可接受；变更后依赖 {@link #save} 的 @CacheEvict 刷新 getByKey 缓存。
 */
@Override
public String getRawValueByKey(String key) {
    if (key == null || key.trim().isEmpty()) return null;
    return sysConfigRepository.findByConfigKeyAndDeleted(key.trim(), 0)
            .map(SysConfig::getConfigValue).orElse(null);
}
```

**问题**:
1. 未配置 L1 Caffeine 本地缓存（项目标准是 L1+L2）
2. `getRawValueByKey()` 不缓存，可能导致频繁查库

**建议**:
- 配置 L1 Caffeine 缓存（参考其他模块）
- 考虑为 `getRawValueByKey()` 添加独立缓存（避免与 `getByKey()` 冲突）

### 6. 错误码规范

**标准**: ErrorCode 常量类

**检查结果**: ✅ 完全合规

**分析**:
- ✅ 使用 `ErrorCode` 常量类
- ✅ 错误码语义明确
- ✅ Controller 和 Service 层统一使用

**错误码使用**:
| 错误码 | 说明 | 使用场景 |
|--------|------|----------|
| `ErrorCode.UNAUTHORIZED` | 未登录 | Controller 认证检查 |
| `ErrorCode.FORBIDDEN` | 权限不足 | Controller 权限检查 |
| `ErrorCode.CONFIG_NOT_FOUND` | 配置不存在 | Service 查询失败 |
| `ErrorCode.CONFIG_SAVE_FAIL` | 配置保存失败 | Service 保存失败 |

**代码示例**:
```java
// Controller
if (AuthTokenFilter.getUserId(request) == null) {
    return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
}
if (!isAdmin(request)) {
    return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
}

// Service
if (vo == null) {
    throw new BusinessException(ErrorCode.CONFIG_SAVE_FAIL, "参数不能为空");
}
entity = sysConfigRepository.findById(vo.getId()).orElseThrow(
    () -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND, "配置不存在"));
```

## 问题清单

### P0 - 阻塞级

无

### P1 - 高优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P1-1 | 缺少 L1 Caffeine 本地缓存 | ConfigServiceImpl.java | 0.5 人日 |

**详细说明**:
- **问题**: 项目标准是 L1 Caffeine + L2 Redis 双层缓存，config 模块仅使用 L2 Redis
- **影响**: 每次查询都需要访问 Redis，增加网络开销
- **修复**: 配置 L1 Caffeine 缓存（参考 `CacheConfig.java`）

### P2 - 中优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P2-1 | `getRawValueByKey()` 不缓存 | ConfigServiceImpl.java | 0.3 人日 |
| P2-2 | 前端类型定义不完整 | front/src/api/config.ts | 0.2 人日 |

**P2-1 详细说明**:
- **问题**: `getRawValueByKey()` 故意不加缓存，可能导致频繁查库
- **影响**: 定时任务等场景频繁调用时性能较差
- **修复**: 添加独立缓存（如 `config-raw`），避免与 `getByKey()` 冲突

**P2-2 详细说明**:
- **问题**: 前端 `SysConfig` 接口缺少字段（valueType、isSensitive、configGroup、remark）
- **影响**: 前端无法访问完整配置信息
- **修复**: 补全前端类型定义，与后端 `ConfigVO` 对齐

```typescript
// 当前定义（不完整）
export interface SysConfig { 
  id: number; 
  configKey: string; 
  configValue: string; 
  description: string; 
  createTime: string 
}

// 应该是（完整）
export interface SysConfig {
  id: number
  configKey: string
  configValue: string
  valueType: string
  isSensitive: number
  configGroup: string
  remark: string
  createTime?: string
  updateTime?: string
}
```

### P3 - 低优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P3-1 | 缺少 SysIndustry 的 Controller | 无 | 1 人日 |
| P3-2 | 缺少 SysConfigGroup 的 Controller | 无 | 1 人日 |

**P3-1 详细说明**:
- **问题**: `sys_industry` 表有 Entity，但无 Controller/Service
- **影响**: 前端无法管理行业分类
- **修复**: 添加 `IndustryController` + `IndustryService`（参考 `ConfigController`）

**P3-2 详细说明**:
- **问题**: `sys_config_group` 表有 Entity，但无 Controller/Service
- **影响**: 前端无法管理配置分组
- **修复**: 添加 `ConfigGroupController` + `ConfigGroupService`

## 修复建议

### 短期（1-2 周）

1. **P1-1: 添加 L1 Caffeine 缓存**
   - 在 `CacheConfig.java` 中配置 `config` 缓存
   - 验证缓存命中率

2. **P2-2: 补全前端类型定义**
   - 更新 `front/src/api/config.ts`
   - 与后端 `ConfigVO` 对齐

### 中期（1 个月）

1. **P2-1: 优化 `getRawValueByKey()` 缓存**
   - 添加独立缓存 `config-raw`
   - 避免与 `getByKey()` 冲突

### 长期（持续改进）

1. **P3-1: 实现 SysIndustry 管理**
   - 添加 Controller/Service/VO
   - 前端页面

2. **P3-2: 实现 SysConfigGroup 管理**
   - 添加 Controller/Service/VO
   - 前端页面

## 总结

**总工作量**: 3.0 人日

**优先级分布**:
- P0: 0 个问题，0 人日
- P1: 1 个问题，0.5 人日
- P2: 2 个问题，0.5 人日
- P3: 2 个问题，2.0 人日

**下一步行动**:
1. 配置 L1 Caffeine 缓存（P1-1）
2. 补全前端类型定义（P2-2）
3. 评估是否需要实现 SysIndustry 和 SysConfigGroup 管理（P3）

**整体评价**:
- config 模块整体质量较高，核心模式合规性良好
- 主要问题是缓存策略不完整（缺少 L1 Caffeine）
- 数据隔离不适用是合理的设计决策（全局配置）
- 建议优先修复 P1 和 P2 问题，P3 问题可根据业务需求决定是否实现
