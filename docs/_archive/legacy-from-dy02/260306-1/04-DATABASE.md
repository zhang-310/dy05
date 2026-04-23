# 04 数据库设计与一致性分析

> 综合评分：65/100（C 级）
> 发现问题：24 项（P0: 3 / P1: 8 / P2: 13）

---

## P0 - 必须修复

### DB-01: 缺少复合索引（查询性能严重受影响）

**现状**: 大量单列索引，未覆盖常见查询模式

**问题示例** (`sql/live/schema.sql`):
```sql
CREATE INDEX idx_live_session_user_id ON live_session(user_id);
CREATE INDEX idx_live_session_status ON live_session(status);
-- 常见查询: WHERE user_id = ? AND status = ? AND deleted = 0  → 无法使用复合索引
```

**修复**: 替换为复合索引
```sql
CREATE INDEX idx_live_session_user_status ON live_session(user_id, status, deleted);
CREATE INDEX idx_live_session_account_status ON live_session(account_id, status, deleted);
```

**受影响模块**: live, product, script, shortvideo, ai（全部需要审查）

---

### DB-02: 数据隔离 Service 层检查不严格

**文件**: `ProductServiceImpl.java`
```java
if (vo.getUserId() != null && vo.getUserId() > 0) {
    predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
}
```
**问题**: userId 从 VO 传入，未验证是否为当前登录用户。攻击者可伪造 userId 查询他人数据。

**修复**: 强制使用 `getCurrentUserId()` 覆盖 VO 中的 userId

---

### DB-03: 缺少数据库迁移框架

**现状**:
- 170 个 SQL 文件散落在 `sql/` 各模块下
- 无版本号规范
- `run-all-migrations.sql` 手动执行
- 无法追踪已执行的迁移

**修复**: 引入 Flyway
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```
```
src/main/resources/db/migration/
  V001__auth_schema.sql
  V002__product_schema.sql
  V003__live_schema.sql
  ...
```

---

## P1 - 应尽快修复

### DB-04: deleted 列单独索引浪费空间

**文件**: `sql/ai/kb-schema.sql`
```sql
CREATE INDEX idx_kb_document_deleted ON ai_kb_document(deleted);  -- 低选择性，无用索引
```

**修复**: 使用部分索引
```sql
CREATE INDEX idx_kb_document_kb_id ON ai_kb_document(kb_id) WHERE deleted = 0;
```

### DB-05: 聚合字段无一致性保证

**文件**: `sql/ai/schema.sql`
```sql
total_documents INTEGER NOT NULL DEFAULT 0,  -- 可能与实际文档数不一致
total_tokens    BIGINT  NOT NULL DEFAULT 0,
```
**修复**: 事务内同步更新 + 定期对账任务

### DB-06: 乐观锁无重试机制

**文件**: `ProductServiceImpl.java`
```java
try {
    dyProductRepository.save(entity);
} catch (OptimisticLockException e) {
    throw new BusinessException(...);  // 直接失败，无重试
}
```
**修复**: 添加 3 次重试或改用悲观锁

### DB-07: INTEGER 字段溢出风险

```sql
viewers  INTEGER DEFAULT 0,   -- 最大值 21 亿，热门视频可能溢出
comments INTEGER DEFAULT 0,
```
**修复**: 统一改为 BIGINT

### DB-08: 冗余字段导致数据不一致

**文件**: `sql/live/schema.sql`
```sql
product_name VARCHAR(256),  -- 冗余自 dy_product 表
```
**修复**: 删除冗余字段，查询时 JOIN；或定期同步

### DB-09: 监控数据表无分区策略

`live_monitor` 是时间序列数据，会快速积累。
**修复**: 添加按月分区

### DB-10: 缺少幂等性约束

**文件**: `LiveMonitorServiceImpl.java`
```java
public long save(LiveMonitorVO vo) {
    LiveMonitor monitor = new LiveMonitor();
    monitor = liveMonitorRepository.save(monitor);  // 重复请求会产生重复记录
}
```
**修复**: 添加唯一约束 `(session_id, timestamp)`

### DB-11: @Column 注解不完整

Entity 字段缺少 `nullable`, `unique`, `precision` 等属性。
**修复**: 逐步补全 @Column 属性

---

## P2 - 优化项

| # | 问题 | 建议 |
|---|------|------|
| DB-12 | Timestamp 使用 java.sql.Timestamp | 迁移到 LocalDateTime |
| DB-13 | 38 处原生 SQL 查询 | 考虑用 Criteria/JPQL 替换 |
| DB-14 | 缺少 @EntityGraph 预防 N+1 | 关键查询添加 JOIN FETCH |
| DB-15 | 无数据归档策略 | 历史数据归档到冷表 |
| DB-16 | API Key 用 VARCHAR 存储 | 固定长度应用 CHAR |
| DB-17 | TEXT 字段无长度限制 | 设置合理上限（如 50000） |
| DB-18 | 唯一索引未覆盖 owner_id | 版本管理等需加 owner_id |
| DB-19 | 缺少数据库连接池监控 | HikariCP 指标导出 |
| DB-20 | 无读写分离设计 | 主从分离提升读性能 |
| DB-21 | 缺少分布式事务处理 | 跨系统操作需 Saga 模式 |
| DB-22 | demo 数据质量参差 | 统一 demo 数据标准 |
| DB-23 | 无数据库备份验证 | 定期恢复测试 |
| DB-24 | 缺少慢查询监控 | pg_stat_statements 启用 |
