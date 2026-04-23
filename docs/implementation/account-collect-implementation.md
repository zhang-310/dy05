# 账号采集优化方案 - 实施总结

**日期**: 2026-04-21  
**状态**: ✅ 后端实现完成，编译通过

---

## 一、实施内容

### 1. 数据库迁移 ✅

**文件**: `sql/shortvideo/migration-account-optimization.sql`

- 创建 `sv_account` 账号主表（50+ 字段）
- 添加 `sv_account_collect_task.sv_account_id` 字段
- 添加 `sv_viral_video.sv_account_id` 字段
- 数据迁移脚本（从现有任务提取账号）
- 索引优化（sec_uid 唯一索引、分类索引、评分索引）

### 2. 后端实现 ✅

#### Entity 层

1. **SvAccount.java** - 账号主表实体（150 行）
   - 基本信息：sec_uid、昵称、头像、简介
   - 账号数据：粉丝数、获赞数、作品数
   - 采集统计：采集次数、累计视频数
   - 分析统计：平均播放量、平均爆款评分
   - 标签分类：行业标签、内容标签、账号分类
   - 来源信息：来源类型、来源关键词

2. **SvAccountCollectTask.java** - 添加 `svAccountId` 字段
3. **SvViralVideo.java** - 添加 `svAccountId` 字段

#### Repository 层

**SvAccountRepository.java** - JPA Repository
- `findByOwnerIdAndSecUidAndDeleted()` - 查找账号
- `findByIdAndDeleted()` - 根据 ID 查找

#### Service 层

**SvAccountService.java** + **SvAccountServiceImpl.java** (340+ 行)

核心方法：
- `searchAccounts()` - 账号列表搜索（支持关键词、分类、来源筛选）
- `getAccountDetail()` - 账号详情（含统计数据）
- `updateAccount()` - 更新账号信息
- `deleteAccount()` - 删除账号
- `findOrCreateAccount()` - 查找或创建账号（核心方法）
- `updateAccountStatistics()` - 更新账号统计数据
- `getAccountVideos()` - 获取账号视频列表

#### Controller 层

**SvAccountController.java** - REST API
- `POST /api/v1/short-video/account/list` - 账号列表
- `POST /api/v1/short-video/account/get` - 账号详情
- `POST /api/v1/short-video/account/update` - 更新账号
- `POST /api/v1/short-video/account/delete` - 删除账号
- `POST /api/v1/short-video/account/videos` - 账号视频列表
- `POST /api/v1/short-video/account/refresh-stats` - 刷新统计

#### VO 层

1. **SvAccountSearchVO.java** - 搜索参数
2. **SvAccountVO.java** - 列表返回
3. **SvAccountDetailVO.java** - 详情返回
4. **SvAccountUpdateVO.java** - 更新参数
5. **AccountVideosQueryVO.java** - 视频查询参数
6. **ViralVideoVO.java** - 视频返回
7. **IdVO.java** - ID 参数（common 模块）

#### 集成改造

**AccountVideoCollectServiceImpl.java** - 采集服务集成

1. **创建任务时自动创建账号**（第 155-170 行）
   ```java
   // 创建或查找账号记录
   SvAccount svAccount = svAccountService.findOrCreateAccount(
       resolved.secUid(),
       resolved.accountName(),
       sourceType,
       sourceKeyword,
       null,
       userId
   );
   task.setSvAccountId(svAccount.getId());
   ```

2. **创建视频时关联账号**（第 663-720 行）
   ```java
   viral.setSvAccountId(svAccountId);  // 关联账号
   ```

3. **任务完成后更新账号统计**（第 492-505 行）
   ```java
   // 更新账号统计数据
   svAccountService.updateAccountStatistics(task.getSvAccountId());
   ```

---

## 二、核心功能

### 1. 账号自动管理

**流程**：
```
用户输入账号链接/关键词
    ↓
解析 sec_uid
    ↓
查找或创建 sv_account 记录
    ↓
创建采集任务（关联 sv_account_id）
    ↓
采集视频（关联 sv_account_id）
    ↓
更新账号统计数据
```

**特点**：
- ✅ 自动去重（同一 sec_uid 只创建一次）
- ✅ 采集次数自动累加
- ✅ 关键词采集自动记录来源关键词
- ✅ 支持手动添加和关键词采集两种来源

### 2. 账号统计计算

**统计维度**：
- 累计采集视频数
- 平均播放量、点赞数、分享数、评论数
- 平均爆款评分、最高爆款评分
- 采集次数、最后采集时间

**计算时机**：
- 采集任务完成后自动计算
- 手动刷新统计（API 接口）

### 3. 账号数据查询

**搜索条件**：
- 关键词（昵称、抖音号、sec_uid）
- 账号分类
- 来源类型（manual/keyword_search）
- 来源关键词
- 最小粉丝数
- 最小爆款评分

**排序支持**：
- 粉丝数
- 爆款评分
- 采集次数
- 更新时间

---

## 三、数据流

### 完整流程

```
用户触发采集
    ↓
AccountVideoCollectServiceImpl.startCollect()
    ├─ 解析输入（sec_uid、昵称）
    ├─ SvAccountService.findOrCreateAccount()
    │   ├─ 查找现有账号（owner_id + sec_uid）
    │   └─ 不存在则创建新账号
    ├─ 创建采集任务（关联 sv_account_id）
    └─ 异步执行采集
        ↓
AccountCollectAsyncRunner.runCollectAsync()
    ├─ 采集视频列表
    ├─ 创建 SvViralVideo（关联 sv_account_id）
    └─ 状态更新为 collected
        ↓
用户选择视频触发分析
    ↓
AccountCollectAsyncRunner.runAnalyzeAsync()
    ├─ 深度拆解视频
    ├─ 入队知识库
    ├─ 状态更新为 completed
    └─ SvAccountService.updateAccountStatistics()
        ├─ 查询该账号所有视频
        ├─ 计算统计数据
        └─ 更新 sv_account 表
```

### 数据表关系

```
sv_account (账号主表)
    ↓ 1:N
sv_account_collect_task (采集任务)
    ↓ 1:N
sv_viral_video (爆款视频)
```

---

## 四、API 接口

### 账号管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/short-video/account/list` | POST | 账号列表（支持搜索筛选） |
| `/api/v1/short-video/account/get` | POST | 账号详情（含统计数据） |
| `/api/v1/short-video/account/update` | POST | 更新账号（标签、分类、备注） |
| `/api/v1/short-video/account/delete` | POST | 删除账号 |
| `/api/v1/short-video/account/videos` | POST | 账号视频列表 |
| `/api/v1/short-video/account/refresh-stats` | POST | 刷新账号统计 |

### 请求示例

**账号列表**：
```json
POST /api/v1/short-video/account/list
{
  "keyword": "美妆",
  "sourceType": "keyword_search",
  "minFollowerCount": 10000,
  "minViralScore": 70,
  "page": 0,
  "rows": 20,
  "sortName": "avgViralScore",
  "sortOrder": "desc"
}
```

**账号详情**：
```json
POST /api/v1/short-video/account/get
{
  "id": 123
}
```

**更新账号**：
```json
POST /api/v1/short-video/account/update
{
  "id": 123,
  "accountCategory": "美妆",
  "industryTags": "[\"护肤\",\"彩妆\"]",
  "contentTags": "[\"教程\",\"测评\"]",
  "notes": "优质账号，值得学习"
}
```

---

## 五、下一步：前端实现

### 需要实现的页面

1. **账号列表页** (`/admin/shortvideo/accounts`)
   - 账号列表展示
   - 搜索筛选
   - 排序
   - 批量操作

2. **账号详情页** (`/admin/shortvideo/accounts/:id`)
   - Tab 1: 基本信息
   - Tab 2: 采集视频
   - Tab 3: 数据分析（图表）
   - Tab 4: 采集历史

3. **集成到现有流程**
   - 采集任务列表显示账号信息
   - 爆款视频列表显示账号信息

---

## 六、测试验证

### 数据库迁移

```sql
-- 1. 执行迁移脚本
\i sql/shortvideo/migration-account-optimization.sql

-- 2. 验证表结构
\d sv_account
\d sv_account_collect_task
\d sv_viral_video

-- 3. 查看数据迁移结果
SELECT COUNT(*) FROM sv_account WHERE deleted = 0;
SELECT COUNT(*) FROM sv_account_collect_task WHERE sv_account_id IS NOT NULL;
SELECT COUNT(*) FROM sv_viral_video WHERE sv_account_id IS NOT NULL;
```

### API 测试

```bash
# 1. 创建采集任务（会自动创建账号）
curl -X POST http://localhost:8080/api/v1/short-video/account-collect/start \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"input": "https://www.douyin.com/user/xxx"}'

# 2. 查看账号列表
curl -X POST http://localhost:8080/api/v1/short-video/account/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"page": 0, "rows": 20}'

# 3. 查看账号详情
curl -X POST http://localhost:8080/api/v1/short-video/account/get \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id": 1}'
```

---

## 七、文件清单

### 数据库

- `sql/shortvideo/migration-account-optimization.sql` - 迁移脚本

### 后端（11 个文件）

**Entity**:
- `SvAccount.java` - 账号主表实体
- `SvAccountCollectTask.java` - 修改（添加 svAccountId）
- `SvViralVideo.java` - 修改（添加 svAccountId）

**Repository**:
- `SvAccountRepository.java` - 账号 Repository

**Service**:
- `SvAccountService.java` - 账号服务接口
- `SvAccountServiceImpl.java` - 账号服务实现
- `AccountVideoCollectServiceImpl.java` - 修改（集成账号管理）

**Controller**:
- `SvAccountController.java` - 账号管理 Controller

**VO**:
- `SvAccountSearchVO.java` - 搜索参数
- `SvAccountVO.java` - 列表返回
- `SvAccountDetailVO.java` - 详情返回
- `SvAccountUpdateVO.java` - 更新参数
- `AccountVideosQueryVO.java` - 视频查询参数
- `ViralVideoVO.java` - 视频返回
- `IdVO.java` - ID 参数（common 模块）

### 文档

- `docs/design/account-collect-optimization.md` - 设计方案
- `docs/implementation/account-collect-implementation.md` - 实施总结（本文档）

---

## 八、编译状态

✅ **BUILD SUCCESS**

```
[INFO] douyin-operations-shortvideo ....................... SUCCESS [ 13.654 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 九、后续工作

### 立即可做

1. **执行数据库迁移**
   ```bash
   psql -U postgres -d douyin_operations -f sql/shortvideo/migration-account-optimization.sql
   ```

2. **重启应用测试**
   ```bash
   start.bat
   ```

3. **测试 API 接口**
   - 创建采集任务（验证账号自动创建）
   - 查看账号列表
   - 查看账号详情
   - 更新账号信息

### 前端实现（预计 2-3 天）

1. 账号列表页面
2. 账号详情页面
3. 账号分析图表
4. 与现有采集流程集成

---

## 十、预期效果

### 账号管理

- ✅ 统一管理所有采集的账号
- ✅ 自动去重（同一 sec_uid 只创建一次）
- ✅ 账号信息完整（粉丝数、简介、认证等）
- ✅ 支持标签分类和备注

### 数据分析

- ✅ 账号级别的数据统计
- ✅ 内容风格分析（待前端实现）
- ✅ 爆款规律发现（待前端实现）
- ✅ 账号质量评估

### 关键词采集优化

- ✅ 关键词采集的账号自动归类
- ✅ 可以查看某个关键词采集了哪些账号
- ✅ 可以对比不同账号的数据表现

---

需要我继续实现前端部分吗？
