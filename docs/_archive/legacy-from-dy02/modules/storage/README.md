# 存储模块（storage）

## 模块概述

文件存储管理，支持本地存储和百度 BOS 云存储。提供分片上传功能，适用于大文件上传场景。

## 后端结构

```
module/storage/
├── config/
│   ├── BosCleanupScheduler.java              # BOS 文件清理
│   └── UploadTaskCleanupScheduler.java       # 上传任务清理
│
├── controller/
│   ├── StorageController.java                # 文件管理
│   └── UploadController.java                 # 分片上传
│
├── entity/
│   ├── SysFile.java                          # 文件记录（sys_file）
│   ├── SysUploadTask.java                    # 上传任务
│   ├── SysUploadChunk.java                   # 上传分片
│   ├── BosFileMetadata.java                  # BOS 文件元数据
│   ├── UploadStatus.java                     # 上传状态枚举
│   └── ChunkStatus.java                      # 分片状态枚举
│
├── service/
│   ├── BosStorageService / Impl              # BOS 存储
│   ├── UploadService / Impl                  # 分片上传
│   ├── BosCleanupServiceImpl.java            # 清理
│   └── BosReferenceImageCacheServiceImpl     # 参考图缓存
│
└── vo/
    ├── UploadInitVO / UploadInitResultVO      # 初始化上传
    ├── UploadChunkVO                          # 上传分片
    ├── UploadCompleteVO / ResultVO            # 完成上传
    └── UploadProgressVO                       # 上传进度
```

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 文件管理 | `pages/StoragePage.tsx` | `/admin/storage` |

## 前端 API

文件：`api/storage.ts`、`api/upload.ts`

## 前端 Hooks

| Hook | 说明 |
|------|------|
| useChunkedUpload | 分片上传封装 |

## 分片上传流程

```
1. initUpload()   → 获取 uploadId + 预计分片数
2. uploadChunk()  → 逐片上传（支持断点续传）
3. completeUpload() → 合并分片 → 返回文件 URL
```

SQL 文件：`sql/storage/`
