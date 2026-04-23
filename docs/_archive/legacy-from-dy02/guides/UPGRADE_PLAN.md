# 📈 全面升级计划

## 当前状态分析

### ✅ 已可用模块（4个）
- auth（认证授权）
- log（日志管理）
- config（系统配置）
- storage（文件存储）

### ⚠️ 需要修复的模块（10个）

**Phase 2（6个模块）**
1. douyin - 抖音账号管理
2. copy - 文案库管理
3. script - 话术违规词
4. shortvideo - 短视频管理
5. live - 直播管理
6. product - 产品销售

**Phase 3（4个模块）**
1. abtest - AB测试管理
2. agent - 智能体管理
3. ai - AI 服务管理
4. wecom - 企业微信集成

## 升级策略

### 第1步：建立代码生成质量标准
✅ 仅使用项目中实际存在的工具类和 API
✅ 导包时使用 jakarta.persistence（不使用 javax）
✅ SearchVO 必须继承 BasicQueryDto
✅ 分页结果仅使用 PageResultVO.of() 工厂方法
✅ 所有代码必须编译通过

### 第2步：逐个模块修复

#### 优先级1（本周完成）
- copy（文案管理）- 5 个 Entity
- script（话术违规词）- 3 个 Entity  
- product（产品销售）- 2 个 Entity

#### 优先级2（下周完成）
- douyin（抖音账号）- 2 个 Entity
- shortvideo（短视频）- 6 个 Entity
- live（直播）- 4 个 Entity

#### 优先级3（后续完成）
- abtest（AB测试）- 2 个 Entity
- agent（智能体）- 3 个 Entity
- ai（AI服务）- 4 个 Entity
- wecom（企业微信）- 4 个 Entity

### 第3步：验证清单

每个模块必须通过：
- [ ] mvn compile 无错误
- [ ] npm run build 无错误
- [ ] 所有 Service 使用 PageResultVO.of()
- [ ] 所有 SearchVO 继承 BasicQueryDto
- [ ] 数据库脚本已生成
- [ ] 前端 API、类型、路由已生成
- [ ] 前端页面组件已生成

### 第4步：集成测试

- [ ] 后端启动无异常
- [ ] 前端构建成功
- [ ] 数据库迁移成功
- [ ] API 调用正常

## 预期成果

✅ 13 个模块全部生产就绪
✅ 代码编译通过率 100%
✅ 完整的前后端实现
✅ 详细的集成文档

## 时间估计

- Phase 2 修复：3-4 小时
- Phase 3 修复：2-3 小时
- 集成验证：1-2 小时
- 总计：6-9 小时

