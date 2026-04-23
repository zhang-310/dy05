> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# storage 模块设计文档

> 版本：1.1 | 更新日期：2026-02-24 | 阶段：P0

---

## 一、需求分析

### 1.1 模块定位

storage 模块提供统一的文件上传和管理服务。支持多种对象存储后端（百度 BOS / 阿里 OSS），为头像、产品图片、视频封面、AI 生成的关键帧图片、视频片段、配音音频、剪辑成片等文件上传提供标准接口。

### 1.2 功能清单

```
storage 模块
├── 文件上传
│   ├── 单文件上传
│   ├── 多文件上传
│   ├── 文件类型校验（白名单）
│   ├── 文件大小限制
│   └── 自动生成文件名（避免重复）
│
├── 文件管理
│   ├── 文件列表（按用户/模块/类型筛选）
│   ├── 文件删除
│   └── 文件访问 URL 生成
│
└── 存储后端
    ├── 百度 BOS 对接
    ├── 阿里 OSS 对接
    └── 本地存储（开发环境）
```

### 1.3 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 文件类型白名单 | 只允许上传图片（jpg/png/gif/webp）、视频（mp4/avi）、音频（mp3/wav），可配置。上传时从 sys_config 读取 `storage.allowed.types` 做校验 |
| BR-02 | 文件大小限制 | 图片 ≤ 10MB，视频 ≤ 200MB，音频 ≤ 50MB（从 sys_config 读取 `storage.max.image.size.mb` 等配置项）。超限返回 STORAGE_FILE_TOO_LARGE(3705) |
| BR-03 | 文件名防重 | 上传后用 UUID + 原始扩展名生成存储路径 |
| BR-04 | 存储后端可切换 | 通过 sys_config `storage.provider` 切换 BOS/OSS/本地存储 |
| BR-05 | 数据隔离 | 文件关联上传用户（owner_id） |
| BR-06 | 文件类型校验前置 | 在 Controller 层做文件类型和大小校验，不合规的直接拒绝，不传入 Service 层 |

---

## 二、数据库设计

#### sys_file — 文件记录表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | | — | 上传用户 ID |
| original_name | VARCHAR(256) | NOT NULL | — | 原始文件名 |
| storage_name | VARCHAR(256) | NOT NULL | — | 存储文件名 |
| storage_path | VARCHAR(512) | NOT NULL | — | 存储路径 |
| file_url | VARCHAR(512) | NOT NULL | — | 访问 URL |
| file_type | VARCHAR(32) | | — | 文件类型（image/video/audio/document） |
| file_ext | VARCHAR(16) | | — | 扩展名（jpg/png/mp4/mp3/wav） |
| file_size | BIGINT | | 0 | 文件大小（字节） |
| module | VARCHAR(64) | | — | 所属模块（auth/douyin/live...） |
| provider | VARCHAR(32) | | 'local' | 存储提供商：local / bos / oss |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(owner_id, module, create_time DESC)`

---

## 三、接口设计

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 1 | POST | /api/v1/storage/upload | 登录 | 单文件上传 |
| 2 | POST | /api/v1/storage/upload-multi | 登录 | 多文件上传 |
| 3 | POST | /api/v1/storage/list | 登录 | 我的文件列表 |
| 4 | POST | /api/v1/storage/delete | 登录 | 删除文件 |
| 5 | POST | /api/v1/storage/admin/list | 管理员 | 全平台文件列表 |

### 上传接口详情

```
POST /api/v1/storage/upload
Content-Type: multipart/form-data
权限：登录
```

**请求参数：**
- file: 文件（multipart）
- module: 所属模块（可选，如 "douyin"）

**响应：**

```json
{
  "status": 200,
  "data": {
    "id": 1,
    "fileUrl": "https://xxx.bos.baidu.com/uploads/2026/02/24/uuid.jpg",
    "originalName": "avatar.jpg",
    "fileSize": 102400
  }
}
```

---

## 四、页面设计

storage 模块不需要独立页面，文件上传组件嵌入在各业务模块的编辑表单中（如头像上传、产品图上传）。

管理端可选提供一个文件管理页面：

| # | 页面 | 路径 | 说明 |
|---|------|------|------|
| 1 | 文件管理 | /admin/storage/file-list.html | 全平台文件列表 |

---

## 五、开发任务拆解

| # | 任务 | 依赖 |
|---|------|------|
| B1 | SQL schema.sql | 无 |
| B2 | Entity + Repository | B1 |
| B3 | StorageProvider 接口 + 本地/BOS/OSS 实现 | 无 |
| B4 | StorageService | B2, B3 |
| B5 | StorageController | B4 |
| F1 | 文件管理页（管理端，可选） | B5 |
