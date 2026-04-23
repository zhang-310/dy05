# 账号采集模块优化方案

**日期**: 2026-04-21  
**目标**: 优化账号管理，支持账号级数据分析和账号基本信息管理

---

## 一、当前架构分析

### 1.1 现有数据模型

#### 表结构

**sv_account_collect_task** - 采集任务表
```sql
CREATE TABLE sv_account_collect_task (
    id                 BIGSERIAL PRIMARY KEY,
    owner_id           BIGINT NOT NULL,
    account_id         BIGINT,              -- 关联 douyin_account.id
    account_url        VARCHAR(512),
    account_name       VARCHAR(128),        -- 账号昵称
    sec_uid            VARCHAR(256),        -- 抖音 sec_uid（唯一标识）
    input_type         VARCHAR(32),         -- account_url/video_url/douyin_id/search_video
    original_input     VARCHAR(1024),
    status             VARCHAR(32),         -- pending/collecting/analyzing/completed/failed
    total_videos       INTEGER DEFAULT 0,
    collected_videos   INTEGER DEFAULT 0,
    analyzed_videos    INTEGER DEFAULT 0,
    indexed_videos     INTEGER DEFAULT 0,
    target_kb_id       BIGINT,
    error_message      TEXT,
    deleted            INTEGER DEFAULT 0,
    create_time        TIMESTAMP,
    update_time        TIMESTAMP
);
```

**sv_viral_video** - 爆款视频表
```sql
ALTER TABLE sv_viral_video 
ADD COLUMN collect_task_id BIGINT;  -- 关联采集任务
```

### 1.2 当前问题

1. **账号信息分散**
   - 账号基本信息（昵称、sec_uid）存储在 `sv_account_collect_task` 中
   - 每次采集创建新任务，账号信息重复存储
   - 无法统一管理同一账号的多次采集

2. **缺少账号维度统计**
   - 无法查看单个账号的所有采集视频
   - 无法统计账号级别的数据（总播放量、平均点赞率等）
   - 无法分析账号的内容风格和爆款规律

3. **账号信息不完整**
   - 只有昵称和 sec_uid
   - 缺少粉丝数、简介、头像、认证信息等
   - 无法判断账号质量和影响力

4. **关键词采集账号管理混乱**
   - 关键词采集会产生大量账号
   - 这些账号没有统一管理
   - 无法去重和归类

---

## 二、优化方案

### 2.1 新增账号主表

#### 表结构设计

**sv_account** - 短视频账号主表
```sql
CREATE TABLE IF NOT EXISTS sv_account (
    id                  BIGSERIAL PRIMARY KEY,
    owner_id            BIGINT NOT NULL,
    sec_uid             VARCHAR(256) NOT NULL,      -- 抖音唯一标识
    douyin_id           VARCHAR(128),               -- 抖音号
    nickname            VARCHAR(128),               -- 昵称
    avatar_url          VARCHAR(512),               -- 头像
    signature           TEXT,                       -- 简介
    follower_count      BIGINT DEFAULT 0,           -- 粉丝数
    following_count     BIGINT DEFAULT 0,           -- 关注数
    total_favorited     BIGINT DEFAULT 0,           -- 获赞总数
    video_count         INTEGER DEFAULT 0,          -- 作品数
    is_verified         BOOLEAN DEFAULT FALSE,      -- 是否认证
    verification_type   VARCHAR(32),                -- 认证类型
    
    -- 采集统计
    collect_count       INTEGER DEFAULT 0,          -- 采集次数
    last_collect_time   TIMESTAMP,                  -- 最后采集时间
    total_collected_videos INTEGER DEFAULT 0,       -- 累计采集视频数
    
    -- 分析统计
    avg_view_count      BIGINT DEFAULT 0,           -- 平均播放量
    avg_like_count      INTEGER DEFAULT 0,          -- 平均点赞数
    avg_viral_score     DECIMAL(5,2) DEFAULT 0,     -- 平均爆款评分
    top_viral_score     DECIMAL(5,2) DEFAULT 0,     -- 最高爆款评分
    
    -- 标签与分类
    industry_tags       VARCHAR(512),               -- 行业标签（JSON数组）
    content_tags        VARCHAR(512),               -- 内容标签（JSON数组）
    account_category    VARCHAR(64),                -- 账号分类
    
    -- 来源与备注
    source_type         VARCHAR(32),                -- manual/keyword_search/recommend
    source_keyword      VARCHAR(256),               -- 来源关键词
    notes               TEXT,                       -- 备注
    
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_sv_acc_sec_uid ON sv_account(owner_id, sec_uid) WHERE deleted = 0;
CREATE INDEX idx_sv_acc_owner ON sv_account(owner_id, deleted);
CREATE INDEX idx_sv_acc_category ON sv_account(account_category) WHERE deleted = 0;
CREATE INDEX idx_sv_acc_source ON sv_account(source_type, source_keyword) WHERE deleted = 0;
```

### 2.2 修改采集任务表

```sql
-- 添加账号主表关联
ALTER TABLE sv_account_collect_task 
ADD COLUMN sv_account_id BIGINT;  -- 关联 sv_account.id

CREATE INDEX idx_sv_act_account ON sv_account_collect_task(sv_account_id) WHERE deleted = 0;

-- account_name、sec_uid 保留用于兼容，但主要从 sv_account 读取
```

### 2.3 修改爆款视频表

```sql
-- 添加账号主表关联
ALTER TABLE sv_viral_video 
ADD COLUMN sv_account_id BIGINT;  -- 关联 sv_account.id

CREATE INDEX idx_sv_vv_account ON sv_viral_video(sv_account_id) WHERE deleted = 0;

-- author_name、author_id 保留用于兼容
```

---

## 三、功能实现

### 3.1 账号自动创建与更新

#### 采集流程优化

```java
// AccountVideoCollectServiceImpl.java

@Override
@Transactional
public AccountCollectTaskVO startCollect(AccountCollectTaskSaveVO vo, Long userId) {
    // 1. 解析输入
    DouyinUrlResolver.ResolveResult resolved = douyinUrlResolver.resolve(rawInput, userId);
    
    // 2. 查找或创建账号记录
    SvAccount account = null;
    if (StringUtils.hasText(resolved.secUid())) {
        account = findOrCreateAccount(resolved, userId);
    }
    
    // 3. 创建采集任务
    SvAccountCollectTask task = new SvAccountCollectTask();
    task.setOwnerId(userId);
    task.setSvAccountId(account != null ? account.getId() : null);
    task.setSecUid(resolved.secUid());
    task.setAccountName(resolved.accountName());
    // ... 其他字段
    
    taskRepository.save(task);
    
    // 4. 异步采集
    asyncRunner.runCollectAsync(task.getId());
    
    return toVO(task);
}

private SvAccount findOrCreateAccount(DouyinUrlResolver.ResolveResult resolved, Long userId) {
    String secUid = resolved.secUid();
    
    // 查找现有账号
    Optional<SvAccount> existing = accountRepository.findByOwnerIdAndSecUid(userId, secUid);
    if (existing.isPresent()) {
        return existing.get();
    }
    
    // 创建新账号
    SvAccount account = new SvAccount();
    account.setOwnerId(userId);
    account.setSecUid(secUid);
    account.setNickname(resolved.accountName());
    account.setSourceType("manual");  // 或 keyword_search
    
    // 尝试获取账号详细信息
    try {
        enrichAccountInfo(account);
    } catch (Exception e) {
        log.warn("获取账号详细信息失败: {}", e.getMessage());
    }
    
    return accountRepository.save(account);
}

private void enrichAccountInfo(SvAccount account) {
    // 调用抖音 API 或爬虫获取账号详细信息
    if (accountVideoScraper != null) {
        AccountInfo info = accountVideoScraper.getAccountInfo(account.getSecUid());
        if (info != null) {
            account.setDouyinId(info.getDouyinId());
            account.setAvatarUrl(info.getAvatarUrl());
            account.setSignature(info.getSignature());
            account.setFollowerCount(info.getFollowerCount());
            account.setFollowingCount(info.getFollowingCount());
            account.setTotalFavorited(info.getTotalFavorited());
            account.setVideoCount(info.getVideoCount());
            account.setIsVerified(info.getIsVerified());
            account.setVerificationType(info.getVerificationType());
        }
    }
}
```

### 3.2 账号统计更新

#### 采集完成后更新账号统计

```java
// AccountCollectAsyncRunner.java

private void updateAccountStatistics(Long accountId) {
    if (accountId == null) return;
    
    SvAccount account = accountRepository.findById(accountId).orElse(null);
    if (account == null) return;
    
    // 统计该账号的所有采集视频
    List<SvViralVideo> videos = viralVideoRepository.findAll((root, query, cb) -> cb.and(
        cb.equal(root.get("svAccountId"), accountId),
        cb.equal(root.get("deleted"), 0)
    ));
    
    if (videos.isEmpty()) return;
    
    // 计算统计数据
    long totalViews = videos.stream()
        .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
        .sum();
    long avgViews = totalViews / videos.size();
    
    long totalLikes = videos.stream()
        .mapToLong(v -> v.getLikeCount() != null ? v.getLikeCount() : 0)
        .sum();
    int avgLikes = (int) (totalLikes / videos.size());
    
    double avgScore = videos.stream()
        .mapToInt(v -> v.getViralScore() != null ? v.getViralScore() : 0)
        .average()
        .orElse(0.0);
    
    int topScore = videos.stream()
        .mapToInt(v -> v.getViralScore() != null ? v.getViralScore() : 0)
        .max()
        .orElse(0);
    
    // 更新账号统计
    account.setTotalCollectedVideos(videos.size());
    account.setAvgViewCount(avgViews);
    account.setAvgLikeCount(avgLikes);
    account.setAvgViralScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
    account.setTopViralScore(BigDecimal.valueOf(topScore));
    account.setLastCollectTime(new Timestamp(System.currentTimeMillis()));
    
    accountRepository.save(account);
}
```

### 3.3 账号管理 API

#### Controller

```java
@RestController
@RequestMapping("/api/v1/short-video/account")
public class SvAccountController {
    
    @Resource
    private SvAccountService accountService;
    
    /**
     * 账号列表
     */
    @PostMapping("/list")
    public RESTResult<PageResultVO<SvAccountVO>> list(@RequestBody SvAccountSearchVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        return RESTResult.success(accountService.searchAccounts(vo, userId));
    }
    
    /**
     * 账号详情
     */
    @PostMapping("/get")
    public RESTResult<SvAccountDetailVO> get(@RequestBody IdVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        return RESTResult.success(accountService.getAccountDetail(vo.getId(), userId));
    }
    
    /**
     * 账号下的视频列表
     */
    @PostMapping("/videos")
    public RESTResult<PageResultVO<ViralVideoVO>> videos(@RequestBody AccountVideosQueryVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        return RESTResult.success(accountService.getAccountVideos(vo, userId));
    }
    
    /**
     * 账号分析报告
     */
    @PostMapping("/analyze")
    public RESTResult<AccountAnalysisReportVO> analyze(@RequestBody IdVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        return RESTResult.success(accountService.analyzeAccount(vo.getId(), userId));
    }
    
    /**
     * 更新账号信息
     */
    @PostMapping("/update")
    public RESTResult<Void> update(@RequestBody SvAccountUpdateVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        accountService.updateAccount(vo, userId);
        return RESTResult.success();
    }
    
    /**
     * 删除账号
     */
    @PostMapping("/delete")
    public RESTResult<Void> delete(@RequestBody IdVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        accountService.deleteAccount(vo.getId(), userId);
        return RESTResult.success();
    }
}
```

---

## 四、前端实现

### 4.1 账号列表页面

**路由**: `/admin/shortvideo/accounts`

**功能**:
- 账号列表展示（昵称、粉丝数、采集次数、平均爆款评分）
- 搜索过滤（昵称、分类、来源关键词）
- 排序（粉丝数、爆款评分、采集次数）
- 批量操作（删除、标签管理）

### 4.2 账号详情页面

**路由**: `/admin/shortvideo/accounts/:id`

**Tab 1: 基本信息**
- 头像、昵称、抖音号
- 粉丝数、获赞数、作品数
- 认证信息、简介
- 行业标签、内容标签
- 编辑按钮（更新标签、备注）

**Tab 2: 采集视频**
- 该账号下所有采集的视频列表
- 支持筛选（爆款评分、播放量）
- 支持批量拆解分析

**Tab 3: 数据分析**
- 视频数据分布图表
  - 播放量分布
  - 点赞率分布
  - 爆款评分分布
- 内容风格分析
  - 高频关键词
  - 常用话题标签
  - 视频时长分布
- 发布规律
  - 发布时间分布
  - 发布频率

**Tab 4: 采集历史**
- 该账号的所有采集任务记录
- 每次采集的视频数量、时间

---

## 五、实施步骤

### 阶段 1: 数据库迁移（1天）

1. 创建 `sv_account` 表
2. 添加 `sv_account_collect_task.sv_account_id` 字段
3. 添加 `sv_viral_video.sv_account_id` 字段
4. 数据迁移脚本（将现有数据迁移到新表）

### 阶段 2: 后端实现（2-3天）

1. Entity、Repository、Service 层
2. 账号自动创建与更新逻辑
3. 账号统计计算逻辑
4. 账号管理 API
5. 账号分析 API

### 阶段 3: 前端实现（2-3天）

1. 账号列表页面
2. 账号详情页面
3. 账号分析图表
4. 与现有采集流程集成

### 阶段 4: 测试与优化（1-2天）

1. 功能测试
2. 性能优化
3. 数据一致性验证

---

## 六、预期效果

### 6.1 账号管理

- ✅ 统一管理所有采集的账号
- ✅ 自动去重（同一 sec_uid 只创建一次）
- ✅ 账号信息完整（粉丝数、简介、认证等）
- ✅ 支持标签分类和备注

### 6.2 数据分析

- ✅ 账号级别的数据统计
- ✅ 内容风格分析
- ✅ 爆款规律发现
- ✅ 账号质量评估

### 6.3 关键词采集优化

- ✅ 关键词采集的账号自动归类
- ✅ 可以查看某个关键词采集了哪些账号
- ✅ 可以对比不同账号的数据表现

---

## 七、后续扩展

### 7.1 账号监控

- 定期更新账号信息（粉丝数、作品数）
- 监控账号新发布的视频
- 爆款预警（新视频达到阈值时通知）

### 7.2 账号推荐

- 基于现有账号推荐相似账号
- 基于关键词推荐优质账号
- 基于爆款率推荐值得学习的账号

### 7.3 账号对比

- 多个账号的数据对比
- 内容风格对比
- 爆款策略对比

---

## 八、技术要点

### 8.1 数据一致性

- 采集任务创建时自动创建/更新账号
- 视频采集完成后关联账号
- 定期同步账号统计数据

### 8.2 性能优化

- 账号统计数据异步计算
- 使用缓存减少数据库查询
- 分页查询优化

### 8.3 扩展性

- 账号表设计支持多平台（抖音、快手、小红书）
- 统计字段可扩展
- 标签系统灵活

---

需要我开始实施这个方案吗？我可以先从数据库迁移脚本开始。
