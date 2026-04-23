# 百度云对象存储（BOS）集成方案

**版本**: v1.2
**日期**: 2026-03-01
**状态**: 已对接，集成中

---

## 📋 核心原则：按用户 → 日期 → 任务存储

**存储路径规范**：`{user_id}/{date}/{task_id}/{文件类型}/`

- **用户隔离**：每个用户只能访问、管理自己路径下的素材，`owner_id` 强制校验，严禁跨用户操作
- **日期分区**：按日期分目录，便于按日/月清理、归档、成本统计
- **任务隔离**：同一任务的所有素材集中存储，便于批量删除、迁移

---

## 📋 目录

1. [集成概述](#集成概述)
2. [BOS 服务配置](#bos-服务配置)
3. [存储架构设计](#存储架构设计)
4. [API 封装](#api-封装)
5. [文件类型规范](#文件类型规范)
6. [CDN 加速](#cdn-加速)
7. [成本优化](#成本优化)
8. [安全策略](#安全策略)

---

## 🎯 集成概述

### 为什么选择百度云 BOS？

1. ✅ **稳定可靠**：99.999999999% 数据可靠性
2. ✅ **访问速度快**：国内 CDN 节点覆盖全国
3. ✅ **成本低**：存储 ¥0.12/GB/月，流量 ¥0.23/GB
4. ✅ **无缝集成**：后台 API 已对接完成
5. ✅ **多媒体支持**：原生支持图片处理、视频转码

### 核心用途

短视频自动化生产系统的所有媒体素材存储：

```
BOS 存储内容：
├── 人物参考图（Character Reference Images）
├── 场景参考图（Scene Reference Images）
├── 关键帧图片（Keyframe Images）
├── 视频片段（Video Clips）
├── 配音文件（Audio Files）
├── 成片视频（Final Videos）
├── 封面图片（Thumbnail Images）
└── 字幕文件（Subtitle Files）
```

---

## ⚙️ BOS 服务配置

### 1. Bucket 创建策略

```yaml
# 开发环境
Bucket 名称: dy01-dev-media
访问权限: 私有（Private）
存储类型: 标准存储
区域: 北京（bj）

# 生产环境
Bucket 名称: dy01-prod-media
访问权限: 公共读（Public Read）
存储类型: 标准存储
区域: 北京（bj）
CDN 加速: 开启
```

### 2. 目录结构设计（按用户 → 日期 → 任务）

```
dy01-prod-media/
├── {user_id}/                           # 用户层级（owner_id）
│   ├── references/                      # 用户级参考图库（跨任务复用）
│   │   ├── characters/                  # 人物参考图
│   │   │   └── {character_id}/
│   │   │       ├── main.jpg
│   │   │       └── variants/
│   │   └── scenes/                      # 场景参考图
│   │       └── {scene_id}/
│   │           ├── main.jpg
│   │           └── depth.png
│   │
│   └── {date}/                          # 日期层级（YYYY-MM-DD）
│       └── {task_id}/                   # 任务层级（project_id）

│           ├── keyframes/               # 关键帧图片
│           │   ├── shot_001.jpg
│           │   ├── shot_002.jpg
│           │   └── ...
│           ├── videos/                  # 视频片段
│           │   ├── shot_001.mp4
│           │   ├── shot_002.mp4
│           │   └── final.mp4            # 成片
│           ├── audios/                   # 音频文件
│           │   ├── voice_001.mp3
│           │   ├── bgm.mp3
│           │   └── final_mix.mp3
│           ├── subtitles/               # 字幕文件
│           │   └── final.srt
│           ├── thumbnails/              # 封面图片
│           │   ├── cover_1.jpg
│           │   └── ...
│           └── references/              # 任务级参考图（仅本任务使用）
│               ├── characters/
│               └── scenes/
│
└── temp/                                # 临时文件（定期清理）
    └── {user_id}/{date}/
        └── ...
```

**路径示例**：

```
# 用户 1001，2026-03-01，任务 5001 的关键帧
1001/2026-03-01/5001/keyframes/shot_001.jpg

# 用户 1001，2026-03-01，任务 5001 的成片
1001/2026-03-01/5001/videos/final.mp4

# 用户 1001 的人物参考图（跨任务复用）
1001/references/characters/char_001/main.jpg

# CDN 完整 URL
https://cdn.yourdomain.com/1001/2026-03-01/5001/keyframes/shot_001.jpg
```

### 3. 环境变量配置

```bash
# .env
# 百度云 BOS 配置
BOS_ACCESS_KEY_ID=your_access_key_id
BOS_SECRET_ACCESS_KEY=your_secret_access_key
BOS_ENDPOINT=https://bj.bcebos.com
BOS_BUCKET_NAME=dy01-prod-media
BOS_CDN_DOMAIN=https://cdn.yourdomain.com  # 自定义 CDN 域名

# 文件上传配置
MAX_FILE_SIZE_IMAGE=10485760        # 10MB
MAX_FILE_SIZE_VIDEO=524288000       # 500MB
MAX_FILE_SIZE_AUDIO=52428800        # 50MB
```

---

## 🏗️ 存储架构设计

### 系统架构图

```
┌──────────────────────────────────────────────────────┐
│                  前端应用                              │
│  - 文件选择/拖拽上传                                    │
│  - 进度条显示                                          │
│  - 预览/播放                                           │
└────────────────┬─────────────────────────────────────┘
                 │ HTTP Upload
                 ↓
┌──────────────────────────────────────────────────────┐
│              后端 API 服务                             │
│  - 文件验证（类型/大小）                                │
│  - 生成唯一文件名                                       │
│  - 调用 BOS Service                                   │
└────────────────┬─────────────────────────────────────┘
                 │ BOS SDK
                 ↓
┌──────────────────────────────────────────────────────┐
│           百度云对象存储 BOS                           │
│  - 文件持久化存储                                       │
│  - 图片处理（缩略图/水印）                              │
│  - 视频转码（可选）                                     │
└────────────────┬─────────────────────────────────────┘
                 │ CDN 分发
                 ↓
┌──────────────────────────────────────────────────────┐
│              百度云 CDN                                │
│  - 全国节点加速                                         │
│  - HTTPS 支持                                         │
│  - 防盗链                                              │
└──────────────────────────────────────────────────────┘
```

### 文件上传流程

```
1. 前端选择文件
   ↓
2. 前端校验（类型、大小）
   ↓
3. 发送 POST /upload API
   ↓
4. 后端二次校验
   ↓
5. 生成唯一文件名（UUID + 原始扩展名）
   ↓
6. 调用 BOS SDK 上传
   ↓
7. 返回 BOS URL（或 CDN URL）
   ↓
8. 前端保存 URL，后续使用
```

---

## 💻 API 封装

### 1. BOS Service 封装（后端）

```typescript
// backend/src/services/BosStorageService.ts

import { BosClient } from '@baiducloud/sdk';
import { v4 as uuidv4 } from 'uuid';
import * as path from 'path';

/** 存储路径：{userId}/{date}/{taskId}/... */
interface StoragePathContext {
  userId: number | string;      // 用户 ID（owner_id）
  date?: string;                // 日期 YYYY-MM-DD，任务相关文件必填
  taskId?: number | string;     // 任务/项目 ID，任务相关文件必填
}

interface UploadOptions {
  bucket?: string;
  folder?: string;
  filename?: string;
  contentType?: string;
  isPublic?: boolean;
  /** 按用户→日期→任务生成路径 */
  pathContext?: StoragePathContext;
}

interface UploadResult {
  url: string;          // BOS 原始 URL
  cdnUrl: string;       // CDN 加速 URL
  bucket: string;
  key: string;          // 对象 key（文件路径）
  size: number;         // 文件大小（字节）
  etag: string;         // ETag（用于校验）
}

export class BosStorageService {
  private client: BosClient;
  private bucketName: string;
  private cdnDomain: string;

  constructor() {
    this.client = new BosClient({
      credentials: {
        ak: process.env.BOS_ACCESS_KEY_ID,
        sk: process.env.BOS_SECRET_ACCESS_KEY,
      },
      endpoint: process.env.BOS_ENDPOINT || 'https://bj.bcebos.com',
    });
    this.bucketName = process.env.BOS_BUCKET_NAME || 'dy01-prod-media';
    this.cdnDomain = process.env.BOS_CDN_DOMAIN || '';
  }

  /**
   * 根据 pathContext 生成标准路径
   * - 有 date+taskId：{userId}/{date}/{taskId}/{subFolder}（任务级文件）
   * - 仅 userId：{userId}/{subFolder}（用户级文件，如参考图）
   */
  private buildPath(ctx: StoragePathContext, subFolder: string): string {
    const { userId, date, taskId } = ctx;
    if (date && taskId) {
      return `${userId}/${date}/${taskId}/${subFolder}`;
    }
    return `${userId}/${subFolder}`;
  }

  /**
   * 上传文件
   * @param options.pathContext 按用户→日期→任务生成路径，不传则使用 options.folder
   */
  async upload(
    fileBuffer: Buffer,
    originalFilename: string,
    options: UploadOptions = {}
  ): Promise<UploadResult> {
    const ext = path.extname(originalFilename);
    const filename = options.filename || `${uuidv4()}${ext}`;
    let folder = options.folder;
    if (options.pathContext) {
      folder = this.buildPath(options.pathContext, options.folder || 'temp');
    }
    folder = folder || 'temp';
    const key = `${folder}/${filename}`;

    // 上传到 BOS
    const result = await this.client.putObjectFromBlob(
      options.bucket || this.bucketName,
      key,
      fileBuffer,
      {
        'Content-Type': options.contentType || this.getContentType(ext),
        'x-bce-acl': options.isPublic ? 'public-read' : 'private',
      }
    );

    // 生成 URL
    const bosUrl = `${this.client.config.endpoint}/${this.bucketName}/${key}`;
    const cdnUrl = this.cdnDomain
      ? `${this.cdnDomain}/${key}`
      : bosUrl;

    return {
      url: bosUrl,
      cdnUrl: cdnUrl,
      bucket: this.bucketName,
      key: key,
      size: fileBuffer.length,
      etag: result.etag,
    };
  }

  /**
   * 上传人物参考图（用户级，跨任务复用）
   * 路径：{userId}/references/characters/{characterId}/
   */
  async uploadCharacterReference(
    fileBuffer: Buffer,
    userId: number | string,
    characterId: string,
    filename: string
  ): Promise<UploadResult> {
    return this.upload(fileBuffer, filename, {
      pathContext: { userId },
      folder: `references/characters/${characterId}`,
      isPublic: false,
    });
  }

  /**
   * 上传场景参考图（用户级）
   * 路径：{userId}/references/scenes/{sceneId}/
   */
  async uploadSceneReference(
    fileBuffer: Buffer,
    userId: number | string,
    sceneId: string,
    filename: string
  ): Promise<UploadResult> {
    return this.upload(fileBuffer, filename, {
      pathContext: { userId },
      folder: `references/scenes/${sceneId}`,
      isPublic: false,
    });
  }

  /**
   * 上传关键帧图片（按用户→日期→任务）
   * 路径：{userId}/{date}/{taskId}/keyframes/shot_001.jpg
   */
  async uploadKeyframe(
    fileBuffer: Buffer,
    userId: number | string,
    date: string,
    taskId: number | string,
    shotNumber: number,
    filename: string
  ): Promise<UploadResult> {
    const paddedShotNumber = String(shotNumber).padStart(3, '0');
    return this.upload(fileBuffer, filename, {
      pathContext: { userId, date, taskId },
      folder: 'keyframes',
      filename: `shot_${paddedShotNumber}.jpg`,
      isPublic: true,
    });
  }

  /**
   * 上传视频片段（按用户→日期→任务）
   * 路径：{userId}/{date}/{taskId}/videos/shot_001.mp4
   */
  async uploadVideoClip(
    fileBuffer: Buffer,
    userId: number | string,
    date: string,
    taskId: number | string,
    shotNumber: number,
    filename: string
  ): Promise<UploadResult> {
    const paddedShotNumber = String(shotNumber).padStart(3, '0');
    return this.upload(fileBuffer, filename, {
      pathContext: { userId, date, taskId },
      folder: 'videos',
      filename: `shot_${paddedShotNumber}.mp4`,
      contentType: 'video/mp4',
      isPublic: true,
    });
  }

  /**
   * 上传成片（按用户→日期→任务）
   * 路径：{userId}/{date}/{taskId}/videos/final.mp4
   */
  async uploadFinalVideo(
    fileBuffer: Buffer,
    userId: number | string,
    date: string,
    taskId: number | string,
    filename: string
  ): Promise<UploadResult> {
    return this.upload(fileBuffer, filename, {
      pathContext: { userId, date, taskId },
      folder: 'videos',
      filename: 'final.mp4',
      contentType: 'video/mp4',
      isPublic: true,
    });
  }

  /**
   * 上传配音文件（按用户→日期→任务）
   * 路径：{userId}/{date}/{taskId}/audios/
   */
  async uploadAudio(
    fileBuffer: Buffer,
    userId: number | string,
    date: string,
    taskId: number | string,
    filename: string
  ): Promise<UploadResult> {
    return this.upload(fileBuffer, filename, {
      pathContext: { userId, date, taskId },
      folder: 'audios',
      contentType: 'audio/mpeg',
      isPublic: false,
    });
  }

  /**
   * 上传封面图片（按用户→日期→任务）
   * 路径：{userId}/{date}/{taskId}/thumbnails/cover_1.jpg
   */
  async uploadThumbnail(
    fileBuffer: Buffer,
    userId: number | string,
    date: string,
    taskId: number | string,
    index: number,
    filename: string
  ): Promise<UploadResult> {
    return this.upload(fileBuffer, filename, {
      pathContext: { userId, date, taskId },
      folder: 'thumbnails',
      filename: `cover_${index}.jpg`,
      isPublic: true,
    });
  }

  /**
   * 获取任务根路径（用于批量删除）
   * 路径：{userId}/{date}/{taskId}/
   */
  getTaskRootKey(userId: number | string, date: string, taskId: number | string): string {
    return `${userId}/${date}/${taskId}/`;
  }

  /**
   * 路径归属校验：key 必须严格以 {userId}/ 开头，用户只能管理自己路径下的素材
   */
  private ensureKeyBelongsToUser(key: string, currentUserId: number | string): void {
    const prefix = `${currentUserId}/`;
    if (!key.startsWith(prefix)) {
      throw new Error('无权操作该路径下的文件');
    }
    if (key.includes('..')) {
      throw new Error('非法路径');
    }
  }

  /**
   * 删除文件（必须校验 key 归属当前用户）
   */
  async delete(key: string, currentUserId: number | string, bucket?: string): Promise<void> {
    this.ensureKeyBelongsToUser(key, currentUserId);
    await this.client.deleteObject(bucket || this.bucketName, key);
  }

  /**
   * 批量删除文件（每个 key 都需校验归属）
   */
  async batchDelete(keys: string[], currentUserId: number | string, bucket?: string): Promise<void> {
    keys.forEach(k => this.ensureKeyBelongsToUser(k, currentUserId));
    const objects = keys.map(key => ({ key }));
    await this.client.deleteMultipleObjects(bucket || this.bucketName, objects);
  }

  /**
   * 删除整个任务目录（按用户→日期→任务）
   * 必须校验 userId === currentUserId，用户只能删除自己的任务素材
   */
  async deleteTaskFiles(
    userId: number | string,
    date: string,
    taskId: number | string,
    currentUserId: number | string
  ): Promise<void> {
    if (String(userId) !== String(currentUserId)) {
      throw new Error('无权删除该任务的文件');
    }
    const prefix = this.getTaskRootKey(userId, date, taskId);
    const objects = await this.client.listObjects(this.bucketName, { prefix });
    const keys = (objects.contents || []).map(obj => obj.key).filter(Boolean) as string[];
    if (keys.length > 0) {
      await this.batchDelete(keys, currentUserId);
    }
  }

  /**
   * 列出用户路径下的文件（prefix 强制为 currentUserId/，禁止跨用户列表）
   */
  async listUserFiles(currentUserId: number | string, prefixSuffix: string = ''): Promise<string[]> {
    const prefix = prefixSuffix
      ? `${currentUserId}/${prefixSuffix}`.replace(/\/+$/, '') + '/'
      : `${currentUserId}/`;
    const objects = await this.client.listObjects(this.bucketName, { prefix });
    return (objects.contents || []).map(obj => obj.key).filter(Boolean) as string[];
  }

  /**
   * 生成临时访问 URL（私有文件，必须校验 key 归属）
   */
  generatePresignedUrl(key: string, currentUserId: number | string, expirationInSeconds: number = 3600): string {
    this.ensureKeyBelongsToUser(key, currentUserId);
    return this.client.generatePresignedUrl(this.bucketName, key, {
      expirationInSeconds: expirationInSeconds,
    });
  }

  /**
   * 获取文件 Content-Type
   */
  private getContentType(ext: string): string {
    const contentTypeMap: Record<string, string> = {
      '.jpg': 'image/jpeg',
      '.jpeg': 'image/jpeg',
      '.png': 'image/png',
      '.gif': 'image/gif',
      '.webp': 'image/webp',
      '.mp4': 'video/mp4',
      '.mp3': 'audio/mpeg',
      '.wav': 'audio/wav',
      '.srt': 'text/plain',
      '.json': 'application/json',
    };
    return contentTypeMap[ext.toLowerCase()] || 'application/octet-stream';
  }

  /**
   * 获取文件信息
   */
  async getObjectMetadata(key: string, bucket?: string) {
    return await this.client.getObjectMetadata(bucket || this.bucketName, key);
  }

  /**
   * 图片处理（生成缩略图）
   */
  generateThumbnailUrl(imageUrl: string, width: number, height: number): string {
    // BOS 图片处理参数
    // https://cloud.baidu.com/doc/BOS/s/Rjwvys0fu
    return `${imageUrl}@w_${width},h_${height},m_lfit`;
  }

  /**
   * 图片处理（添加水印）
   */
  generateWatermarkUrl(imageUrl: string, watermarkText: string): string {
    // Base64 编码水印文本
    const encoded = Buffer.from(watermarkText).toString('base64');
    return `${imageUrl}@wm_2,t_${encoded},g_se,x_10,y_10`;
  }
}
```

---

### 2. 文件上传 API（Controller）

```typescript
// backend/src/controllers/FileUploadController.ts

import { Controller, Post, UseInterceptors, UploadedFile, Body } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { BosStorageService } from '../services/BosStorageService';

@Controller('api/file')
export class FileUploadController {
  constructor(private readonly bosService: BosStorageService) {}

  /**
   * 上传人物参考图（用户级）
   * 路径：{userId}/references/characters/{characterId}/
   */
  @Post('upload/character-reference')
  @UseInterceptors(FileInterceptor('file'))
  async uploadCharacterReference(
    @UploadedFile() file: Express.Multer.File,
    @Body('characterId') characterId: string,
    @CurrentUser() user: { id: number }  // 从认证上下文获取
  ) {
    this.validateImageFile(file);
    const result = await this.bosService.uploadCharacterReference(
      file.buffer,
      user.id,
      characterId,
      file.originalname
    );
    return { success: true, data: { url: result.cdnUrl, key: result.key, size: result.size } };
  }

  /**
   * 上传场景参考图（用户级）
   */
  @Post('upload/scene-reference')
  @UseInterceptors(FileInterceptor('file'))
  async uploadSceneReference(
    @UploadedFile() file: Express.Multer.File,
    @Body('sceneId') sceneId: string,
    @CurrentUser() user: { id: number }
  ) {
    this.validateImageFile(file);
    const result = await this.bosService.uploadSceneReference(
      file.buffer,
      user.id,
      sceneId,
      file.originalname
    );
    return { success: true, data: { url: result.cdnUrl, key: result.key, size: result.size } };
  }

  /**
   * 上传关键帧图片（按用户→日期→任务）
   * 路径：{userId}/{date}/{taskId}/keyframes/shot_001.jpg
   * 日期：优先使用 task.createTime，否则用当前日期
   */
  @Post('upload/keyframe')
  @UseInterceptors(FileInterceptor('file'))
  async uploadKeyframe(
    @UploadedFile() file: Express.Multer.File,
    @Body('taskId') taskId: string,
    @Body('shotNumber') shotNumber: number,
    @Body('date') dateParam?: string,  // 可选，格式 YYYY-MM-DD，不传则用当天
    @CurrentUser() user: { id: number }
  ) {
    this.validateImageFile(file);
    const date = dateParam || new Date().toISOString().slice(0, 10);
    const result = await this.bosService.uploadKeyframe(
      file.buffer,
      user.id,
      date,
      taskId,
      shotNumber,
      file.originalname
    );
    return { success: true, data: { url: result.cdnUrl, key: result.key } };
  }

  /**
   * 上传视频片段（按用户→日期→任务）
   */
  @Post('upload/video-clip')
  @UseInterceptors(FileInterceptor('file'))
  async uploadVideoClip(
    @UploadedFile() file: Express.Multer.File,
    @Body('taskId') taskId: string,
    @Body('shotNumber') shotNumber: number,
    @Body('date') dateParam?: string,
    @CurrentUser() user: { id: number }
  ) {
    this.validateVideoFile(file);
    const date = dateParam || new Date().toISOString().slice(0, 10);
    const result = await this.bosService.uploadVideoClip(
      file.buffer,
      user.id,
      date,
      taskId,
      shotNumber,
      file.originalname
    );
    return { success: true, data: { url: result.cdnUrl, key: result.key } };
  }

  /**
   * 验证图片文件
   */
  private validateImageFile(file: Express.Multer.File) {
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const maxSize = 10 * 1024 * 1024; // 10MB

    if (!allowedTypes.includes(file.mimetype)) {
      throw new Error('不支持的图片格式，仅支持 JPG、PNG、WEBP');
    }

    if (file.size > maxSize) {
      throw new Error('图片文件过大，最大支持 10MB');
    }
  }

  /**
   * 验证视频文件
   */
  private validateVideoFile(file: Express.Multer.File) {
    const allowedTypes = ['video/mp4'];
    const maxSize = 500 * 1024 * 1024; // 500MB

    if (!allowedTypes.includes(file.mimetype)) {
      throw new Error('不支持的视频格式，仅支持 MP4');
    }

    if (file.size > maxSize) {
      throw new Error('视频文件过大，最大支持 500MB');
    }
  }
}
```

---

### 3. 前端集成示例（React）

```typescript
// frontend-react/src/services/fileUpload.ts

import request from '@/utils/request';

/**
 * 上传人物参考图
 */
export async function uploadCharacterReference(
  file: File,
  characterId: string
): Promise<{ url: string; key: string; size: number }> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('characterId', characterId);

  const response = await request.post<unknown, { data: { url: string; key: string; size: number } }>(
    '/api/file/upload/character-reference',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
}

/**
 * 上传场景参考图
 */
export async function uploadSceneReference(
  file: File,
  sceneId: string
): Promise<{ url: string; key: string; size: number }> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('sceneId', sceneId);

  const response = await request.post<unknown, { data: { url: string; key: string; size: number } }>(
    '/api/file/upload/scene-reference',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
}

/**
 * 上传关键帧图片（后端从 token 获取 userId，日期取当天，taskId 由前端传入）
 * 存储路径：{userId}/{date}/{taskId}/keyframes/shot_001.jpg
 */
export async function uploadKeyframe(
  file: File,
  taskId: string,
  shotNumber: number
): Promise<{ url: string; key: string }> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('taskId', taskId);
  formData.append('shotNumber', String(shotNumber));

  const response = await request.post<unknown, { data: { url: string; key: string } }>(
    '/api/file/upload/keyframe',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
}
```

---

## 📁 文件类型规范

### 1. 图片文件规范

| 类型 | 格式 | 分辨率推荐 | 文件大小限制 | 用途 |
|------|------|-----------|-------------|------|
| **人物参考图** | JPG/PNG | 512x512 ~ 1024x1024 | 10MB | IP-Adapter 人物一致性 |
| **场景参考图** | JPG/PNG | 1024x1024 | 10MB | ControlNet 场景控制 |
| **关键帧图片** | JPG | 1024x576 (16:9) | 5MB | 图生视频输入 |
| **封面图片** | JPG | 1080x1920 (9:16) | 2MB | 视频封面 |

### 2. 视频文件规范

| 类型 | 格式 | 分辨率 | 帧率 | 文件大小限制 | 用途 |
|------|------|--------|------|-------------|------|
| **视频片段** | MP4 (H.264) | 1080x1920 | 24/30 fps | 500MB | 分镜视频素材 |
| **成片** | MP4 (H.264) | 1080x1920 | 24/30 fps | 500MB | 最终发布视频 |

### 3. 音频文件规范

| 类型 | 格式 | 采样率 | 比特率 | 文件大小限制 | 用途 |
|------|------|--------|--------|-------------|------|
| **配音** | MP3 | 44.1kHz | 128kbps | 50MB | 语音合成输出 |
| **背景音乐** | MP3 | 44.1kHz | 128kbps | 50MB | BGM |

---

## 🚀 CDN 加速

### 1. CDN 配置

```yaml
# 百度云 CDN 配置
域名: cdn.yourdomain.com
源站类型: BOS（自动同步）
缓存配置:
  - 图片文件（jpg/png/webp）: 7 天
  - 视频文件（mp4）: 30 天
  - 音频文件（mp3）: 30 天
HTTPS: 开启（免费证书）
防盗链: 开启（Referer 白名单）
```

### 2. URL 使用规则（按用户→日期→任务）

```typescript
// 开发环境：使用 BOS 原始 URL
const imageUrl = 'https://bj.bcebos.com/dy01-dev-media/1001/2026-03-01/5001/keyframes/shot_001.jpg';

// 生产环境：使用 CDN URL（推荐）
const imageUrl = 'https://cdn.yourdomain.com/1001/2026-03-01/5001/keyframes/shot_001.jpg';

// 图片处理（缩略图）
const thumbnailUrl = 'https://cdn.yourdomain.com/1001/2026-03-01/5001/keyframes/shot_001.jpg@w_300,h_169';

// 日期说明：建议使用任务创建日期 task.createTime 或当前日期
// 格式：YYYY-MM-DD
```

---

## 💰 成本优化

### 1. 成本构成

```yaml
# 百度云 BOS 计费（北京区域）
存储费用:
  - 标准存储: ¥0.12/GB/月
  - 低频存储: ¥0.08/GB/月（超过 30 天未访问的文件）

流量费用:
  - CDN 回源流量: ¥0.23/GB
  - 直接下载流量: ¥0.50/GB（不推荐）

CDN 费用:
  - 国内流量: ¥0.23/GB
  - HTTPS 请求: ¥0.05/万次
```

### 2. 单条视频存储成本估算

```
单条 30 秒短视频存储：
├── 关键帧图片 × 6: 6MB
├── 视频片段 × 6: 60MB
├── 成片视频: 15MB
├── 配音文件 × 3: 3MB
├── 封面图片 × 3: 2MB
└── 总计: 约 86MB

月存储成本（单条）:
86MB × ¥0.12/GB = ¥0.01/月

月产 300 条视频:
300 × 86MB = 25.8GB
25.8GB × ¥0.12 = ¥3.1/月（存储）
```

### 3. 流量成本估算

```
单次视频播放（CDN）:
成片视频 15MB × ¥0.23/GB = ¥0.0035

月播放 10,000 次:
10,000 × ¥0.0035 = ¥35/月（流量）
```

### 4. 总成本估算

```
月产 300 条视频 + 月播放 10,000 次:
- 存储: ¥3.1/月
- 流量: ¥35/月
- 总计: ¥38/月

年成本: ¥456/年
```

### 5. 成本优化策略

```typescript
// 1. 定期清理临时文件（超过 7 天）
async function cleanupTempFiles() {
  const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
  // 列出 temp/ 目录下的文件
  // 删除超过 7 天的文件
}

// 2. 低频文件转低频存储（超过 30 天未访问）
async function moveToInfrequentStorage() {
  // 检查文件最后访问时间
  // 将超过 30 天未访问的文件转为低频存储
  // 节省 33% 存储成本
}

// 3. 启用 CDN 缓存（减少回源流量）
// 图片缓存 7 天
// 视频缓存 30 天

// 4. 压缩图片（减少存储和流量）
async function compressImage(imageBuffer: Buffer): Promise<Buffer> {
  // 使用 sharp 库压缩图片
  // JPG 质量 80%
  // PNG 转 WebP
}
```

---

## 🔒 安全策略

### 1. 用户路径隔离（强制）

**核心规则**：用户只能管理自己路径下的素材，所有涉及 key 的操作必须校验归属。

```typescript
/**
 * 路径归属校验：key 必须严格以 {userId}/ 开头
 * 用于：删除、批量删除、临时签名 URL、列表、下载 等所有接收 key 的接口
 */
function ensureKeyBelongsToUser(key: string, currentUserId: number | string): void {
  const prefix = `${currentUserId}/`;
  if (!key.startsWith(prefix)) {
    throw new Error('无权操作该路径下的文件');
  }
  // 防止路径穿越：禁止 ../ 等
  if (key.includes('..')) {
    throw new Error('非法路径');
  }
}

// 使用示例：删除文件前校验
async delete(key: string, currentUserId: number | string): Promise<void> {
  ensureKeyBelongsToUser(key, currentUserId);
  await this.client.deleteObject(this.bucketName, key);
}

// 批量删除：每个 key 都需校验
async batchDelete(keys: string[], currentUserId: number | string): Promise<void> {
  keys.forEach(k => ensureKeyBelongsToUser(k, currentUserId));
  const objects = keys.map(key => ({ key }));
  await this.client.deleteMultipleObjects(this.bucketName, objects);
}

// 临时签名 URL：仅允许访问自己的文件
generatePresignedUrl(key: string, currentUserId: number | string, expirationInSeconds = 3600): string {
  ensureKeyBelongsToUser(key, currentUserId);
  return this.client.generatePresignedUrl(this.bucketName, key, { expirationInSeconds });
}

// 删除任务文件：校验 userId 与当前用户一致
async deleteTaskFiles(userId: number | string, date: string, taskId: number | string, currentUserId: number | string): Promise<void> {
  if (String(userId) !== String(currentUserId)) {
    throw new Error('无权删除该任务的文件');
  }
  const prefix = this.getTaskRootKey(userId, date, taskId);
  // ... listObjects + batchDelete
}
```

**业务规则**：

| 操作 | 校验规则 |
|------|---------|
| 上传 | 路径由后端生成，强制使用 `user.id`，不信任前端传入的路径 |
| 删除 | key 必须以 `{currentUserId}/` 开头 |
| 批量删除 | 每个 key 都必须通过归属校验 |
| 临时 URL | key 必须以 `{currentUserId}/` 开头 |
| 列表 | listObjects 的 prefix 必须为 `{currentUserId}/` 或 `{currentUserId}/...` |
| 任务删除 | 传入的 userId 必须等于 currentUserId |

### 2. 访问控制（私有/公共、防盗链）

```typescript
// 1. 私有文件使用临时签名 URL（必须校验 key 归属）
const privateUrl = bosService.generatePresignedUrl(
  '1001/references/characters/123/main.jpg',
  currentUserId,
  3600
);

// 2. 公共文件使用 CDN URL（仅限本用户路径下的文件可公开）
const publicUrl = 'https://cdn.yourdomain.com/1001/2026-03-01/5001/videos/final.mp4';

// 3. 防盗链（Referer 白名单）
// 在 BOS 控制台配置允许的域名
```

```typescript
// 1. 上传前校验文件类型和大小
function validateFile(file: File): boolean {
  const allowedTypes = ['image/jpeg', 'image/png', 'video/mp4'];
  const maxSize = 500 * 1024 * 1024; // 500MB

  return allowedTypes.includes(file.type) && file.size <= maxSize;
}

// 2. 上传后校验 ETag
async function verifyUpload(key: string, expectedEtag: string): Promise<boolean> {
  const metadata = await bosService.getObjectMetadata(key);
  return metadata.etag === expectedEtag;
}
```

### 4. 权限管理（Bucket 策略）

```yaml
# BOS Bucket 策略（JSON）
{
  "accessControlList": [
    {
      "grantee": [{ "id": "user_id_1" }],
      "permission": ["READ", "WRITE"]
    },
    {
      "grantee": [{ "id": "*" }],
      "permission": ["READ"],
      "condition": {
        "referer": {
          "stringLike": ["https://yourdomain.com/*"]
        }
      }
    }
  ]
}
```

---

## 📊 监控与运维

### 1. 监控指标

```typescript
// 定期检查存储使用情况
async function getStorageStats() {
  // 总存储容量
  // 文件数量
  // 本月流量
  // 本月费用
}

// 告警规则
const alertRules = {
  storageUsage: '存储超过 100GB 时告警',
  monthlyTraffic: '月流量超过 1TB 时告警',
  monthlyCost: '月费用超过 ¥200 时告警',
};
```

### 2. 日志记录

```typescript
// 记录所有文件操作（key 含 userId/date/taskId，便于审计）
interface FileOperationLog {
  operation: 'upload' | 'delete' | 'download';
  key: string;           // 如 1001/2026-03-01/5001/keyframes/shot_001.jpg
  userId: string;
  date?: string;         // 从 key 解析
  taskId?: string;       // 从 key 解析
  timestamp: Date;
  fileSize?: number;
  result: 'success' | 'failed';
  error?: string;
}
```

### 3. 按用户/日期统计存储用量

```typescript
// 按用户统计：listObjects 前缀 userId/
// 按日期统计：listObjects 前缀 userId/2026-03-01/
// 按任务统计：listObjects 前缀 userId/2026-03-01/taskId/
async function getStorageUsageByUser(userId: string): Promise<number> {
  const objects = await bosClient.listObjects(bucket, { prefix: `${userId}/` });
  return (objects.contents || []).reduce((sum, obj) => sum + (obj.size || 0), 0);
}
```

---

## 🎯 集成清单

### 后端集成清单

- [x] BOS SDK 安装配置
- [x] BosStorageService 封装（含路径归属校验）
- [x] 用户路径隔离：ensureKeyBelongsToUser、deleteObject/listUserFiles/getPublicUrlForUser 强制校验
- [x] StorageController：上传路径由后端生成（userId/prefix），list/delete/getUrl 均校验归属
- [ ] 文件上传 API 测试
- [ ] 临时签名 URL 生成（私有 Bucket 场景，传入 key 时校验归属）
- [ ] 图片处理集成

### 前端集成清单

- [ ] 文件上传组件（支持拖拽）
- [ ] 上传进度条显示
- [ ] 图片预览组件
- [ ] 视频播放器集成
- [ ] CDN URL 替换
- [ ] 错误处理和重试

### 运维集成清单

- [ ] CDN 域名配置
- [ ] HTTPS 证书配置
- [ ] 防盗链配置
- [ ] 监控告警配置
- [ ] 定期清理脚本

---

## 🔗 参考文档

- [百度云 BOS 官方文档](https://cloud.baidu.com/doc/BOS/index.html)
- [百度云 BOS Node.js SDK](https://cloud.baidu.com/doc/BOS/s/4jwvyrq1t)
- [百度云 CDN 使用指南](https://cloud.baidu.com/doc/CDN/index.html)
- [百度云图片处理](https://cloud.baidu.com/doc/BOS/s/Rjwvys0fu)

---

---

## 📌 存储路径规范速查

| 文件类型 | 路径格式 | 示例 |
|---------|---------|------|
| 人物参考图 | `{userId}/references/characters/{characterId}/` | 1001/references/characters/char_001/main.jpg |
| 场景参考图 | `{userId}/references/scenes/{sceneId}/` | 1001/references/scenes/scene_001/main.jpg |
| 关键帧 | `{userId}/{date}/{taskId}/keyframes/` | 1001/2026-03-01/5001/keyframes/shot_001.jpg |
| 视频片段 | `{userId}/{date}/{taskId}/videos/` | 1001/2026-03-01/5001/videos/shot_001.mp4 |
| 成片 | `{userId}/{date}/{taskId}/videos/final.mp4` | 1001/2026-03-01/5001/videos/final.mp4 |
| 配音 | `{userId}/{date}/{taskId}/audios/` | 1001/2026-03-01/5001/audios/voice_001.mp3 |
| 封面 | `{userId}/{date}/{taskId}/thumbnails/` | 1001/2026-03-01/5001/thumbnails/cover_1.jpg |

**日期格式**：`YYYY-MM-DD`，建议使用任务创建日期或当前日期。

---

**文档版本**: v1.2
**最后更新**: 2026-03-01
**维护人员**: Claude Code

**BOS 集成完成！存储按 用户→日期→任务 分层，用户只能管理自己路径下的素材。** 🚀
