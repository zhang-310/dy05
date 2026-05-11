# Live 模块 Sprint 1 详细修复计划

**模块**: live  
**Sprint**: Sprint 1（P0 问题修复）  
**周期**: 2026-05-11 ~ 2026-05-25（2 周）  
**负责人**: 后端开发者 A + B

---

## 任务 B1.1: LiveScriptVO 字段补全

### 问题描述
LiveScript Entity 有 30 个字段，但 LiveScriptVO 只有 8 个字段，导致前端无法展示关键业务数据（话术类型、风格、效果评分等）。

### 影响范围
- 前端无法展示话术类型、风格
- 无法显示 AI 生成标识
- 无法显示效果评分和数据变化
- 数据分析功能严重受限

### 修复方案

#### 步骤 1: 分析 Entity 字段（15 分钟）

```bash
# 查看 LiveScript Entity
cat douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/entity/LiveScript.java
```

**Entity 字段清单**（30 个）:
```java
// 基础字段（8 个 - VO 已有）
private Long id;
private Long sessionId;
private String scriptContent;
private Integer sequenceNo;
private Timestamp executionTime;
private Boolean executed;
private Timestamp actualExecutionTime;
private Timestamp createTime;
private Timestamp updateTime;

// 缺失字段（22 个 - 需要添加）
private String scriptType;           // 话术类型（opening/product/closing 等）
private String style;                 // 话术风格（专业/友好/激情等）
private Boolean aiGenerated;          // 是否 AI 生成
private Long productId;               // 关联商品 ID
private Long aiCallLogId;             // AI 调用日志 ID
private String generationStatus;      // 生成状态（pending/completed/failed）
private Boolean violationChecked;     // 是否违规检测
private String violationResult;       // 违规检测结果
private Integer viewerDelta;          // 观看人数变化
private Integer interactionDelta;     // 互动量变化
private Integer conversionDelta;      // 转化量变化
private Double effectivenessScore;    // 效果评分（0-100）
private Integer durationLimitSec;     // 时长限制（秒）
private String requirement;           // 生成要求
private Long referencedScriptId;      // 参考话术 ID
private String referencedScriptSnapshot; // 参考话术快照
private String approvalStatus;        // 审批状态
private Long userId;                  // 用户 ID
private String generationPromptHash;  // 生成提示哈希
private Long abExperimentId;          // AB 实验 ID
private Long abVariantId;             // AB 变体 ID
private String aiSuggestion;          // AI 建议
private Long promptTemplateId;        // 提示模板 ID
```

#### 步骤 2: 修改 LiveScriptVO（30 分钟）

**文件位置**: `douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/vo/LiveScriptVO.java`

```java
package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 直播话术 VO
 * 完整版本 - 包含所有 30 个字段
 */
@Data
public class LiveScriptVO {
    // ===== 基础字段（原有 8 个）=====
    private Long id;
    private Long sessionId;
    private String scriptContent;
    private Integer sequenceNo;
    private Timestamp executionTime;
    private Boolean executed;
    private Timestamp actualExecutionTime;
    private Timestamp createTime;
    private Timestamp updateTime;

    // ===== 新增字段（22 个）=====
    
    // 话术元数据
    private String scriptType;           // 话术类型
    private String style;                 // 话术风格
    private Boolean aiGenerated;          // 是否 AI 生成
    private Long productId;               // 关联商品 ID
    
    // AI 生成相关
    private Long aiCallLogId;             // AI 调用日志 ID
    private String generationStatus;      // 生成状态
    private String requirement;           // 生成要求
    private String generationPromptHash;  // 生成提示哈希
    private Long promptTemplateId;        // 提示模板 ID
    private String aiSuggestion;          // AI 建议
    
    // 质量与合规
    private Boolean violationChecked;     // 是否违规检测
    private String violationResult;       // 违规检测结果
    private String approvalStatus;        // 审批状态
    
    // 效果数据
    private Integer viewerDelta;          // 观看人数变化
    private Integer interactionDelta;     // 互动量变化
    private Integer conversionDelta;      // 转化量变化
    private Double effectivenessScore;    // 效果评分
    
    // 其他
    private Integer durationLimitSec;     // 时长限制
    private Long referencedScriptId;      // 参考话术 ID
    private String referencedScriptSnapshot; // 参考话术快照
    private Long userId;                  // 用户 ID
    private Long abExperimentId;          // AB 实验 ID
    private Long abVariantId;             // AB 变体 ID
}
```

#### 步骤 3: 更新 Service 层转换方法（30 分钟）

**文件位置**: `douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/service/impl/LiveScriptServiceImpl.java`

找到 `toVO()` 方法并更新：

```java
private LiveScriptVO toVO(LiveScript entity) {
    if (entity == null) return null;
    
    LiveScriptVO vo = new LiveScriptVO();
    
    // 基础字段
    vo.setId(entity.getId());
    vo.setSessionId(entity.getSessionId());
    vo.setScriptContent(entity.getScriptContent());
    vo.setSequenceNo(entity.getSequenceNo());
    vo.setExecutionTime(entity.getExecutionTime());
    vo.setExecuted(entity.getExecuted());
    vo.setActualExecutionTime(entity.getActualExecutionTime());
    vo.setCreateTime(entity.getCreateTime());
    vo.setUpdateTime(entity.getUpdateTime());
    
    // 新增字段
    vo.setScriptType(entity.getScriptType());
    vo.setStyle(entity.getStyle());
    vo.setAiGenerated(entity.getAiGenerated());
    vo.setProductId(entity.getProductId());
    vo.setAiCallLogId(entity.getAiCallLogId());
    vo.setGenerationStatus(entity.getGenerationStatus());
    vo.setViolationChecked(entity.getViolationChecked());
    vo.setViolationResult(entity.getViolationResult());
    vo.setViewerDelta(entity.getViewerDelta());
    vo.setInteractionDelta(entity.getInteractionDelta());
    vo.setConversionDelta(entity.getConversionDelta());
    vo.setEffectivenessScore(entity.getEffectivenessScore());
    vo.setDurationLimitSec(entity.getDurationLimitSec());
    vo.setRequirement(entity.getRequirement());
    vo.setReferencedScriptId(entity.getReferencedScriptId());
    vo.setReferencedScriptSnapshot(entity.getReferencedScriptSnapshot());
    vo.setApprovalStatus(entity.getApprovalStatus());
    vo.setUserId(entity.getUserId());
    vo.setGenerationPromptHash(entity.getGenerationPromptHash());
    vo.setAbExperimentId(entity.getAbExperimentId());
    vo.setAbVariantId(entity.getAbVariantId());
    vo.setAiSuggestion(entity.getAiSuggestion());
    vo.setPromptTemplateId(entity.getPromptTemplateId());
    
    return vo;
}
```

**优化建议**: 使用 MapStruct 自动映射（可选）

```java
// 添加依赖到 pom.xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

// 创建 Mapper 接口
@Mapper(componentModel = "spring")
public interface LiveScriptMapper {
    LiveScriptVO toVO(LiveScript entity);
    List<LiveScriptVO> toVOList(List<LiveScript> entities);
}
```

#### 步骤 4: 编译验证（5 分钟）

```bash
# 编译 live 模块
mvn compile -pl douyin-operations-live -am

# 运行测试
mvn test -pl douyin-operations-live -Dtest=LiveScriptServiceImplTest
```

#### 步骤 5: 提交代码（5 分钟）

```bash
git add douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/vo/LiveScriptVO.java
git add douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/service/impl/LiveScriptServiceImpl.java
git commit -m "fix(live): 补全 LiveScriptVO 22 个缺失字段

- 添加话术元数据字段（scriptType, style, aiGenerated, productId）
- 添加 AI 生成相关字段（aiCallLogId, generationStatus 等）
- 添加质量与合规字段（violationChecked, approvalStatus）
- 添加效果数据字段（viewerDelta, effectivenessScore 等）
- 更新 Service 层 toVO() 方法
- 与 LiveScript Entity 保持完全一致

关联任务: B1.1
影响范围: 前端可展示完整话术数据"
```

### 验收标准
- [ ] LiveScriptVO 包含所有 30 个字段
- [ ] toVO() 方法正确映射所有字段
- [ ] 编译通过（`mvn compile`）
- [ ] 现有测试通过
- [ ] 代码审查通过

### 工作量
- **预估**: 4 小时
- **实际**: ___ 小时

---

## 任务 B1.2: LiveSessionSaveVO 字段补全

### 问题描述
LiveSessionSaveVO 缺少 `personaId`, `scriptStyle`, `sessionType`, `liveFormat` 等字段，导致创建场次时无法设置这些属性。

### 修复方案

#### 步骤 1: 修改 LiveSessionSaveVO（15 分钟）

**文件位置**: `douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/vo/LiveSessionSaveVO.java`

```java
package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;

/**
 * 直播场次保存 VO
 */
@Data
public class LiveSessionSaveVO {
    private Long id;  // 更新时必填
    
    @NotBlank(message = "直播标题不能为空")
    @Size(max = 128, message = "直播标题不能超过 128 字符")
    private String liveTitle;
    
    @Size(max = 512, message = "直播描述不能超过 512 字符")
    private String liveDescription;
    
    private Timestamp scheduledTime;
    
    @Min(value = 0, message = "状态值无效")
    @Max(value = 2, message = "状态值无效")
    private Integer status;  // 0-待开播, 1-直播中, 2-已结束
    
    // ===== 新增字段 =====
    
    @NotNull(message = "人设 ID 不能为空")
    private Long personaId;  // 关联人设
    
    @NotBlank(message = "话术风格不能为空")
    private String scriptStyle;  // 话术风格（专业/友好/激情/种草/促销）
    
    @NotBlank(message = "场次类型不能为空")
    private String sessionType;  // 场次类型（普通/品牌专场/大促）
    
    @NotBlank(message = "直播形式不能为空")
    private String liveFormat;  // 直播形式（单人/多人/连麦）
    
    private Long accountId;  // 抖音账号 ID（可选）
}
```

#### 步骤 2: 更新 Service 层（15 分钟）

**文件位置**: `douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/service/impl/LiveSessionServiceImpl.java`

找到 `save()` 方法并更新：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long save(LiveSessionSaveVO vo) {
    // 参数校验
    if (vo == null) {
        throw new BusinessException(ErrorCode.VALIDATION_FAIL, "保存参数不能为空");
    }
    
    LiveSession session;
    if (vo.getId() != null && vo.getId() > 0) {
        // 更新
        session = liveSessionRepository.findByIdAndDeleted(vo.getId(), 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "场次不存在"));
    } else {
        // 新增
        session = new LiveSession();
        session.setDeleted(0);
    }
    
    // 设置字段
    session.setLiveTitle(vo.getLiveTitle());
    session.setLiveDescription(vo.getLiveDescription());
    session.setScheduledTime(vo.getScheduledTime());
    if (vo.getStatus() != null) {
        session.setStatus(vo.getStatus());
    }
    
    // 新增字段
    session.setPersonaId(vo.getPersonaId());
    session.setScriptStyle(vo.getScriptStyle());
    session.setSessionType(vo.getSessionType());
    session.setLiveFormat(vo.getLiveFormat());
    if (vo.getAccountId() != null) {
        session.setAccountId(vo.getAccountId());
    }
    
    session = liveSessionRepository.save(session);
    return session.getId();
}
```

#### 步骤 3: 验证（10 分钟）

```bash
# 编译
mvn compile -pl douyin-operations-live -am

# 测试
mvn test -pl douyin-operations-live -Dtest=LiveSessionServiceImplTest
```

### 验收标准
- [ ] LiveSessionSaveVO 包含所有必需字段
- [ ] 字段校验注解正确
- [ ] Service 层正确处理新字段
- [ ] 编译通过
- [ ] 测试通过

### 工作量
- **预估**: 2 小时
- **实际**: ___ 小时

---

## 任务 B1.3: 添加缓存实现

### 问题描述
所有查询直接访问数据库，无缓存策略，导致：
- 数据库负载高
- 响应时间慢（200-500ms）
- 高并发性能差

### 修复方案

#### 步骤 1: 配置 Caffeine 缓存（30 分钟）

**文件位置**: `douyin-operations-common/src/main/java/cn/gaifan/douyinOperations/common/config/CacheConfig.java`

```java
package cn.gaifan.douyinOperations.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置 - L1（Caffeine）+ L2（Redis）
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // ===== L1 缓存（Caffeine 本地缓存）=====
    
    /**
     * 话术缓存（5 分钟，最大 1000 条）
     */
    @Bean(name = "liveScriptCache")
    public Cache<Long, Object> liveScriptCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }
    
    /**
     * 场次缓存（5 分钟，最大 500 条）
     */
    @Bean(name = "liveSessionCache")
    public Cache<Long, Object> liveSessionCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }
    
    /**
     * 账号统计缓存（5 分钟，最大 500 条）
     */
    @Bean(name = "accountStatisticsCache")
    public Cache<Long, Object> accountStatisticsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }

    // ===== L2 缓存（Redis 分布式缓存）=====
    
    /**
     * Redis 缓存管理器
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // 默认 5 分钟
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
```

#### 步骤 2: 在 Service 层使用缓存（45 分钟）

**文件位置**: `douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/service/impl/LiveScriptServiceImpl.java`

```java
@Service
public class LiveScriptServiceImpl implements LiveScriptService {
    
    @Resource
    private LiveScriptRepository liveScriptRepository;
    
    @Resource(name = "liveScriptCache")
    private Cache<Long, Object> liveScriptCache;
    
    /**
     * 获取话术（带 L1 + L2 缓存）
     */
    @Override
    @Cacheable(value = "liveScript", key = "#id", unless = "#result == null")
    public LiveScriptVO get(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        }
        
        // 1. 先查 L1 本地缓存（Caffeine）
        Object cached = liveScriptCache.getIfPresent(id);
        if (cached instanceof LiveScriptVO) {
            log.debug("L1 缓存命中: scriptId={}", id);
            return (LiveScriptVO) cached;
        }
        
        // 2. L1 未命中，查数据库（L2 Redis 缓存由 @Cacheable 自动处理）
        LiveScript entity = liveScriptRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "话术不存在"));
        
        LiveScriptVO vo = toVO(entity);
        
        // 3. 写入 L1 本地缓存
        liveScriptCache.put(id, vo);
        
        return vo;
    }
    
    /**
     * 保存话术（清除缓存）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "liveScript", key = "#result")
    public Long save(LiveScriptSaveVO vo) {
        // ... 保存逻辑
        
        Long scriptId = entity.getId();
        
        // 清除 L1 缓存
        liveScriptCache.invalidate(scriptId);
        
        return scriptId;
    }
    
    /**
     * 删除话术（清除缓存）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "liveScript", key = "#id")
    public void delete(Long id) {
        // ... 删除逻辑
        
        // 清除 L1 缓存
        liveScriptCache.invalidate(id);
    }
}
```

#### 步骤 3: 验证缓存效果（15 分钟）

```bash
# 1. 启动应用
mvn spring-boot:run -pl douyin-operations-app

# 2. 测试缓存命中
curl -X POST http://localhost:8080/api/v1/live/script/get \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id": 1}'

# 3. 查看日志（应该看到 "L1 缓存命中"）

# 4. 查看 Redis 缓存
redis-cli
> KEYS liveScript::*
> GET liveScript::1
```

### 验收标准
- [ ] L1 缓存（Caffeine）配置正确
- [ ] L2 缓存（Redis）配置正确
- [ ] Service 层正确使用缓存
- [ ] 缓存命中率 > 80%（生产环境）
- [ ] 响应时间从 200ms → 10ms（缓存命中）

### 工作量
- **预估**: 8 小时
- **实际**: ___ 小时

---

## 任务进度追踪

| 任务 | 状态 | 开始时间 | 完成时间 | 实际工作量 | 备注 |
|------|------|----------|----------|------------|------|
| B1.1 | ⚠️ TODO | - | - | - | - |
| B1.2 | ⚠️ TODO | - | - | - | - |
| B1.3 | ⚠️ TODO | - | - | - | - |
| B1.4 | ⚠️ TODO | - | - | - | - |
| B1.5 | ⚠️ TODO | - | - | - | - |

---

**创建时间**: 2026-05-11  
**最后更新**: 2026-05-11  
**负责人**: 后端开发者 A + B
