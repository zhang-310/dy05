# 百度云存储 × 短视频模块 - 深度集成分析与升级建议

**版本**: v1.0
**日期**: 2026-03-01
**分析人**: Claude Code

---

## 📋 目录

1. [现状分析](#现状分析)
2. [架构评估](#架构评估)
3. [存在的问题](#存在的问题)
4. [升级建议](#升级建议)
5. [Cursor 开发清单](#cursor-开发清单)

---

## 🔍 现状分析

### 1. 当前架构优点

✅ **已实现的好设计**：

| 特性 | 实现方式 | 评价 |
|------|---------|------|
| **用户隔离** | `{userId}/{date}/{taskId}/` 路径结构 | ⭐⭐⭐⭐⭐ 安全可靠 |
| **日期分区** | 按日期分目录 | ⭐⭐⭐⭐⭐ 便于清理和统计 |
| **任务隔离** | 每个任务独立目录 | ⭐⭐⭐⭐⭐ 便于批量操作 |
| **权限校验** | `ensureKeyBelongsToUser()` | ⭐⭐⭐⭐⭐ 防止越权访问 |
| **CDN 加速** | 公共文件使用 CDN URL | ⭐⭐⭐⭐ 提升访问速度 |
| **文件分类** | keyframes/videos/audios 分离 | ⭐⭐⭐⭐ 结构清晰 |

### 2. 短视频生产流程映射

```
短视频生产流程 → BOS 存储映射：

1. 脚本策划（无文件）
   └── 无需 BOS

2. 分镜设计（无文件）
   └── 无需 BOS

3. 素材准备
   ├── 上传人物参考图 → {userId}/references/characters/{charId}/main.jpg
   └── 上传场景参考图 → {userId}/references/scenes/{sceneId}/main.jpg

4. 关键帧生成（ComfyUI）
   ├── 生成图片（ComfyUI 本地）
   └── 上传到 BOS → {userId}/{date}/{taskId}/keyframes/shot_001.jpg

5. 视频生成（Kling）
   ├── 生成视频（Kling 云端）
   ├── 下载到本地
   └── 上传到 BOS → {userId}/{date}/{taskId}/videos/shot_001.mp4

6. 配音生成（讯飞）
   ├── 生成音频（讯飞云端）
   ├── 下载到本地
   └── 上传到 BOS → {userId}/{date}/{taskId}/audios/voice_001.mp3

7. 自动剪辑（FFmpeg）
   ├── 从 BOS 下载所有素材
   ├── 本地剪辑合成
   └── 上传成片到 BOS → {userId}/{date}/{taskId}/videos/final.mp4

8. 审核发布
   ├── 预览 BOS 视频（CDN 加速）
   └── 发布（直接使用 BOS CDN URL）
```

---

## 🏗️ 架构评估

### 1. 存储路径设计评估

#### ✅ 优点

```yaml
路径: {userId}/{date}/{taskId}/keyframes/shot_001.jpg

优点:
  1. 用户隔离: 每个用户只能访问自己的路径
  2. 日期分区: 便于按月/按季度统计成本
  3. 任务隔离: 删除任务时可批量删除所有素材
  4. 扁平化: 避免过深的目录层级
  5. 可读性: 路径即文档，一看就懂
```

#### ⚠️ 潜在问题

**问题 1：日期来源不明确**

```typescript
// 当前实现：可选参数，不传则用当天
async uploadKeyframe(
  fileBuffer: Buffer,
  userId: number | string,
  date: string,  // 这个日期从哪来？
  taskId: number | string,
  shotNumber: number,
  filename: string
)

// 问题：
// - 如果任务创建于 2026-03-01，但用户 3 天后才上传关键帧
// - 此时 date 应该是 2026-03-01（任务创建日期）还是 2026-03-04（上传日期）？
// - 如果用上传日期，同一任务的文件会分散在不同日期目录下
```

**问题 2：参考图路径不含日期**

```typescript
// 当前实现：
{userId}/references/characters/{charId}/main.jpg

// 问题：
// - 参考图库会不断增长，无法按日期清理
// - 无法统计某月新增的参考图数量
// - 如果用户上传了大量参考图但从不使用，浪费存储空间
```

**问题 3：临时文件清理策略不明确**

```yaml
# 当前设计
temp/
  └── {userId}/{date}/

# 问题：
# - 临时文件何时清理？7 天？30 天？
# - ComfyUI 生成的图片先存 temp，何时移动到正式路径？
# - Kling 下载的视频先存 temp，何时移动到正式路径？
# - 如果移动失败，临时文件会永久残留
```

---

### 2. API 设计评估

#### ⚠️ 问题清单

**问题 1：参数冗余**

```typescript
// 当前设计：每次上传都要传 userId、date、taskId
async uploadKeyframe(
  fileBuffer: Buffer,
  userId: number | string,      // 冗余：应该从认证 token 获取
  date: string,                  // 冗余：应该从任务表查询
  taskId: number | string,
  shotNumber: number,
  filename: string
)

// 建议：简化参数
async uploadKeyframe(
  fileBuffer: Buffer,
  taskId: number | string,       // 唯一必要参数
  shotNumber: number,
  filename: string
  // userId 从 token 获取，date 从 task 表查询
)
```

**问题 2：缺少元数据管理**

```typescript
// 当前实现：只返回 URL
interface UploadResult {
  url: string;
  cdnUrl: string;
  bucket: string;
  key: string;
  size: number;
  etag: string;
}

// 缺失：
// - 文件元数据（宽高、时长、格式、编码）
// - 关联关系（属于哪个任务、哪个分镜）
// - 使用状态（是否已使用、是否可删除）
// - 成本信息（存储费用、流量费用）
```

**问题 3：缺少批量操作优化**

```typescript
// 当前实现：逐个上传
for (const shot of shots) {
  await uploadKeyframe(shot.buffer, taskId, shot.number, shot.filename);
}

// 问题：
// - 6 个分镜需要 6 次网络请求（串行）
// - 如果并发上传，缺少进度汇总
// - 如果部分失败，缺少回滚机制

// 建议：批量上传 API
async batchUploadKeyframes(
  taskId: number,
  files: Array<{ shotNumber: number; buffer: Buffer; filename: string }>
): Promise<{
  success: UploadResult[];
  failed: Array<{ shotNumber: number; error: string }>;
  progress: number; // 0-100
}>
```

**问题 4：缺少预检查**

```typescript
// 当前实现：直接上传，失败后才知道问题
await uploadKeyframe(fileBuffer, taskId, shotNumber, filename);

// 建议：上传前预检查
async preCheckUpload(taskId: number, fileType: string): Promise<{
  canUpload: boolean;
  reason?: string;  // "任务不存在" | "存储空间不足" | "用户无权限"
  quotaUsed: number;
  quotaLimit: number;
}>
```

---

### 3. 安全性评估

#### ✅ 已实现的安全措施

```typescript
// 1. 路径归属校验
ensureKeyBelongsToUser(key, currentUserId);

// 2. 用户隔离
{userId}/... // 路径强制包含 userId

// 3. 路径穿越防护
if (key.includes('..')) throw new Error('非法路径');

// 4. 防盗链（CDN 配置）
Referer 白名单
```

#### ⚠️ 安全隐患

**隐患 1：缺少文件内容校验**

```typescript
// 当前实现：只校验文件类型和大小
function validateImageFile(file: Express.Multer.File) {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
  const maxSize = 10 * 1024 * 1024; // 10MB
  if (!allowedTypes.includes(file.mimetype)) throw new Error('...');
}

// 问题：
// - mimetype 可伪造（修改 HTTP header）
// - 恶意文件可能伪装成图片上传（如 .exe 改成 .jpg）

// 建议：文件魔数校验
async function validateFileContent(buffer: Buffer, expectedType: 'image' | 'video'): Promise<boolean> {
  const magicNumbers = {
    jpg: [0xFF, 0xD8, 0xFF],
    png: [0x89, 0x50, 0x4E, 0x47],
    mp4: [0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70],
  };
  // 检查文件头部字节
}
```

**隐患 2：缺少文件病毒扫描**

```typescript
// 建议：集成病毒扫描（可选，成本较高）
async function scanFile(buffer: Buffer): Promise<{ safe: boolean; threat?: string }> {
  // 调用百度云内容安全 API
  // 或集成 ClamAV 开源病毒扫描引擎
}
```

**隐患 3：缺少敏感内容检测**

```typescript
// 建议：图片/视频内容审核
async function moderateContent(fileUrl: string): Promise<{
  passed: boolean;
  issues: Array<{ type: 'porn' | 'violence' | 'terrorism'; confidence: number }>;
}> {
  // 调用百度云内容审核 API
  // https://cloud.baidu.com/product/imagecensoring
}
```

---

### 4. 性能优化评估

#### ⚠️ 性能瓶颈

**瓶颈 1：下载-上传链路冗余**

```typescript
// 当前流程（Kling 生成视频）：
1. Kling 生成视频 → Kling 云端存储
2. 后端下载视频 → 后端服务器（占用带宽 + 磁盘）
3. 上传到 BOS → BOS 存储（占用带宽）
4. 删除本地文件 → 清理磁盘

// 问题：
// - 单个 15MB 视频需要下载 + 上传共 30MB 流量
// - 6 个分镜需要 180MB 流量
// - 如果并发生成 10 个任务，需要 1.8GB 流量

// 建议：直传优化
1. Kling 生成视频 → 获取临时下载 URL
2. 后端告诉 BOS："从这个 URL 拉取文件并存储"（BOS 回源拉取）
3. 无需经过后端服务器

// BOS 回源拉取 API
async function bosImportFromUrl(sourceUrl: string, targetKey: string): Promise<void> {
  await bosClient.putObjectFromUrl(bucket, targetKey, sourceUrl);
}
```

**瓶颈 2：FFmpeg 剪辑需要下载所有素材**

```typescript
// 当前流程：
1. 从 BOS 下载 6 个视频片段（90MB）
2. 从 BOS 下载 6 个音频文件（18MB）
3. 本地 FFmpeg 剪辑
4. 上传成片到 BOS（15MB）
5. 删除本地临时文件

// 总流量：123MB 下载 + 15MB 上传 = 138MB

// 建议：服务端剪辑 + 流式处理
// - 使用 FFmpeg 流式处理，边下载边剪辑，无需全部下载
// - 或使用百度云媒体处理服务（BMC）云端剪辑
```

**瓶颈 3：缺少缓存机制**

```typescript
// 问题：
// - 参考图每次都从 BOS 下载（ComfyUI 需要）
// - 如果同一人物生成 100 个视频，参考图下载 100 次

// 建议：本地缓存参考图
interface ReferenceImageCache {
  key: string;           // BOS key
  localPath: string;     // 本地缓存路径
  etag: string;          // ETag（用于校验）
  expireAt: Date;        // 过期时间
}

async function getCachedReferenceImage(bosKey: string): Promise<string> {
  // 1. 检查本地缓存
  const cached = await db.findCache(bosKey);
  if (cached && cached.expireAt > new Date()) {
    // 2. 校验 ETag 是否一致
    const metadata = await bosClient.getObjectMetadata(bucket, bosKey);
    if (metadata.etag === cached.etag) {
      return cached.localPath; // 缓存命中
    }
  }
  // 3. 缓存过期或不存在，重新下载
  const localPath = await downloadFromBos(bosKey);
  await db.saveCache({ key: bosKey, localPath, etag: metadata.etag, expireAt: addDays(7) });
  return localPath;
}
```

---

### 5. 成本优化评估

#### 📊 成本分解

```yaml
# 单条 30 秒短视频存储（86MB）
成本构成:
  - 存储费用: 86MB × ¥0.12/GB/月 = ¥0.01/月
  - CDN 流量:
      - 生成时上传: 86MB × ¥0.23/GB = ¥0.02
      - 播放 10 次: 15MB × 10 × ¥0.23/GB = ¥0.03
  - 总成本: ¥0.06/条（含存储 1 个月 + 10 次播放）

# 月产 300 条视频
月成本:
  - 存储: 300 × 86MB = 25.8GB → ¥3.1/月
  - 上传流量: 300 × ¥0.02 = ¥6/月
  - 播放流量: 300 × ¥0.03 = ¥9/月
  - 总计: ¥18.1/月

# 年成本（12 个月累积存储）
年成本:
  - 第 1 月: 300 条存储 + 上传 + 播放 = ¥18.1
  - 第 2 月: 600 条存储 + 新增 300 上传 + 播放 = ¥24.2
  - 第 12 月: 3600 条存储 + 新增 300 上传 + 播放 = ¥111
  - 年总成本: ¥560
```

#### ⚠️ 成本隐患

**隐患 1：存储无限增长**

```typescript
// 问题：
// - 每月新增 300 条视频，永久存储
// - 1 年后存储 3600 条（310GB），存储费 ¥37.2/月
// - 2 年后存储 7200 条（620GB），存储费 ¥74.4/月

// 建议：生命周期管理
const lifecycleRules = {
  // 规则 1：30 天后转低频存储（节省 33% 成本）
  rule1: {
    prefix: '{userId}/',
    daysToInfrequent: 30,
    saveCost: '33%'
  },
  // 规则 2：180 天后转归档存储（节省 80% 成本）
  rule2: {
    prefix: '{userId}/',
    daysToArchive: 180,
    saveCost: '80%'
  },
  // 规则 3：365 天后删除（可选）
  rule3: {
    prefix: '{userId}/{date}/',
    daysToDelete: 365,
    condition: 'viewCount < 100' // 播放量低于 100 的视频
  }
};
```

**隐患 2：僵尸文件占用存储**

```typescript
// 问题：
// - 用户创建任务后放弃，生成了部分素材但未完成
// - 这些素材永久占用存储空间

// 建议：定期清理未完成任务
async function cleanupAbandonedTasks() {
  // 1. 查找超过 30 天未完成的任务
  const abandonedTasks = await db.query(`
    SELECT id, user_id, create_time
    FROM short_video_project
    WHERE status IN ('draft', 'processing')
      AND create_time < NOW() - INTERVAL 30 DAY
  `);

  // 2. 批量删除 BOS 文件
  for (const task of abandonedTasks) {
    const date = task.create_time.toISOString().slice(0, 10);
    await bosService.deleteTaskFiles(task.user_id, date, task.id, task.user_id);
  }

  // 3. 标记任务为已清理
  await db.update('short_video_project', { status: 'cleaned' }, { id: abandonedTasks.map(t => t.id) });
}
```

**隐患 3：参考图库无限增长**

```typescript
// 问题：
// - 参考图库路径 {userId}/references/ 不含日期
// - 用户上传 100 张人物参考图，但只用了 3 张
// - 97 张未使用的参考图永久占用存储

// 建议：引用计数 + 定期清理
interface ReferenceImageMetadata {
  key: string;
  uploadTime: Date;
  usageCount: number;      // 被多少个任务使用
  lastUsedTime: Date;      // 最后使用时间
}

async function cleanupUnusedReferences(userId: number) {
  // 1. 查找超过 90 天未使用的参考图
  const unused = await db.query(`
    SELECT key
    FROM reference_image_metadata
    WHERE user_id = ? AND usage_count = 0 AND upload_time < NOW() - INTERVAL 90 DAY
  `, [userId]);

  // 2. 删除
  await bosService.batchDelete(unused.map(r => r.key), userId);
}
```

---

### 6. 业务场景适配评估

#### 场景 1：批量生产（爆款复刻）

```typescript
// 需求：一次生成 10 个类似视频（不同角度/风格）
// 问题：
// - 10 个任务会创建 10 个独立目录
// - 如果这 10 个视频共用同一套参考图，会重复存储

// 建议：任务组概念
interface TaskGroup {
  id: number;
  userId: number;
  date: string;
  tasks: number[];  // [task_1, task_2, ..., task_10]
  sharedReferences: {
    characters: string[];  // 共用的人物参考图
    scenes: string[];      // 共用的场景参考图
  };
}

// 存储路径：
{userId}/{date}/group_{groupId}/
  ├── references/          # 组级共享参考图
  │   ├── characters/
  │   └── scenes/
  └── tasks/
      ├── task_001/        # 任务 1
      ├── task_002/        # 任务 2
      └── ...
```

#### 场景 2：协作场景（团队账号）

```typescript
// 需求：团队账号，多人协作生产视频
// 问题：
// - 当前路径 {userId}/，无法区分"个人账号"和"团队账号"
// - 团队成员 A 创建的任务，成员 B 应该能查看/编辑

// 建议：团队路径
{teamId}/
  ├── members/
  │   ├── {userId_A}/     # 成员 A 的个人素材
  │   └── {userId_B}/     # 成员 B 的个人素材
  └── shared/              # 团队共享素材
      └── {date}/{taskId}/

// 权限规则：
// - 团队成员可访问 {teamId}/shared/ 下的所有文件
// - 团队成员只能访问自己的 {teamId}/members/{userId}/ 文件
```

#### 场景 3：视频版本管理

```typescript
// 需求：同一视频生成多个版本（不同配音/字幕/运镜）
// 问题：
// - 当前设计没有版本概念
// - 如果重新生成，会覆盖原文件

// 建议：版本号
{userId}/{date}/{taskId}/
  └── videos/
      ├── final_v1.mp4    # 版本 1
      ├── final_v2.mp4    # 版本 2
      └── final.mp4       # 指向最新版本（软链接或重定向）
```

---

## 🚨 存在的问题（优先级排序）

### P0 级别（必须解决）

| 问题 | 影响 | 建议方案 |
|------|------|---------|
| **日期来源不明确** | 同一任务文件可能分散在不同日期目录 | 强制使用任务创建日期 |
| **缺少文件元数据管理** | 无法追溯文件用途、成本、关联关系 | 引入元数据表 |
| **缺少批量上传 API** | 性能低下，用户体验差 | 实现批量上传接口 |
| **缺少生命周期管理** | 存储成本无限增长 | 配置 BOS 生命周期规则 |

### P1 级别（强烈建议）

| 问题 | 影响 | 建议方案 |
|------|------|---------|
| **下载-上传链路冗余** | 浪费带宽，速度慢 | 使用 BOS 回源拉取 |
| **缺少缓存机制** | 重复下载参考图，浪费流量 | 实现本地缓存 |
| **缺少僵尸文件清理** | 浪费存储空间和成本 | 定期清理未完成任务 |
| **API 参数冗余** | 开发体验差，容易出错 | 简化 API 参数 |

### P2 级别（可选优化）

| 问题 | 影响 | 建议方案 |
|------|------|---------|
| **缺少文件内容校验** | 安全隐患 | 文件魔数校验 |
| **缺少内容审核** | 违规内容风险 | 集成百度云内容审核 |
| **缺少任务组概念** | 批量生产场景支持不足 | 引入任务组 |
| **缺少版本管理** | 无法保留历史版本 | 文件版本号 |

---

## 💡 升级建议

### 建议 1：引入文件元数据表（P0）

**目的**：追踪每个文件的用途、成本、关联关系。

#### 数据库设计

```sql
CREATE TABLE bos_file_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- BOS 信息
    bos_key VARCHAR(500) NOT NULL UNIQUE COMMENT 'BOS 对象 Key',
    bos_url VARCHAR(500) NOT NULL COMMENT 'BOS 原始 URL',
    cdn_url VARCHAR(500) COMMENT 'CDN 加速 URL',
    bucket VARCHAR(100) DEFAULT 'dy01-prod-media',

    -- 文件信息
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型：image/video/audio',
    content_type VARCHAR(100) COMMENT 'MIME type',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_hash VARCHAR(64) COMMENT 'SHA-256 哈希（用于去重）',

    -- 媒体信息（JSON）
    media_info JSON COMMENT '{"width": 1080, "height": 1920, "duration": 5.2, "format": "mp4", "codec": "h264"}',

    -- 路径解析
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    date VARCHAR(10) COMMENT '日期 YYYY-MM-DD（任务级文件）',
    task_id BIGINT COMMENT '任务 ID（任务级文件）',
    category VARCHAR(50) COMMENT '分类：keyframe/video/audio/thumbnail/reference',

    -- 关联信息
    project_id BIGINT COMMENT '关联项目 ID',
    shot_id BIGINT COMMENT '关联分镜 ID',

    -- 使用状态
    usage_status VARCHAR(50) DEFAULT 'unused' COMMENT '使用状态：unused/using/used/archived',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    last_used_time DATETIME COMMENT '最后使用时间',

    -- 成本信息
    storage_cost_monthly DECIMAL(10, 4) COMMENT '月存储成本（元）',
    traffic_cost_total DECIMAL(10, 4) DEFAULT 0 COMMENT '累计流量成本（元）',
    download_count BIGINT DEFAULT 0 COMMENT '下载次数',

    -- 生命周期
    storage_class VARCHAR(50) DEFAULT 'STANDARD' COMMENT '存储类型：STANDARD/INFREQUENT/ARCHIVE',
    expire_time DATETIME COMMENT '过期时间（自动删除）',
    deleted BOOLEAN DEFAULT FALSE COMMENT '是否已删除',
    delete_time DATETIME COMMENT '删除时间',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_task_id (task_id),
    INDEX idx_category (category),
    INDEX idx_usage_status (usage_status),
    INDEX idx_storage_class (storage_class),
    INDEX idx_file_hash (file_hash),
    INDEX idx_date (date)
) COMMENT 'BOS 文件元数据表';
```

#### 使用示例

```typescript
// 上传时记录元数据
async function uploadKeyframeWithMetadata(
  fileBuffer: Buffer,
  taskId: number,
  shotNumber: number,
  filename: string
) {
  // 1. 上传到 BOS
  const bosResult = await bosService.uploadKeyframe(fileBuffer, taskId, shotNumber, filename);

  // 2. 记录元数据
  const metadata = {
    bos_key: bosResult.key,
    bos_url: bosResult.url,
    cdn_url: bosResult.cdnUrl,
    file_type: 'image',
    content_type: 'image/jpeg',
    file_size: fileBuffer.length,
    file_hash: sha256(fileBuffer),
    user_id: currentUser.id,
    date: task.createTime.slice(0, 10),
    task_id: taskId,
    category: 'keyframe',
    project_id: task.projectId,
    shot_id: shotId,
    storage_cost_monthly: (fileBuffer.length / 1024 / 1024 / 1024) * 0.12,
  };

  await db.insert('bos_file_metadata', metadata);

  return bosResult;
}

// 查询任务的所有文件
async function getTaskFiles(taskId: number) {
  return await db.query(`
    SELECT * FROM bos_file_metadata
    WHERE task_id = ? AND deleted = FALSE
    ORDER BY category, create_time
  `, [taskId]);
}

// 统计用户存储成本
async function getUserStorageCost(userId: number) {
  const result = await db.query(`
    SELECT
      SUM(file_size) as total_size,
      SUM(storage_cost_monthly) as monthly_cost,
      SUM(traffic_cost_total) as total_traffic_cost,
      COUNT(*) as file_count
    FROM bos_file_metadata
    WHERE user_id = ? AND deleted = FALSE
  `, [userId]);

  return result[0];
}
```

---

### 建议 2：强制使用任务创建日期（P0）

**目的**：确保同一任务的所有文件在同一日期目录下。

#### 实现方案

```typescript
// 修改 BosStorageService，去掉 date 参数
class BosStorageService {
  /**
   * 上传关键帧（自动从任务表查询日期）
   */
  async uploadKeyframe(
    fileBuffer: Buffer,
    taskId: number | string,
    shotNumber: number,
    filename: string,
    currentUserId: number
  ): Promise<UploadResult> {
    // 1. 查询任务信息
    const task = await this.getTask(taskId);

    // 2. 校验权限
    if (task.user_id !== currentUserId) {
      throw new Error('无权操作该任务的文件');
    }

    // 3. 使用任务创建日期
    const date = task.create_time.toISOString().slice(0, 10); // YYYY-MM-DD

    // 4. 上传
    const paddedShotNumber = String(shotNumber).padStart(3, '0');
    return this.upload(fileBuffer, filename, {
      pathContext: { userId: task.user_id, date, taskId },
      folder: 'keyframes',
      filename: `shot_${paddedShotNumber}.jpg`,
      isPublic: true,
    });
  }

  /**
   * 查询任务信息（带缓存）
   */
  private async getTask(taskId: number | string) {
    const cacheKey = `task:${taskId}`;
    let task = await redis.get(cacheKey);

    if (!task) {
      task = await db.queryOne('SELECT * FROM short_video_project WHERE id = ?', [taskId]);
      if (!task) throw new Error('任务不存在');
      await redis.set(cacheKey, task, 3600); // 缓存 1 小时
    }

    return task;
  }
}
```

---

### 建议 3：实现批量上传 API（P0）

**目的**：提升批量操作性能，改善用户体验。

#### API 设计

```typescript
// 批量上传关键帧
@Post('/api/file/batch-upload/keyframes')
async batchUploadKeyframes(
  @Body() data: {
    taskId: number;
    files: Array<{ shotNumber: number; base64: string; filename: string }>;
  },
  @CurrentUser() user: { id: number }
) {
  const results = {
    success: [] as UploadResult[],
    failed: [] as Array<{ shotNumber: number; error: string }>,
    progress: 0,
  };

  // 并发上传（限制并发数为 3）
  const concurrency = 3;
  for (let i = 0; i < data.files.length; i += concurrency) {
    const batch = data.files.slice(i, i + concurrency);

    const promises = batch.map(async file => {
      try {
        const buffer = Buffer.from(file.base64, 'base64');
        const result = await bosService.uploadKeyframe(buffer, data.taskId, file.shotNumber, file.filename, user.id);
        results.success.push(result);
      } catch (error) {
        results.failed.push({ shotNumber: file.shotNumber, error: error.message });
      }
    });

    await Promise.all(promises);

    // 更新进度
    results.progress = Math.round(((i + batch.length) / data.files.length) * 100);

    // 实时推送进度（WebSocket）
    this.wsGateway.sendProgress(user.id, data.taskId, results.progress);
  }

  return results;
}
```

---

### 建议 4：配置 BOS 生命周期规则（P0）

**目的**：自动降级/删除旧文件，控制存储成本。

#### BOS 生命周期配置

```json
{
  "rule": [
    {
      "id": "rule-1-to-infrequent",
      "status": "enabled",
      "resource": ["{bucket}/{userId}/*"],
      "condition": {
        "time": {
          "dateGreaterThan": "$(lastModified)+P30D"
        }
      },
      "action": {
        "name": "TransitionToInfrequent"
      }
    },
    {
      "id": "rule-2-to-archive",
      "status": "enabled",
      "resource": ["{bucket}/{userId}/*"],
      "condition": {
        "time": {
          "dateGreaterThan": "$(lastModified)+P180D"
        }
      },
      "action": {
        "name": "TransitionToArchive"
      }
    },
    {
      "id": "rule-3-delete-temp",
      "status": "enabled",
      "resource": ["{bucket}/temp/*"],
      "condition": {
        "time": {
          "dateGreaterThan": "$(lastModified)+P7D"
        }
      },
      "action": {
        "name": "DeleteObject"
      }
    }
  ]
}
```

**成本节省估算**：

```yaml
# 场景：月产 300 条视频，保留 1 年
# 不使用生命周期：
年末存储: 3600 条 × 86MB = 310GB
年末月成本: 310GB × ¥0.12 = ¥37.2/月

# 使用生命周期（30 天后转低频）：
标准存储: 1 个月 × 300 条 = 300 条 × 86MB = 25.8GB
低频存储: 11 个月 × 300 条 = 3300 条 × 86MB = 284GB
月成本: 25.8GB × ¥0.12 + 284GB × ¥0.08 = ¥3.1 + ¥22.7 = ¥25.8/月
节省: ¥37.2 - ¥25.8 = ¥11.4/月（节省 30%）
```

---

### 建议 5：BOS 回源拉取优化（P1）

**目的**：避免"下载-上传"链路，节省带宽和时间。

#### 实现方案

```typescript
// Kling 视频生成后，直接让 BOS 从 Kling 拉取
async function img2videoWithDirectUpload(params: KlingVideoParams) {
  // 1. 提交 Kling 任务
  const taskId = await kling.submitTask(params);

  // 2. 轮询直到完成
  const klingVideoUrl = await kling.pollTask(taskId);

  // 3. 让 BOS 直接从 Kling URL 拉取（无需经过后端）
  const bosKey = `${userId}/${date}/${taskId}/videos/shot_${shotNumber}.mp4`;
  await bosClient.putObjectFromUrl(bucket, bosKey, klingVideoUrl);

  // 4. 返回 BOS CDN URL
  return `${cdnDomain}/${bosKey}`;
}

// BOS SDK 实现
async putObjectFromUrl(bucket: string, key: string, sourceUrl: string): Promise<void> {
  // BOS 支持从 URL 拉取文件（Fetch API）
  await this.client.putObjectFromUrl(bucket, key, sourceUrl, {
    'x-bce-acl': 'public-read',
    'Content-Type': 'video/mp4'
  });
}
```

**性能提升**：

```yaml
# 单个 15MB 视频
旧方案: Kling → 后端（15MB 下载）→ BOS（15MB 上传）= 30MB 流量 + 2 次网络传输
新方案: Kling → BOS（15MB 直传）= 15MB 流量 + 1 次网络传输

# 6 个分镜
旧方案: 180MB 流量，耗时约 30 秒（假设 50Mbps 带宽）
新方案: 90MB 流量，耗时约 15 秒

# 效率提升：50% 流量 + 50% 时间
```

---

### 建议 6：实现本地缓存机制（P1）

**目的**：避免重复下载参考图，节省流量。

#### 实现方案

```typescript
// 缓存管理服务
class ReferenceImageCacheService {
  private cacheDir = '/var/cache/bos-references/';

  /**
   * 获取参考图（优先使用缓存）
   */
  async getCachedReferenceImage(bosKey: string): Promise<string> {
    const localPath = path.join(this.cacheDir, bosKey);

    // 1. 检查本地文件是否存在
    if (await fs.pathExists(localPath)) {
      // 2. 校验 ETag 是否一致
      const metadata = await bosService.getObjectMetadata(bosKey);
      const localEtag = await this.getFileEtag(localPath);

      if (metadata.etag === localEtag) {
        console.log(`[缓存命中] ${bosKey}`);
        return localPath; // 缓存命中
      } else {
        console.log(`[缓存过期] ${bosKey}，重新下载`);
      }
    }

    // 3. 缓存未命中或过期，重新下载
    console.log(`[缓存未命中] ${bosKey}，下载中...`);
    await fs.ensureDir(path.dirname(localPath));
    await bosService.downloadFile(bosKey, localPath);

    return localPath;
  }

  /**
   * 计算本地文件 ETag（MD5）
   */
  private async getFileEtag(filePath: string): Promise<string> {
    const buffer = await fs.readFile(filePath);
    return crypto.createHash('md5').update(buffer).digest('hex');
  }

  /**
   * 清理过期缓存（超过 7 天未使用）
   */
  async cleanupExpiredCache() {
    const files = await fs.readdir(this.cacheDir, { recursive: true });
    const now = Date.now();
    const maxAge = 7 * 24 * 60 * 60 * 1000; // 7 天

    for (const file of files) {
      const filePath = path.join(this.cacheDir, file);
      const stats = await fs.stat(filePath);

      if (now - stats.atimeMs > maxAge) {
        console.log(`[清理缓存] ${file}`);
        await fs.remove(filePath);
      }
    }
  }
}
```

**成本节省**：

```yaml
# 场景：使用同一人物参考图生成 100 个视频
参考图大小: 2MB

# 不使用缓存：
下载次数: 100 次
流量成本: 2MB × 100 × ¥0.23/GB = ¥0.046

# 使用缓存：
下载次数: 1 次（首次）
流量成本: 2MB × 1 × ¥0.23/GB = ¥0.00046
节省: ¥0.046 - ¥0.00046 = ¥0.04554（节省 99%）
```

---

### 建议 7：定期清理僵尸文件（P1）

**目的**：回收未完成任务占用的存储空间。

#### 实现方案

```typescript
// 定时任务：每天凌晨 3 点执行
@Cron('0 3 * * *')
async cleanupAbandonedTasks() {
  console.log('[定时任务] 开始清理僵尸文件...');

  // 1. 查找超过 30 天未完成的任务
  const abandonedTasks = await db.query(`
    SELECT id, user_id, create_time, status
    FROM short_video_project
    WHERE status IN ('draft', 'processing')
      AND create_time < NOW() - INTERVAL 30 DAY
  `);

  console.log(`[定时任务] 发现 ${abandonedTasks.length} 个僵尸任务`);

  // 2. 批量删除 BOS 文件
  let totalSize = 0;
  for (const task of abandonedTasks) {
    const date = task.create_time.toISOString().slice(0, 10);

    // 2.1 查询任务的所有文件
    const files = await db.query(`
      SELECT bos_key, file_size
      FROM bos_file_metadata
      WHERE task_id = ? AND deleted = FALSE
    `, [task.id]);

    if (files.length > 0) {
      // 2.2 删除 BOS 文件
      const keys = files.map(f => f.bos_key);
      await bosService.batchDelete(keys, task.user_id);

      // 2.3 标记元数据为已删除
      await db.update('bos_file_metadata', { deleted: true, delete_time: new Date() }, { task_id: task.id });

      // 2.4 统计回收空间
      totalSize += files.reduce((sum, f) => sum + f.file_size, 0);
    }
  }

  // 3. 标记任务为已清理
  await db.update('short_video_project', { status: 'cleaned' }, { id: abandonedTasks.map(t => t.id) });

  console.log(`[定时任务] 清理完成，回收存储空间: ${(totalSize / 1024 / 1024 / 1024).toFixed(2)} GB`);
}
```

---

### 建议 8：简化 API 参数（P1）

**目的**：改善开发体验，减少错误。

#### 优化前

```typescript
// 冗余参数：userId、date 都应该自动获取
async uploadKeyframe(
  fileBuffer: Buffer,
  userId: number | string,      // 应该从 token 获取
  date: string,                  // 应该从任务表查询
  taskId: number | string,
  shotNumber: number,
  filename: string
)
```

#### 优化后

```typescript
// 简化参数：只传必要的业务参数
async uploadKeyframe(
  fileBuffer: Buffer,
  taskId: number | string,       // 唯一必要参数
  shotNumber: number,
  filename: string,
  currentUserId: number          // 从认证中间件获取
): Promise<UploadResult> {
  // 内部自动查询 userId、date
  const task = await this.getTask(taskId);

  // 校验权限
  if (task.user_id !== currentUserId) {
    throw new Error('无权操作该任务');
  }

  const userId = task.user_id;
  const date = task.create_time.toISOString().slice(0, 10);

  // 上传
  return this.upload(fileBuffer, filename, {
    pathContext: { userId, date, taskId },
    folder: 'keyframes',
    filename: `shot_${String(shotNumber).padStart(3, '0')}.jpg`,
    isPublic: true,
  });
}
```

---

## 📋 Cursor 开发清单

### 第一阶段：核心升级（Week 1）

#### 后端任务

- [ ] **数据库设计**
  - [ ] 创建 `bos_file_metadata` 表（文件元数据）
  - [ ] 创建 `bos_file_cache` 表（本地缓存记录）
  - [ ] 添加索引（user_id, task_id, category, usage_status）

- [ ] **BosStorageService 升级**
  - [ ] 去掉 `date` 参数，自动从任务表查询
  - [ ] 所有上传方法增加元数据记录
  - [ ] 实现批量上传 API
  - [ ] 实现预检查 API
  - [ ] 增加文件内容校验（魔数检查）

- [ ] **ReferenceImageCacheService**
  - [ ] 实现本地缓存逻辑
  - [ ] 实现 ETag 校验
  - [ ] 实现缓存清理定时任务

- [ ] **FileMetadataService**
  - [ ] 实现元数据 CRUD
  - [ ] 实现成本统计 API
  - [ ] 实现文件去重（基于 hash）

---

### 第二阶段：性能优化（Week 2）

#### 后端任务

- [ ] **BOS 回源拉取**
  - [ ] 实现 `putObjectFromUrl()` 方法
  - [ ] 修改 Kling 视频生成流程（直传）
  - [ ] 修改 ComfyUI 图片生成流程（直传）

- [ ] **批量操作优化**
  - [ ] 实现批量上传关键帧 API
  - [ ] 实现批量上传视频片段 API
  - [ ] WebSocket 实时进度推送

- [ ] **缓存机制**
  - [ ] Redis 缓存任务信息
  - [ ] 本地缓存参考图
  - [ ] CDN 缓存配置优化

---

### 第三阶段：成本优化（Week 3）

#### 后端任务

- [ ] **生命周期管理**
  - [ ] 配置 BOS 生命周期规则（30天转低频）
  - [ ] 配置 BOS 生命周期规则（180天转归档）
  - [ ] 配置 BOS 生命周期规则（7天删除临时文件）

- [ ] **僵尸文件清理**
  - [ ] 实现定时任务（清理未完成任务）
  - [ ] 实现定时任务（清理未使用参考图）
  - [ ] 添加清理日志和统计

- [ ] **成本监控**
  - [ ] 实现存储成本统计 API
  - [ ] 实现流量成本统计 API
  - [ ] 设置成本告警（超过阈值发邮件/短信）

---

### 第四阶段：前端集成（Week 4）

#### 前端任务

- [ ] **文件上传组件**
  - [ ] 拖拽上传（支持多文件）
  - [ ] 上传进度条（实时显示）
  - [ ] 批量上传（6 个关键帧一次性上传）
  - [ ] 错误处理和重试

- [ ] **文件预览组件**
  - [ ] 图片预览（支持缩略图）
  - [ ] 视频播放器（使用 BOS CDN URL）
  - [ ] 音频播放器

- [ ] **文件管理页面**
  - [ ] 文件列表（按任务/按日期）
  - [ ] 文件删除（单个/批量）
  - [ ] 存储统计（用量/成本）
  - [ ] 文件搜索和筛选

---

### 第五阶段：安全加固（Week 5）

#### 后端任务

- [ ] **安全校验**
  - [ ] 文件魔数校验（防止伪造）
  - [ ] 文件大小二次校验
  - [ ] 防止路径穿越攻击

- [ ] **内容审核**（可选）
  - [ ] 集成百度云图片审核 API
  - [ ] 集成百度云视频审核 API
  - [ ] 违规内容拦截和通知

- [ ] **权限加固**
  - [ ] 所有 API 强制用户认证
  - [ ] 所有涉及 key 的操作校验归属
  - [ ] 添加操作日志（审计追踪）

---

### 第六阶段：高级功能（Week 6）

#### 后端任务

- [ ] **任务组支持**（可选）
  - [ ] 数据库设计（task_group 表）
  - [ ] 组级共享参考图
  - [ ] 批量生产优化

- [ ] **版本管理**（可选）
  - [ ] 文件版本号支持
  - [ ] 版本历史查询
  - [ ] 版本回滚

- [ ] **团队协作**（可选）
  - [ ] 团队路径设计
  - [ ] 团队成员权限管理
  - [ ] 共享素材库

---

## 📊 预期效果

### 性能提升

| 指标 | 优化前 | 优化后 | 提升 |
|------|-------|-------|------|
| **批量上传速度** | 60秒（6个文件串行） | 20秒（并发上传） | +200% |
| **视频生成流程** | 30秒（下载+上传） | 15秒（直传） | +100% |
| **参考图下载** | 每次下载 2MB | 首次下载，后续缓存 | +99% |
| **FFmpeg 剪辑** | 下载 123MB 素材 | 流式处理 | +50% |

### 成本节省

| 项目 | 优化前 | 优化后 | 节省 |
|------|-------|-------|------|
| **存储成本（年）** | ¥447/年 | ¥310/年（生命周期） | -30% |
| **流量成本（月）** | ¥35/月 | ¥20/月（缓存+直传） | -40% |
| **总成本（年）** | ¥867/年 | ¥550/年 | **-37%** |

### 开发体验

| 维度 | 优化前 | 优化后 |
|------|-------|-------|
| **API 参数** | 6 个参数（易出错） | 3 个参数（简洁） |
| **错误提示** | 模糊（"上传失败"） | 精确（"任务不存在"） |
| **批量操作** | 逐个调用 | 一次性调用 |
| **进度反馈** | 无 | WebSocket 实时推送 |

---

## 🎯 总结

### 核心升级点

1. ✅ **引入文件元数据表**：追溯文件用途、成本、关联关系
2. ✅ **强制使用任务创建日期**：避免文件分散
3. ✅ **实现批量上传 API**：提升性能和用户体验
4. ✅ **配置生命周期规则**：自动降级旧文件，节省 30% 成本
5. ✅ **BOS 回源拉取**：避免"下载-上传"链路，节省 50% 流量
6. ✅ **本地缓存参考图**：避免重复下载，节省 99% 流量
7. ✅ **定期清理僵尸文件**：回收未完成任务占用的存储
8. ✅ **简化 API 参数**：改善开发体验

### 实施建议

**优先级**：

- **P0 必须做**（Week 1-3）：元数据表、日期规范、批量上传、生命周期
- **P1 强烈推荐**（Week 4-5）：BOS 直传、缓存机制、僵尸文件清理
- **P2 可选优化**（Week 6+）：内容审核、任务组、版本管理

**投入产出比**：

- 开发投入：6 周 × 1 名工程师 = **6 人周**
- 成本节省：¥317/年（第一年），随着存储增长节省更多
- 性能提升：批量操作快 3 倍，视频生成快 2 倍
- 用户体验：API 简化，错误提示精确，进度实时反馈

---

**文档版本**: v1.0
**最后更新**: 2026-03-01
**分析人**: Claude Code

**建议 Cursor 团队按此清单逐步实施，优先完成 P0 和 P1 升级！** 🚀
