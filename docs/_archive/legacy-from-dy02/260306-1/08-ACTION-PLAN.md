# 08 按优先级的修复行动计划

> 本文档基于 7 份深度分析报告，提炼出可执行的行动计划

---

## Phase 1: 紧急修复（1-3 天）

### 安全类（不修则不可上线）

| # | 行动项 | 预估工时 | 负责范围 |
|---|--------|---------|---------|
| 1 | 轮换所有泄露的 API 密钥 + `.env` 加入 .gitignore + 清理 git 历史 | 2h | DevOps |
| 2 | 修复 5 个 Controller 的 userId 硬编码（改用 AuthTokenFilter） | 2h | 后端 |
| 3 | 所有 Service getById 方法添加 owner 校验 | 4h | 后端 |
| 4 | 删除 application.yml 中 Token Secret 默认值 | 0.5h | 后端 |
| 5 | 删除 docker-compose 中 guest/guest 默认密码 | 0.5h | DevOps |

### 构建类（CI/CD 正确性）

| # | 行动项 | 预估工时 |
|---|--------|---------|
| 6 | CI/CD JDK 版本 17 → 21 | 0.5h |
| 7 | SpotBugs 移除 continue-on-error | 0.5h |

---

## Phase 2: 高优先级修复（1-2 周）

### 后端架构

| # | 行动项 | 预估工时 | 收益 |
|---|--------|---------|------|
| 8 | 创建 BaseSpecificationBuilder 消除 54 处重复 | 8h | 维护性 |
| 9 | 引入 MapStruct 替代手工 VO 转换 | 16h | 维护性 |
| 10 | 创建 @RequireAuth 注解统一权限提取 | 4h | 安全+维护 |
| 11 | 大数据量查询改为分页批处理 | 4h | 性能 |
| 12 | 乐观锁添加重试机制 | 4h | 可靠性 |

### 前端质量

| # | 行动项 | 预估工时 | 收益 |
|---|--------|---------|------|
| 13 | 拆分 LiveSessionDetailPage（1592行→5子组件） | 8h | 维护性 |
| 14 | 拆分 LiveScriptBuilderPage（1043行） | 6h | 维护性 |
| 15 | 消除 any 类型 + 定义具体 VO 接口 | 8h | 类型安全 |
| 16 | 补全 useEffect cleanup 防内存泄漏 | 4h | 稳定性 |
| 17 | 统一错误处理（console.error → Toast） | 4h | 用户体验 |

### 数据库

| # | 行动项 | 预估工时 | 收益 |
|---|--------|---------|------|
| 18 | 添加复合索引覆盖常见查询 | 4h | 性能 |
| 19 | 引入 Flyway 数据库迁移框架 | 8h | 可维护 |
| 20 | 添加幂等性唯一约束 | 4h | 数据一致 |

### DevOps

| # | 行动项 | 预估工时 | 收益 |
|---|--------|---------|------|
| 21 | Nginx 添加速率限制 | 2h | 安全 |
| 22 | 启用 SSL/TLS + Let's Encrypt | 4h | 安全 |
| 23 | Docker 添加非 root 用户 | 2h | 安全 |
| 24 | 补充关键告警规则（DB/Cache/MQ） | 4h | 可观测 |

### 文档

| # | 行动项 | 预估工时 | 收益 |
|---|--------|---------|------|
| 25 | 重写 README.md | 2h | 项目形象 |
| 26 | 同步错误码（Java → TypeScript） | 2h | 一致性 |
| 27 | 更新 CLAUDE.md 模块前缀表 | 1h | 开发规范 |

---

## Phase 3: 质量提升（2-4 周）

### 测试覆盖

| # | 行动项 | 预估工时 |
|---|--------|---------|
| 28 | live 模块单元测试（Controller + Service） | 16h |
| 29 | product 模块并发测试 | 8h |
| 30 | auth 模块安全测试 | 8h |
| 31 | 数据库集成测试（多租户隔离验证） | 8h |
| 32 | 前端关键页面测试（5个核心页面） | 16h |
| 33 | API 契约测试（前后端一致性） | 8h |

### 性能优化

| # | 行动项 | 预估工时 |
|---|--------|---------|
| 34 | 缓存策略完善（@CacheEvict + TTL 调优） | 8h |
| 35 | 异步处理（话术生成/评分计算改用 MQ） | 16h |
| 36 | 前端 useMemo/useCallback 优化 | 8h |
| 37 | 列表虚拟滚动（大数据场景） | 8h |
| 38 | 数据库慢查询监控 + pg_stat_statements | 4h |

### 前端增强

| # | 行动项 | 预估工时 |
|---|--------|---------|
| 39 | ErrorBoundary 增强（错误上报 + 降级方案） | 4h |
| 40 | 添加 ESLint + Prettier 代码风格强制 | 4h |
| 41 | Zustand Store 完善（UI/Cache/Dict） | 8h |
| 42 | a11y 基础改造（ARIA + 键盘导航） | 8h |

---

## Phase 4: 长期改进（1-3 月）

| # | 行动项 | 预估工时 |
|---|--------|---------|
| 43 | 国际化框架搭建（后端 MessageSource + 前端 i18n） | 40h |
| 44 | API 版本控制策略（v2 迁移方案） | 16h |
| 45 | 读写分离 + 数据库分片 | 40h |
| 46 | Kubernetes 迁移（替代 Docker Compose） | 40h |
| 47 | 全链路追踪（Jaeger/Zipkin） | 16h |
| 48 | E2E 自动化测试（Playwright 完整流程） | 40h |
| 49 | 性能基准文档 + 持续性能测试 | 16h |
| 50 | ADR（Architecture Decision Records）补充 | 8h |

---

## 工时总结

| Phase | 行动项数 | 预估总工时 | 时间范围 |
|-------|---------|----------|---------|
| Phase 1 | 7 | 10h | 1-3 天 |
| Phase 2 | 20 | 107h | 1-2 周 |
| Phase 3 | 15 | 136h | 2-4 周 |
| Phase 4 | 8 | 216h | 1-3 月 |
| **总计** | **50** | **469h** | — |

---

## 快速胜利清单（每项 < 2 小时）

立即可执行、效果显著的改进：

1. `.env` 加入 .gitignore（5 分钟）
2. CI JDK 17 → 21（10 分钟）
3. 删除 Token Secret 默认值（10 分钟）
4. 删除 docker-compose guest 密码（10 分钟）
5. Nginx 添加 client_max_body_size（10 分钟）
6. Prometheus 保留时间 15d → 90d（10 分钟）
7. README.md 技术栈描述修正（30 分钟）
8. 错误码同步脚本编写（1 小时）
9. SpotBugs 移除 continue-on-error（10 分钟）
10. Docker 添加 dumb-init（30 分钟）

---

## 验收标准

### Phase 1 完成标准
- [ ] 所有 API 密钥已轮换
- [ ] 5 个 Controller 使用 AuthTokenFilter
- [ ] CI/CD 使用 JDK 21 构建成功
- [ ] docker-compose 无明文默认密码

### Phase 2 完成标准
- [ ] 后端编译 0 errors + 0 SpotBugs critical
- [ ] 前端 0 个 `any` 类型使用
- [ ] 最大组件行数 < 500 行
- [ ] 数据库全部使用 Flyway 管理
- [ ] Nginx 启用 SSL + 速率限制

### Phase 3 完成标准
- [ ] 测试覆盖率 > 50%（后端）/ > 30%（前端）
- [ ] 核心 API P95 延迟 < 500ms
- [ ] 告警规则覆盖所有关键组件
- [ ] ErrorBoundary + Toast 统一错误处理

### Phase 4 完成标准
- [ ] 国际化支持中英双语
- [ ] E2E 测试覆盖核心流程
- [ ] 数据库读写分离上线
- [ ] 全链路追踪可观测
