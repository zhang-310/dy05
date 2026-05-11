# A/B Test 模块文档

## 文档索引

| 文档 | 说明 | 状态 |
|------|------|------|
| [architecture-review.md](./architecture-review.md) | 架构审查报告（773 行） | ✅ 已完成 |

## 模块概览

**位置**: `douyin-operations-intelligence/module/abtest`  
**功能**: A/B 实验管理、变体配置、事件记录、统计分析  
**评分**: 92/100 (Grade A)

## 核心特性

1. **实验管理**: 支持 video/live/copy 三种实验类型
2. **统计分析**: 卡方检验、转化率对比、日趋势分析
3. **自动收敛**: 定时任务自动结束达到统计显著的实验
4. **话术风格 A/B**: 为直播话术生成提供风格分配和转化追踪

## 关键问题

### P0 - 阻塞级（必须修复）
- ⚠️ 缺少 `owner_id` 数据隔离校验（安全风险）

### P1 - 高优先级
- ⚠️ SQL schema 缺少 `target_entity_type`/`target_entity_id` 字段
- ⚠️ 事件表无分页查询（性能风险）
- ⚠️ 缺少批量操作接口
- ⚠️ 缺少模块设计文档

## 快速链接

- **SQL Schema**: `sql/abtest/schema.sql`
- **Controller**: `AbTestController.java`
- **Service**: `AbTestServiceImpl.java`
- **前端页面**: `front/src/pages/abtest/`

## 下一步

1. 修复 P0 数据隔离问题（0.5 人日）
2. 补充 SQL schema 缺失字段（0.2 人日）
3. 添加事件表分页查询（0.3 人日）
4. 编写模块设计文档（1.0 人日）

---

**最后更新**: 2026-05-09  
**审查人**: Claude Code (Opus 4.6)
