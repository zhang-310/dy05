# 前后端迭代升级快速启动指南

**目标**: 帮助团队快速理解升级计划并开始执行

---

## 📋 文档索引

| 文档 | 用途 | 链接 |
|------|------|------|
| 迭代升级计划 | 总体路线图（6 个 Sprint） | [ITERATION-UPGRADE-PLAN.md](./ITERATION-UPGRADE-PLAN.md) |
| Sprint 1 任务清单 | 当前 Sprint 详细任务 | [SPRINT-1-TASKS.md](./SPRINT-1-TASKS.md) |
| 模块修复计划 | 各模块详细问题清单 | [modules/*/fix-plan.md](./modules/) |
| 前端优化计划 | 前端专项优化 | [modules/FRONTEND-PRODUCT-OPTIMIZATION.md](./modules/FRONTEND-PRODUCT-OPTIMIZATION.md) |

---

## 🎯 当前状态（2026-05-11）

### 已完成 ✅
- douyin 模块 P0-1（N+1 查询）
- douyin 模块 P1-1（OAuth state 防重放）
- douyin 模块 P1-3（API 限流保护）
- douyin 模块 P1-5（账号统计缓存）
- douyin 模块 P1-6（话术学习管道并行处理）
- 前端任务 #16（直播场次完整流程）

### 进行中 ⚠️
- Sprint 1：P0 阻塞级问题修复（2 周）

### 待开始 📅
- Sprint 2：P1 高优先级问题修复（3 周）
- Sprint 3：P2 中优先级问题修复（4 周）
- Sprint 4：测试覆盖率提升（4 周）
- Sprint 5：核心流程优化（3 周）
- Sprint 6：性能与安全优化（2 周）

---

## 🚀 快速启动（今天就开始）

### Step 1: 环境准备（30 分钟）

```bash
# 1. 拉取最新代码
git checkout main
git pull origin main

# 2. 创建 Sprint 1 分支
git checkout -b sprint-1-p0-fixes

# 3. 后端：确保依赖服务运行
cd C:\claude\dy05
docker compose -f docker/docker-compose.yml up -d postgres redis rabbitmq elasticsearch

# 4. 后端：编译检查
mvn clean compile

# 5. 前端：安装依赖
cd front
npm install

# 6. 前端：类型检查
npm run type-check
```

### Step 2: 任务分配（1 小时）

**后端团队**（2 人）:
- 开发者 A：live 模块 VO 修改 + 缓存实现（B1.1-B1.3）
- 开发者 B：live 模块索引 + N+1 查询修复 + 单元测试（B1.4-B1.8）

**前端团队**（2 人）:
- 开发者 C：类型定义 + API 层类型修复（F1.1-F1.5）
- 开发者 D：页面组件类型修复 + 错误处理（F1.6-F1.9）

### Step 3: 开始第一个任务（立即）

#### 后端开发者 A - 第一个任务

```bash
# 任务：B1.1 LiveScriptVO 字段补全

# 1. 找到文件
code douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/vo/LiveScriptVO.java

# 2. 参考 Entity 添加缺失字段
# 参考文件：douyin-operations-live/src/main/java/cn/gaifan/douyinOperations/module/live/entity/LiveScript.java

# 3. 添加 22 个字段（见下方清单）

# 4. 编译验证
mvn compile -pl douyin-operations-live -am

# 5. 提交代码
git add .
git commit -m "fix(live): 补全 LiveScriptVO 22 个缺失字段

- 添加 scriptType, style, aiGenerated 等字段
- 与 LiveScript Entity 保持一致
- 关联 Issue: B1.1"
```

**LiveScriptVO 需要添加的字段**:
```java
private String scriptType;        // 话术类型
private String style;              // 话术风格
private Boolean aiGenerated;       // 是否 AI 生成
private Long productId;            // 关联商品
private Long aiCallLogId;          // AI 调用日志
private String generationStatus;   // 生成状态
private Boolean violationChecked;  // 是否违规检测
private String violationResult;    // 违规检测结果
private Integer viewerDelta;       // 观看人数变化
private Integer interactionDelta;  // 互动量变化
private Integer conversionDelta;   // 转化量变化
private Double effectivenessScore; // 效果评分
private Integer durationLimitSec;  // 时长限制
private String requirement;        // 生成要求
private Long referencedScriptId;   // 参考话术ID
private String referencedScriptSnapshot; // 参考话术快照
private String approvalStatus;     // 审批状态
private Long userId;               // 用户ID
private String generationPromptHash; // 生成提示哈希
private Long abExperimentId;       // AB实验ID
private Long abVariantId;          // AB变体ID
private String aiSuggestion;       // AI建议
private Long promptTemplateId;     // 提示模板ID
```

#### 前端开发者 C - 第一个任务

```bash
# 任务：F1.1 LiveScript 类型定义补全

# 1. 找到文件
code front/src/types/live.ts

# 2. 找到 LiveScript 接口

# 3. 添加 22 个字段（与后端 VO 一致）

# 4. 类型检查
npm run type-check

# 5. 提交代码
git add .
git commit -m "fix(front): 补全 LiveScript 类型定义

- 添加 scriptType, style, effectivenessScore 等字段
- 与后端 LiveScriptVO 保持一致
- 关联 Issue: F1.1"
```

**LiveScript 类型定义**:
```typescript
export interface LiveScript {
  // 现有字段
  id: number
  sessionId: number
  scriptContent: string
  sequenceNo: number
  executionTime?: number
  executed?: boolean
  actualExecutionTime?: number
  createTime?: string
  updateTime?: string
  
  // 新增字段
  scriptType?: string
  style?: string
  aiGenerated?: boolean
  productId?: number
  aiCallLogId?: number
  generationStatus?: string
  violationChecked?: boolean
  violationResult?: string
  viewerDelta?: number
  interactionDelta?: number
  conversionDelta?: number
  effectivenessScore?: number
  durationLimitSec?: number
  requirement?: string
  referencedScriptId?: number
  referencedScriptSnapshot?: string
  approvalStatus?: string
  userId?: number
  generationPromptHash?: string
  abExperimentId?: number
  abVariantId?: number
  aiSuggestion?: string
  promptTemplateId?: number
}
```

---

## 📊 进度追踪

### 每日更新进度

在 `SPRINT-1-TASKS.md` 中更新任务状态：
- ⚠️ TODO → 🔄 IN PROGRESS → ✅ DONE

### 每日站会（15 分钟）

**时间**: 每天上午 10:00  
**议题**:
1. 昨天完成了什么？
2. 今天计划做什么？
3. 有什么阻塞问题？

### 代码审查流程

```bash
# 1. 创建功能分支
git checkout -b feature/B1.1-livescript-vo-fields

# 2. 完成开发并提交
git add .
git commit -m "fix(live): 补全 LiveScriptVO 字段"

# 3. 推送到远程
git push origin feature/B1.1-livescript-vo-fields

# 4. 创建 Pull Request
# 标题：[Sprint 1] B1.1: 补全 LiveScriptVO 字段
# 描述：添加 22 个缺失字段，与 Entity 保持一致

# 5. 等待审查通过后合并到 sprint-1-p0-fixes
```

---

## 🔍 质量检查清单

### 后端代码提交前

- [ ] `mvn compile` 通过
- [ ] 相关单元测试通过
- [ ] 代码格式化（IDEA: Ctrl+Alt+L）
- [ ] 无 TODO/FIXME 注释
- [ ] 提交信息符合规范

### 前端代码提交前

- [ ] `npm run type-check` 通过
- [ ] `npm run build` 成功
- [ ] 相关组件测试通过
- [ ] 代码格式化（Prettier）
- [ ] 无 console.log
- [ ] 提交信息符合规范

---

## 🆘 常见问题

### Q1: 后端编译失败怎么办？

```bash
# 1. 清理并重新编译
mvn clean compile

# 2. 检查依赖
mvn dependency:tree

# 3. 更新依赖
mvn clean install -U
```

### Q2: 前端类型检查失败怎么办？

```bash
# 1. 清理并重新安装
rm -rf node_modules package-lock.json
npm install

# 2. 检查 TypeScript 版本
npm list typescript

# 3. 查看详细错误
npm run type-check -- --pretty
```

### Q3: 测试失败怎么办？

```bash
# 后端：运行单个测试
mvn test -Dtest=LiveScriptServiceImplTest

# 前端：运行单个测试
npm test -- SessionsPage.test.tsx
```

### Q4: 如何查看模块详细问题？

```bash
# 查看 live 模块修复计划
cat docs/modules/live/fix-plan.md

# 查看 douyin 模块修复计划
cat docs/modules/douyin/fix-plan.md
```

---

## 📞 联系方式

| 角色 | 姓名 | 联系方式 |
|------|------|----------|
| 项目经理 | TBD | - |
| 后端负责人 | TBD | - |
| 前端负责人 | TBD | - |
| 测试负责人 | TBD | - |

---

## 📚 参考资源

### 项目文档
- [CLAUDE.md](../CLAUDE.md) - 项目总览
- [BUILD.md](./BUILD.md) - 构建指南
- [SSOT.md](./SSOT.md) - 单一事实来源

### 技术规范
- [coding-style.md](../.claude/rules/common/coding-style.md) - 编码规范
- [testing.md](../.claude/rules/common/testing.md) - 测试规范
- [security.md](../.claude/rules/common/security.md) - 安全规范

### 外部资源
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [React 文档](https://react.dev/)
- [TypeScript 文档](https://www.typescriptlang.org/)

---

**创建时间**: 2026-05-11  
**最后更新**: 2026-05-11  
**维护者**: 项目团队
