# 账号采集优化 - 测试报告

**日期**: 2026-04-21  
**测试人**: Claude  
**状态**: 🟡 部分完成（数据库迁移成功，等待后端启动）

---

## 一、测试环境

### 1. 数据库 ✅

**PostgreSQL 容器**: `dy-postgres`
- 状态: ✅ 运行中
- 端口: 5433
- 数据库: `douyin_operations`

**迁移结果**:
```
✅ CREATE TABLE sv_account
✅ CREATE INDEX (6 个索引)
✅ ALTER TABLE sv_account_collect_task (添加 sv_account_id)
✅ ALTER TABLE sv_viral_video (添加 sv_account_id)
✅ 数据迁移完成
```

**迁移统计**:
- 总账号数: 3
- 手动添加: 3
- 关键词采集: 0
- 有粉丝数据: 0

- 总采集任务: 3
- 已关联账号: 3
- 未关联账号: 0

- 总视频数: 1243
- 已关联账号: 39
- 未关联账号: 1204

**已迁移账号**:
```
ID | 昵称           | sec_uid                    | 粉丝数 | 采集视频 | 平均评分
---+----------------+----------------------------+--------+----------+---------
1  | 百家语录       | MS4wLjABAAAAZwKBt...       | 0      | 0        | 0.00
2  | 有个同事叫老张 | MS4wLjABAAAAclyMt...       | 0      | 0        | 0.00
3  | 未登录         | self                       | 0      | 0        | 0.00
```

### 2. 后端 ⏸️

**状态**: ⏸️ 未运行
- 端口: 8080
- 需要启动: `start.bat` 或 `mvn spring-boot:run`

### 3. 前端 ✅

**状态**: ✅ 运行中
- 端口: 3000
- 开发服务器: Vite

---

## 二、测试计划

### 阶段 1: 后端 API 测试

**前置条件**: 启动后端服务

#### 1.1 账号列表 API

```bash
curl -X POST http://localhost:8080/api/v1/short-video/account/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "page": 0,
    "rows": 20,
    "sortName": "updateTime",
    "sortOrder": "desc"
  }'
```

**预期结果**:
```json
{
  "status": 200,
  "message": "success",
  "data": {
    "total": 3,
    "list": [
      {
        "id": 1,
        "nickname": "百家语录",
        "secUid": "MS4wLjABAAAAZwKBt...",
        "followerCount": 0,
        "totalCollectedVideos": 0,
        "avgViralScore": 0.00,
        "sourceType": "manual"
      }
    ],
    "pageNum": 0,
    "pageSize": 20
  }
}
```

#### 1.2 账号详情 API

```bash
curl -X POST http://localhost:8080/api/v1/short-video/account/get \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id": 1}'
```

**预期结果**:
```json
{
  "status": 200,
  "data": {
    "id": 1,
    "nickname": "百家语录",
    "followerCount": 0,
    "totalCollectedVideos": 0,
    "avgViralScore": 0.00,
    "taskCount": 1,
    "pendingAnalysisCount": 0,
    "analyzedCount": 0
  }
}
```

#### 1.3 更新账号 API

```bash
curl -X POST http://localhost:8080/api/v1/short-video/account/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "id": 1,
    "accountCategory": "语录",
    "industryTags": "[\"励志\",\"情感\"]",
    "contentTags": "[\"短句\",\"文案\"]",
    "notes": "测试账号"
  }'
```

**预期结果**:
```json
{
  "status": 200,
  "message": "success"
}
```

#### 1.4 账号视频列表 API

```bash
curl -X POST http://localhost:8080/api/v1/short-video/account/videos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "accountId": 1,
    "page": 0,
    "rows": 20
  }'
```

#### 1.5 刷新统计 API

```bash
curl -X POST http://localhost:8080/api/v1/short-video/account/refresh-stats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id": 1}'
```

### 阶段 2: 前端页面测试

**前置条件**: 后端服务运行中

#### 2.1 账号列表页

**URL**: http://localhost:3000/admin/shortvideo/accounts

**测试项**:
- [ ] 页面正常加载
- [ ] 账号列表展示（3 个账号）
- [ ] 搜索功能（输入"百家"）
- [ ] 来源类型筛选（选择"手动添加"）
- [ ] 状态筛选（选择"活跃"）
- [ ] 排序功能（点击"粉丝数"列头）
- [ ] 分页功能（切换页码）
- [ ] 查看详情按钮（点击眼睛图标）
- [ ] 删除按钮（点击删除图标，取消操作）

**预期效果**:
```
┌─────────────────────────────────────────────────────────────┐
│ 账号管理                                    [刷新]           │
├─────────────────────────────────────────────────────────────┤
│ [搜索框] [来源类型▼] [状态▼]                                │
├─────────────────────────────────────────────────────────────┤
│ ID │ 账号          │ 粉丝数 │ 采集视频 │ 平均评分 │ 操作   │
├────┼───────────────┼────────┼──────────┼──────────┼────────┤
│ 1  │ 百家语录      │ 0      │ 0        │ 0.0      │ 👁️ 🗑️  │
├────┼───────────────┼────────┼──────────┼──────────┼────────┤
│ 2  │ 有个同事叫... │ 0      │ 0        │ 0.0      │ 👁️ 🗑️  │
├────┼───────────────┼────────┼──────────┼──────────┼────────┤
│ 3  │ 未登录        │ 0      │ 0        │ 0.0      │ 👁️ 🗑️  │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2 账号详情页

**URL**: http://localhost:3000/admin/shortvideo/accounts/1

**测试项**:
- [ ] 页面正常加载
- [ ] 基本信息展示（昵称、粉丝数、采集视频数）
- [ ] Tab 切换（详细信息、采集视频、数据分析）
- [ ] 编辑功能
  - [ ] 点击"编辑"按钮
  - [ ] 修改账号分类为"语录"
  - [ ] 修改行业标签为 `["励志","情感"]`
  - [ ] 修改内容标签为 `["短句","文案"]`
  - [ ] 修改备注为"测试账号"
  - [ ] 点击"保存"按钮
  - [ ] 验证更新成功提示
- [ ] 刷新统计功能
  - [ ] 点击"刷新统计"按钮
  - [ ] 验证成功提示
- [ ] 返回列表功能
  - [ ] 点击返回按钮
  - [ ] 验证跳转到列表页

**预期效果**:
```
┌─────────────────────────────────────────────────────────────┐
│ ← 账号详情                      [刷新统计] [编辑]           │
├─────────────────────────────────────────────────────────────┤
│ 🖼️  百家语录                                                 │
│                                                              │
│     粉丝数      获赞总数    作品数      采集视频            │
│     0           0           0           0                   │
│                                                              │
│                                         平均评分  最高评分  │
│                                         0.0       0.0       │
│                                         采集次数            │
│                                         1                   │
├─────────────────────────────────────────────────────────────┤
│ [详细信息] [采集视频 (0)] [数据分析]                        │
├─────────────────────────────────────────────────────────────┤
│ 账号分类: 语录                                               │
│ 行业标签: ["励志","情感"]                                    │
│ 内容标签: ["短句","文案"]                                    │
│ 来源信息: 手动添加                                           │
│ 备注: 测试账号                                               │
└─────────────────────────────────────────────────────────────┘
```

### 阶段 3: 集成测试（完整流程）

#### 3.1 创建新采集任务

**步骤**:
1. 访问 http://localhost:3000/admin/shortvideo/collect
2. 输入账号链接（如：`https://www.douyin.com/user/MS4wLjABAAAA...`）
3. 点击"开始采集"
4. 等待采集完成

**预期结果**:
- ✅ 采集任务创建成功
- ✅ 自动创建 `sv_account` 记录
- ✅ 采集任务关联 `sv_account_id`
- ✅ 采集的视频关联 `sv_account_id`

#### 3.2 验证账号自动创建

**步骤**:
1. 访问 http://localhost:3000/admin/shortvideo/accounts
2. 查看账号列表

**预期结果**:
- ✅ 新账号出现在列表中
- ✅ 账号信息正确（昵称、sec_uid）
- ✅ 采集视频数 > 0
- ✅ 来源类型显示正确

#### 3.3 验证账号统计

**步骤**:
1. 点击新账号的"查看详情"
2. 查看统计数据

**预期结果**:
- ✅ 采集视频数正确
- ✅ 平均播放量 > 0
- ✅ 平均点赞数 > 0
- ✅ 平均爆款评分 > 0

#### 3.4 验证视频列表

**步骤**:
1. 切换到"采集视频" Tab
2. 查看视频列表

**预期结果**:
- ✅ 视频列表展示
- ✅ 视频信息完整（封面、标题、播放量、点赞数）
- ✅ 爆款评分显示
- ✅ 分页功能正常

---

## 三、当前状态

### 已完成 ✅

1. ✅ 数据库迁移成功
2. ✅ 账号表创建成功
3. ✅ 数据迁移完成（3 个账号）
4. ✅ 前端服务运行中
5. ✅ 前端代码编译通过
6. ✅ TypeScript 类型检查通过

### 待完成 ⏸️

1. ⏸️ 启动后端服务
2. ⏸️ 后端 API 测试
3. ⏸️ 前端页面测试
4. ⏸️ 集成测试

---

## 四、启动后端

### 方式 1: 使用 start.bat

```bash
start.bat
```

### 方式 2: 使用 Maven

```bash
mvn -pl douyin-operations-app -am spring-boot:run
```

### 方式 3: IDEA 运行

1. 打开 IDEA
2. 找到 `DouyinOperationsApplication.java`
3. 右键 → Run

---

## 五、测试检查清单

### 数据库层 ✅

- [x] sv_account 表创建成功
- [x] sv_account_collect_task.sv_account_id 字段添加成功
- [x] sv_viral_video.sv_account_id 字段添加成功
- [x] 索引创建成功
- [x] 数据迁移成功

### 后端层 ⏸️

- [ ] 后端服务启动成功
- [ ] 账号列表 API 正常
- [ ] 账号详情 API 正常
- [ ] 更新账号 API 正常
- [ ] 删除账号 API 正常
- [ ] 账号视频列表 API 正常
- [ ] 刷新统计 API 正常

### 前端层 ⏸️

- [ ] 账号列表页加载正常
- [ ] 搜索功能正常
- [ ] 筛选功能正常
- [ ] 排序功能正常
- [ ] 分页功能正常
- [ ] 账号详情页加载正常
- [ ] 编辑功能正常
- [ ] 刷新统计功能正常
- [ ] Tab 切换正常
- [ ] 视频列表展示正常

### 集成测试 ⏸️

- [ ] 创建采集任务自动创建账号
- [ ] 采集任务关联账号
- [ ] 视频关联账号
- [ ] 账号统计自动更新
- [ ] 关键词采集账号归类

---

## 六、下一步

1. **启动后端服务**
   ```bash
   start.bat
   ```

2. **访问账号列表页**
   ```
   http://localhost:3000/admin/shortvideo/accounts
   ```

3. **执行测试计划**
   - 按照上述测试计划逐项测试
   - 记录测试结果
   - 发现问题及时修复

4. **创建新采集任务**
   - 测试账号自动创建功能
   - 验证统计数据计算

---

## 七、已知问题

### 1. 现有账号无粉丝数据

**原因**: 迁移的账号是从采集任务中提取的，只有 sec_uid 和昵称，没有粉丝数等详细信息。

**解决方案**:
- 创建新的采集任务，会自动获取账号详细信息
- 或者手动调用刷新统计 API

### 2. 大部分视频未关联账号

**原因**: 只有 39 个视频通过采集任务关联了账号，其他 1204 个视频是手动添加的。

**影响**: 这些视频不会出现在账号的视频列表中。

**解决方案**: 正常，手动添加的视频不需要关联账号。

---

## 八、测试数据

### 测试账号

```sql
-- 查看所有账号
SELECT id, nickname, sec_uid, source_type, collect_count, total_collected_videos 
FROM sv_account 
WHERE deleted = 0;

-- 查看账号关联的采集任务
SELECT t.id, t.status, t.total_videos, t.collected_videos, a.nickname
FROM sv_account_collect_task t
JOIN sv_account a ON t.sv_account_id = a.id
WHERE t.deleted = 0;

-- 查看账号关联的视频
SELECT v.id, v.title, v.view_count, v.like_count, v.viral_score, a.nickname
FROM sv_viral_video v
JOIN sv_account a ON v.sv_account_id = a.id
WHERE v.deleted = 0
LIMIT 10;
```

---

需要我继续等待后端启动后进行测试吗？或者你可以手动启动后端，然后告诉我继续测试。
