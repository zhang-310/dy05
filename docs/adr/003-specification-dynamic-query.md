# ADR-003: 使用 JPA Specification 动态查询

## 状态
已采纳

## 背景
动态查询的实现方式:
1. 字符串拼接 SQL -- 有 SQL 注入风险
2. QueryDSL -- 需要额外的代码生成步骤
3. JPA Criteria API -- 原生支持，但代码冗长
4. JPA Specification -- 基于 Criteria API 的封装，可组合

## 决策
使用 Spring Data JPA 的 Specification 接口构建动态查询。

## 后果
- 正面: 类型安全、防 SQL 注入、可组合、无需额外依赖
- 负面: 代码较冗长（已通过 BaseSpecificationBuilder 缓解）
