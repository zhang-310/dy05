# Log 模块模式合规性检查报告

**检查日期**: 2026-05-08  
**检查范围**: log 模块（审计与操作日志）  
**检查标准**: docs/adr/ 架构决策记录  
**模块位置**: `douyin-operations-platform/src/main/java/cn/gaifan/douyinOperations/module/log/`

## 执行摘要

**总体评分**: 72/100 (等级 C)

| 模式 | 合规性 | 问题数 |
|------|--------|--------|
| API 规范（ADR-001）| ✅ 完全合规 | 0 |
| 数据访问（ADR-003）| ✅ 完全合规 | 0 |
| 数据隔离（ADR-004）| ❌ 不适用但缺少说明 | 1 |
| 逻辑删除（ADR-005）| ❌ 未实现 | 2 |
| 缓存策略 | ⚠️ 未实现（合理） | 0 |
| 错误码规范 | ⚠️ 部分合规 | 1 |

**关键发现**:
- ✅ API 设计完全符合统一 POST 规范，包含查询和导出功能
- ✅ 使用 JPA Specification 实现灵活的动态查询
- ✅ 自动化日志采集机制（OperationLogFilter）设计优秀
- ❌ 日志表缺少逻辑删除字段（deleted），无法支持日志归档需求
- ❌ 缺少数据隔离说明文档（日志是全局资源还是租户隔离）
- ⚠️ 错误处理仅使用 UNAUTHORIZED，缺少其他业务错误码

## 详细检查结果

### 1. API 规范（ADR-001）✅

**合规性**: 完全合规

**检查项**:
- ✅ 所有业务 API 使用 POST 方法（`/operation/page`, `/system/page`, `/operation/export`, `/system/export`）
- ✅ 路径模式符合 `/api/v1/<模块>/<资源>/<动作>` 规范
- ✅ 使用 `@RequestBody` 接收查询参数
- ✅ 返回 `RESTResult<T>` 统一响应体
- ✅ 导出接口直接返回 CSV 文件流（合理例外）

**代码示例**:
```java
@PostMapping("/operation/page")
public RESTResult<PageResultVO<OperationLogVO>> operationPage(
    HttpServletRequest request,
    @RequestBody(required = false) OperationLogSearchVO vo) {
    // ...
}
```

**评价**: API 设计规范，符合项目统一标准。导出功能使用 POST + 直接文件流响应，避免了大数据量的 JSON 序列化开销。

---

### 2. 数据访问模式（ADR-003）✅

**合规性**: 完全合规

**检查项**:
- ✅ Repository 继承 `JpaRepository` + `JpaSpecificationExecutor`
- ✅ Service 层使用 Specification 构建动态查询
- ✅ 支持多条件组合（module/action/username/status/时间范围）
- ✅ 支持动态排序和分页
- ✅ 使用 `BasicQueryDto` 作为查询基类

**代码示例**:
```java
Specification<OperationLog> spec = (root, query, cb) -> {
    List<Predicate> list = new ArrayList<>();
    if (q.getModule() != null && !q.getModule().trim().isEmpty()) {
        list.add(cb.like(root.get("module"), "%" + q.getModule().trim() + "%"));
    }
    // ... 其他条件
    return cb.and(list.toArray(new Predicate[0]));
};
```

**评价**: 动态查询实现标准，代码清晰。时间戳解析逻辑（`parseTimestamp`）支持多种格式，用户体验良好。

---

### 3. 数据隔离（ADR-004）❌

**合规性**: 不适用但缺少说明

**检查项**:
- ❌ Entity 无 `ownerId` 字段
- ❌ Service 层无 `ownerId` 过滤逻辑
- ❌ 缺少文档说明日志是全局资源还是租户隔离

**问题分析**:

日志模块的数据隔离策略不明确：

1. **操作日志（OperationLog）**:
   - 包含 `userId` 字段，记录操作者
   - 但查询时不强制按 `userId` 过滤
   - 管理员可以查看所有用户的操作日志（合理）
   - 普通用户是否应该只能查看自己的日志？（未定义）

2. **系统日志（SystemLog）**:
   - 无 `userId` 字段，属于全局系统事件
   - 应该只有管理员可访问（Controller 注释建议，但未强制）

**当前实现**:
```java
// LogController.java - 仅检查登录状态，未区分管理员/普通用户
if (AuthTokenFilter.getUserId(request) == null) {
    return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
}
```

**建议**:
1. 明确日志访问权限策略（全局 vs 租户隔离）
2. 如果需要租户隔离，添加 `ownerId` 字段和过滤逻辑
3. 如果是全局资源，添加管理员权限检查
4. 在模块文档中说明设计决策

---

### 4. 逻辑删除（ADR-005）❌

**合规性**: 未实现

**检查项**:
- ❌ `OperationLog` Entity 无 `deleted` 字段
- ❌ `SystemLog` Entity 无 `deleted` 字段
- ❌ Entity 未使用 `@SQLRestriction("deleted = 0")`
- ❌ SQL schema 明确注释"无 deleted 字段，系统日志不支持逻辑删除"

**问题分析**:

日志表不支持逻辑删除，导致以下问题：

1. **无法归档历史日志**: 删除操作是物理删除，数据无法恢复
2. **合规性风险**: 某些行业要求审计日志保留特定时间（如金融行业 7 年）
3. **性能问题**: 日志表持续增长，无法通过逻辑删除 + 定期归档优化查询性能

**SQL schema 注释**:
```sql
-- 注意：无 deleted 字段，系统日志不支持逻辑删除
```

**建议**:
1. 添加 `deleted` 字段（INTEGER DEFAULT 0）
2. Entity 添加 `@SQLRestriction("deleted = 0")`
3. 实现日志归档功能（定时任务标记 deleted=1）
4. 提供归档日志查询接口（管理员专用）

**迁移 SQL**:
```sql
ALTER TABLE sys_operation_log ADD COLUMN deleted INTEGER DEFAULT 0;
ALTER TABLE sys_system_log ADD COLUMN deleted INTEGER DEFAULT 0;
CREATE INDEX idx_sys_operation_log_deleted ON sys_operation_log(deleted);
CREATE INDEX idx_sys_system_log_deleted ON sys_system_log(deleted);
```

---

### 5. 缓存策略 ⚠️

**合规性**: 未实现（合理）

**检查项**:
- ⚠️ Service 层未使用 `@Cacheable` 注解
- ⚠️ 未集成 Caffeine 或 Redis 缓存

**问题分析**:

日志模块未使用缓存，这是**合理的设计决策**：

1. **日志数据特性**:
   - 写多读少（每个请求写入，查询频率低）
   - 实时性要求高（需要看到最新日志）
   - 查询条件多样（难以命中缓存）

2. **缓存收益低**:
   - 日志查询通常是管理员临时排查问题
   - 不是高频热点数据
   - 缓存命中率预期很低

3. **缓存成本高**:
   - 日志数据量大，缓存占用内存多
   - 需要频繁失效缓存（每次写入）
   - 增加系统复杂度

**评价**: 不使用缓存是正确的架构决策，无需修改。

---

### 6. 错误码规范 ⚠️

**合规性**: 部分合规

**检查项**:
- ✅ 使用 `ErrorCode` 常量类
- ⚠️ 仅使用 `ErrorCode.UNAUTHORIZED`
- ❌ 缺少其他业务错误码（如参数校验失败、导出超限等）

**当前使用**:
```java
// LogController.java
return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
```

**缺失的错误码**:
1. 导出行数超限（当前硬编码 2000 行，但未返回明确错误码）
2. 时间范围过大（可能导致性能问题）
3. 参数校验失败（虽然 `BasicQueryDto.validateParams()` 会修正，但未返回错误）

**建议**:
1. 添加日志模块专用错误码：
   ```java
   // ErrorCode.java
   public static final int LOG_EXPORT_LIMIT_EXCEEDED = 50001;
   public static final int LOG_TIME_RANGE_TOO_LARGE = 50002;
   ```

2. 在 Controller 中使用：
   ```java
   if (vo.getRows() > EXPORT_MAX_ROWS) {
       return RESTResult.error(ErrorCode.LOG_EXPORT_LIMIT_EXCEEDED, 
           "导出行数超限，最多 " + EXPORT_MAX_ROWS + " 行");
   }
   ```

---

## 问题清单

### P0 - 阻塞级

无

### P1 - 高优先级

1. **缺少逻辑删除支持**
   - 文件: `OperationLog.java`, `SystemLog.java`
   - 问题: 无 `deleted` 字段，无法归档历史日志
   - 影响: 合规性风险、性能问题
   - 修复: 添加 `deleted` 字段 + `@SQLRestriction`

2. **数据隔离策略不明确**
   - 文件: `LogController.java`, `OperationLogServiceImpl.java`
   - 问题: 未定义日志访问权限（全局 vs 租户隔离）
   - 影响: 安全风险（普通用户可能看到其他用户日志）
   - 修复: 明确策略 + 添加权限检查

### P2 - 中优先级

3. **错误码不完整**
   - 文件: `LogController.java`
   - 问题: 仅使用 `UNAUTHORIZED`，缺少业务错误码
   - 影响: 前端无法精确处理错误场景
   - 修复: 添加 `LOG_EXPORT_LIMIT_EXCEEDED` 等错误码

4. **缺少导出限制提示**
   - 文件: `LogController.java`
   - 问题: 导出超限时静默截断，未返回错误
   - 影响: 用户体验差（不知道数据被截断）
   - 修复: 超限时返回明确错误信息

### P3 - 低优先级

5. **缺少模块文档**
   - 问题: 无 `docs/modules/log/00-大纲.md` 等设计文档
   - 影响: 新开发者难以理解模块设计意图
   - 修复: 补充模块设计文档

6. **日志采集异常被静默吞掉**
   - 文件: `OperationLogFilter.java` L88, `OperationLogServiceImpl.java` L88
   - 问题: `catch (Exception ignored)` 可能隐藏问题
   - 影响: 日志丢失时难以排查
   - 修复: 添加 WARN 级别日志（已在 ServiceImpl 中实现，Filter 中缺失）

---

## 修复建议

### 短期（1-2 周）

**优先级 P1 问题**:

1. **添加逻辑删除支持**（2 人日）
   - 编写迁移 SQL（添加 `deleted` 字段）
   - 修改 Entity（添加 `@SQLRestriction`）
   - 实现归档接口（管理员专用）
   - 编写单元测试

2. **明确数据隔离策略**（1 人日）
   - 评审日志访问权限需求
   - 实现权限检查逻辑（管理员 vs 普通用户）
   - 更新 API 文档
   - 编写集成测试

### 中期（1 个月）

**优先级 P2 问题**:

3. **完善错误码体系**（0.5 人日）
   - 添加日志模块错误码
   - 更新 Controller 错误处理
   - 同步前端错误码映射

4. **优化导出功能**（1 人日）
   - 添加导出限制提示
   - 支持分批导出（大数据量场景）
   - 添加导出进度反馈

### 长期（持续改进）

**优先级 P3 问题**:

5. **补充模块文档**（2 人日）
   - 编写 `00-大纲.md`（模块概述、核心功能）
   - 编写 `01-需求分析.md`（日志采集策略、访问权限）
   - 编写 `02-数据模型.md`（表结构、索引设计）
   - 编写 `08-测试与验收.md`（测试用例）

6. **增强可观测性**（1 人日）
   - OperationLogFilter 添加异常日志
   - 添加日志采集成功率监控指标
   - 添加日志表增长趋势监控

---

## 架构亮点

尽管存在合规性问题，log 模块仍有以下**优秀设计**：

### 1. 自动化日志采集 ✅

**OperationLogFilter** 设计优秀：
- 使用 `ContentCachingRequestWrapper` 采集请求体
- 使用 `ContentCachingResponseWrapper` 采集响应体
- 自动脱敏敏感字段（`BodyMaskUtil`）
- 异步写入，不阻塞主流程

```java
@Component
@Order(3)
public class OperationLogFilter implements Filter {
    // 自动采集所有 /api/v1 请求的日志
}
```

### 2. 灵活的时间戳解析 ✅

支持多种时间格式：
- Unix 时间戳（秒/毫秒）
- ISO 8601 格式（`2026-05-08T10:30:00`）
- 日期格式（`2026-05-08`，自动补全时分秒）

```java
private static Timestamp parseTimestamp(String s, boolean startOfDay) {
    if (s.matches("^\\d+$")) {
        long ms = Long.parseLong(s);
        if (s.length() <= 10) ms *= 1000;
        return new Timestamp(ms);
    }
    if (s.length() == 10) s += startOfDay ? " 00:00:00" : " 23:59:59";
    return Timestamp.valueOf(s.replace("T", " "));
}
```

### 3. CSV 导出功能 ✅

- 支持 UTF-8 BOM（Excel 兼容）
- 自动转义 CSV 特殊字符
- 限制导出行数（防止 OOM）
- 直接流式输出（避免内存占用）

```java
w.write("\uFEFF"); // BOM for Excel
w.println("ID,traceId,userId,username,...");
for (OperationLogVO o : list) {
    w.println(escapeCsv(o.getId()) + "," + ...);
}
```

### 4. 防御性编程 ✅

- 日志落库失败不影响主流程（`catch (Exception ignored)`）
- 字符串自动截断（防止超长字段）
- 参数自动校验和修正（`BasicQueryDto.validateParams()`）

---

## 总结

**总体评价**: log 模块的核心功能实现优秀，但在合规性和可维护性方面存在不足。

**优势**:
- ✅ API 设计规范，符合项目标准
- ✅ 自动化日志采集机制设计优秀
- ✅ 动态查询实现灵活
- ✅ CSV 导出功能完善

**不足**:
- ❌ 缺少逻辑删除支持（P1）
- ❌ 数据隔离策略不明确（P1）
- ⚠️ 错误码体系不完整（P2）
- ⚠️ 缺少模块设计文档（P3）

**总工作量**: 6.5 人日

| 优先级 | 工作量 | 问题数 |
|--------|--------|--------|
| P1 | 3 人日 | 2 |
| P2 | 1.5 人日 | 2 |
| P3 | 3 人日 | 2 |

**建议**: 优先修复 P1 问题（逻辑删除 + 数据隔离），确保日志模块的合规性和安全性。P2/P3 问题可在后续迭代中逐步改进。
