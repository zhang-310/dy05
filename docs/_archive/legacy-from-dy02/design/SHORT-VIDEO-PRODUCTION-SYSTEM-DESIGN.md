# 短视频自动化生产系统设计方案

**版本**: v1.0
**日期**: 2026-03-01
**状态**: 设计阶段

---

## 📋 目录

1. [系统概述](#系统概述)
2. [架构设计](#架构设计)
3. [AI 能力盘点](#ai-能力盘点)
4. [全链路流程](#全链路流程)
5. [核心功能模块](#核心功能模块)
6. [AI 接口规划](#ai-接口规划)
7. [数据库设计](#数据库设计)
8. [前端页面设计](#前端页面设计)
9. [实施计划](#实施计划)

---

## 🎯 系统概述

### 业务目标

构建一个**短视频全流程自动化生产系统**，支持：

1. ✅ **AI 爆款复刻**：分析爆款视频 → 自动生成类似内容
2. ✅ **热门视频二创**：提取热门元素 → 自动改编创新
3. ✅ **日常短视频策划**：自动生成脚本 → 一键成片
4. ✅ **软广视频策划**：植入广告 → 自然融合
5. ✅ **热门分析采集**：自动抓取 → 智能分析
6. ✅ **自动发布**：标题优化 → 定时发布

### 核心价值

- **效率提升**：10 倍效率（传统 1 天 → AI 1-2 小时）
- **成本降低**：节省 70% 人力成本
- **质量保证**：AI + 人工审核双重保障
- **规模化**：日产 10+ 条短视频

---

## 🏗️ 架构设计

### 系统分层

```
┌──────────────────────────────────────────────────────────┐
│              短视频运营模块（业务层）                        │
├──────────────────────────────────────────────────────────┤
│  1. 脚本策划        2. 分镜设计        3. 素材生产          │
│  4. 视频剪辑        5. 审核发布        6. 数据分析          │
└────────────────────┬─────────────────────────────────────┘
                     │ 调用 API
                     ↓
┌──────────────────────────────────────────────────────────┐
│              AI 能力模块（能力层）                          │
├──────────────────────────────────────────────────────────┤
│  ✅ 已有能力                    ❌ 待开发能力                │
│  - 文生图（text2img）            - 脚本生成（script-gen）   │
│  - 语音合成（TTS）               - 分镜生成（shot-list）    │
│  - 视频剪辑（trim/merge）        - 图生视频（img2video）    │
│  - 知识检索（KB search）         - 字幕生成（subtitle）     │
│  - 爆款分析（viral）             - 音视频混合（mix）        │
│                                 - 自动发布（publish）       │
└──────────────────────────────────────────────────────────┘
                     │ 调用第三方
                     ↓
┌──────────────────────────────────────────────────────────┐
│              第三方 AI 服务（基础层）                       │
├──────────────────────────────────────────────────────────┤
│  - DeepSeek（文案生成）         - MiniMax（图生视频）       │
│  - 通义千问（脚本优化）          - RunwayML（视频生成）     │
│  - 讯飞星火（语音合成）          - FFmpeg（视频处理）       │
│  - Stable Diffusion（图像生成） - OpenCV（图像处理）       │
└──────────────────────────────────────────────────────────┘
```

### 模块关系

```
短视频运营模块
├── 脚本策划服务
│   ├── 调用 AI: 知识检索、脚本生成
│   └── 调用 AI: 爆款分析、主题扩展
├── 分镜设计服务
│   ├── 调用 AI: 分镜生成
│   └── 调用 AI: 场景描述生成
├── 素材生产服务
│   ├── 调用 AI: 文生图（首尾帧）
│   ├── 调用 AI: 图生视频（素材）
│   └── 调用 AI: 语音合成（配音）
├── 视频剪辑服务
│   ├── 调用 AI: 视频裁剪、合并
│   ├── 调用 AI: 字幕生成
│   └── 调用 AI: 音视频混合
├── 审核发布服务
│   ├── 人工审核 + AI 辅助审核
│   └── 调用 AI: 标题生成、发布
└── 数据分析服务
    ├── 调用 AI: 热门采集
    └── 调用 AI: 爆款分析
```

---

## 📊 AI 能力盘点

### ✅ 已有能力（可直接使用）

| 能力 | API 端点 | 用途 | 状态 |
|------|---------|------|------|
| **文生图** | `/ai/media/image/text2img` | 生成首尾帧、封面 | ✅ 可用 |
| **语音合成** | `/ai/media/tts/generate` | 生成配音 | ✅ 可用 |
| **视频裁剪** | `/ai/media/video/trim` | 裁剪素材 | ✅ 可用 |
| **视频合并** | `/ai/media/video/merge` | 拼接多段视频 | ✅ 可用 |
| **视频转码** | `/ai/media/video/transcode` | 格式转换 | ✅ 可用 |
| **知识检索** | `/ai/knowledge-base/{kbId}/search` | 脚本灵感、案例查询 | ✅ 可用 |
| **爆款分析** | `/ai/evolution/viral/list` | 分析热门视频 | ✅ 可用 |
| **视频对比** | `/ai/evolution/video/compare` | 对比多个视频 | ✅ 可用 |

### ❌ 待开发能力（需要新增）

| 能力 | 优先级 | 建议第三方 API | 用途 |
|------|-------|---------------|------|
| **脚本生成** | P0 | DeepSeek、通义千问 | 根据主题生成脚本 |
| **分镜生成** | P0 | 自研（基于 LLM） | 脚本 → 分镜列表 |
| **图生视频** | P0 | **MiniMax** / RunwayML | 图片 → 短视频素材 |
| **字幕生成** | P1 | 剪映 API、必剪 API | 自动生成字幕 |
| **音视频混合** | P1 | FFmpeg（自研） | 视频 + 音频 → 成片 |
| **标题生成** | P1 | DeepSeek | 生成吸引人的标题 |
| **自动发布** | P2 | 抖音开放平台 | 自动发布到平台 |
| **热门采集** | P2 | 抖音数据 API | 采集热门视频数据 |

---

## 🔄 全链路流程

### 方式 1：AI 爆款复刻

```
┌─────────────────────────────────────────────────────────────┐
│  1. 输入爆款视频 URL                                          │
│     ↓                                                        │
│  2. AI 分析（爆款拆解）                                       │
│     - 提取关键要素（标题、脚本、镜头、音乐）                    │
│     - 分析成功因素（数据、互动、情感）                         │
│     ↓                                                        │
│  3. AI 生成脚本（参考爆款，创新改编）                          │
│     - 调用知识库（类似案例）                                   │
│     - 调用脚本生成 API                                        │
│     ↓                                                        │
│  4. AI 生成分镜（脚本 → 分镜列表）                            │
│     - 分镜 1：开场（0-3s）                                    │
│     - 分镜 2：冲突（3-10s）                                   │
│     - 分镜 3：解决（10-20s）                                  │
│     - 分镜 4：结尾（20-30s）                                  │
│     ↓                                                        │
│  5. AI 生成素材                                              │
│     - 文生图：每个分镜生成关键帧图片                           │
│     - 图生视频：图片 → 3-5 秒视频片段                         │
│     - 语音合成：根据脚本生成配音                               │
│     ↓                                                        │
│  6. AI 自动剪辑                                              │
│     - 视频拼接：多段素材 + 转场效果                           │
│     - 字幕生成：自动识别/生成字幕                              │
│     - 音视频混合：视频 + 配音 → 成片                          │
│     ↓                                                        │
│  7. 人工审核（可选）                                          │
│     - 预览成片                                                │
│     - 调整修改（重新生成某段）                                 │
│     ↓                                                        │
│  8. AI 生成发布内容                                          │
│     - 标题生成（3 个候选）                                    │
│     - 封面生成（文生图）                                       │
│     - 话题推荐（#热门话题）                                    │
│     ↓                                                        │
│  9. 自动/手动发布                                            │
│     - 定时发布                                                │
│     - 多平台分发（抖音、视频号）                               │
└─────────────────────────────────────────────────────────────┘
```

### 方式 2：日常短视频策划

```
1. 输入主题/关键词
   ↓
2. AI 知识检索（从知识库查找灵感）
   ↓
3. AI 脚本生成（3 个方向供选择）
   ↓
4. 选择脚本 → 后续流程同上
```

### 方式 3：软广视频策划

```
1. 输入产品信息 + 广告要求
   ↓
2. AI 软广脚本生成（自然植入）
   ↓
3. 选择脚本 → 后续流程同上
```

---

## 🧩 核心功能模块

### 1. 脚本策划模块

**功能**：
- 爆款脚本分析
- AI 脚本生成
- 脚本库管理
- 脚本模板

**页面**：`ScriptPlanningPage.tsx`

**核心接口**：
```typescript
// 分析爆款脚本
POST /short-video/script/analyze-viral
{
  viralVideoUrl: string
  extractLevel: 'basic' | 'deep' // 基础分析/深度分析
}

// AI 生成脚本
POST /short-video/script/generate
{
  type: 'viral-clone' | 'daily' | 'soft-ad' // 爆款复刻/日常/软广
  theme?: string                            // 主题
  viralVideoId?: number                     // 参考爆款 ID
  productInfo?: string                      // 产品信息（软广）
  style?: string                            // 风格（搞笑、情感、干货等）
  duration?: number                         // 时长（秒）
}

// 保存脚本
POST /short-video/script/save
{
  title: string
  content: string
  type: string
  tags: string[]
}
```

---

### 2. 分镜设计模块

**功能**：
- AI 分镜生成
- 手动调整分镜
- 分镜预览
- 场景描述

**页面**：`ShotListDesignPage.tsx`

**核心接口**：
```typescript
// AI 生成分镜
POST /short-video/shot-list/generate
{
  scriptId: number                          // 脚本 ID
  scriptContent: string                     // 脚本内容
  shotCount?: number                        // 分镜数量（默认 4-6 个）
  style?: string                            // 风格
}

// 返回：
{
  shots: [
    {
      shotNumber: 1,
      timeRange: '0-3s',
      sceneDescription: '开场：城市夜景，霓虹灯闪烁',
      cameraAngle: '俯拍',
      action: '镜头缓缓推进',
      dialogue: '你知道吗？在这座城市里...',
      mood: '神秘、吸引'
    },
    // ...
  ]
}

// 保存分镜
POST /short-video/shot-list/save
{
  scriptId: number
  shots: ShotItem[]
}
```

---

### 3. 素材生产模块

**功能**：
- 首尾帧生成（文生图）
- 视频素材生成（图生视频）
- 配音生成（TTS）
- 素材库管理

**页面**：`MaterialProductionPage.tsx`

**核心接口**：
```typescript
// 批量生成首尾帧
POST /short-video/material/generate-keyframes
{
  shotListId: number
  shots: [
    {
      shotId: number
      sceneDescription: string
      style?: string
    }
  ]
}

// 返回：
{
  keyframes: [
    { shotId: 1, imageUrl: 'https://cdn.yourdomain.com/projects/123/keyframes/shot_001.jpg', prompt: '...' },  // BOS CDN URL
    { shotId: 2, imageUrl: 'https://cdn.yourdomain.com/projects/123/keyframes/shot_002.jpg', prompt: '...' }   // BOS CDN URL
  ]
}

// 图生视频（批量）
POST /short-video/material/img2video-batch
{
  keyframes: [
    {
      shotId: number
      imageUrl: string
      duration: number // 秒
      motion: 'zoom-in' | 'pan-right' | 'static' // 运镜
    }
  ]
}

// 返回：
{
  videos: [
    { shotId: 1, videoUrl: 'https://cdn.yourdomain.com/projects/123/videos/shot_001.mp4', duration: 3 },  // BOS CDN URL
    { shotId: 2, videoUrl: 'https://cdn.yourdomain.com/projects/123/videos/shot_002.mp4', duration: 5 }   // BOS CDN URL
  ]
}

// 批量生成配音
POST /short-video/material/generate-voice-batch
{
  shots: [
    {
      shotId: number
      dialogue: string
      voice?: string // 音色
      speed?: number
    }
  ]
}

// 返回：
{
  voices: [
    { shotId: 1, audioUrl: 'https://cdn.yourdomain.com/projects/123/audios/voice_001.mp3', duration: 2.5 },  // BOS CDN URL
    { shotId: 2, audioUrl: 'https://cdn.yourdomain.com/projects/123/audios/voice_002.mp3', duration: 3.2 }   // BOS CDN URL
  ]
}
```

---

### 4. 视频剪辑模块

**功能**：
- 自动拼接
- 字幕生成
- 转场效果
- 背景音乐
- 音视频混合

**页面**：`VideoEditingPage.tsx`

**核心接口**：
```typescript
// 自动剪辑成片
POST /short-video/edit/auto-compose
{
  projectId: number
  materials: {
    videos: [{ shotId: number, videoUrl: string, duration: number }]
    voices: [{ shotId: number, audioUrl: string }]
    bgmUrl?: string
    subtitles?: [{ shotId: number, text: string, startTime: number, duration: number }]
  }
  settings: {
    transition: 'fade' | 'cut' | 'slide' // 转场
    bgmVolume?: number                    // 背景音乐音量
    voiceVolume?: number                  // 配音音量
  }
}

// 返回：
{
  finalVideoUrl: string
  duration: number
  thumbnail: string
}

// 生成字幕
POST /short-video/edit/generate-subtitles
{
  videoUrl?: string      // 视频 URL（自动识别）
  script?: string        // 脚本（根据脚本生成）
  language?: string
}

// 返回：
{
  subtitles: [
    { text: '你知道吗？', startTime: 0, duration: 1.5 },
    { text: '在这座城市里', startTime: 1.5, duration: 2.0 }
  ],
  srtUrl: string // SRT 文件 URL
}
```

---

### 5. 审核发布模块

**功能**：
- 成片预览
- 人工审核
- AI 审核辅助
- 标题生成
- 封面优化
- 定时发布
- 多平台分发

**页面**：`PublishManagementPage.tsx`

**核心接口**：
```typescript
// AI 生成标题
POST /short-video/publish/generate-title
{
  videoUrl: string
  script?: string
  keywords?: string[]
  count?: number // 生成几个候选标题
}

// 返回：
{
  titles: [
    { text: '震惊！99%的人不知道这个秘密...', score: 0.92 },
    { text: '3分钟教你搞定XX，太实用了！', score: 0.88 }
  ]
}

// AI 审核
POST /short-video/publish/ai-review
{
  videoUrl: string
  title: string
  cover: string
}

// 返回：
{
  passed: boolean
  issues: [
    { type: 'sensitive-word', content: '违禁词：XXX' },
    { type: 'low-quality', content: '画质较低，建议重新生成' }
  ],
  suggestions: [
    '建议修改标题，避免使用"震惊"等标题党词汇',
    '建议增加字幕，提升观看体验'
  ]
}

// 发布
POST /short-video/publish/publish
{
  videoId: number
  platforms: ['douyin', 'weixin-video'] // 抖音、视频号
  publishTime?: string                  // 定时发布
  title: string
  cover: string
  tags: string[]
  location?: string
}

// 返回：
{
  results: [
    { platform: 'douyin', success: true, url: 'https://...' },
    { platform: 'weixin-video', success: false, error: '标题过长' }
  ]
}
```

---

### 6. 数据分析模块

**功能**：
- 热门视频采集
- 爆款数据分析
- 趋势预测
- 竞品分析

**页面**：`DataAnalysisPage.tsx`

**核心接口**：
```typescript
// 采集热门视频
POST /short-video/data/collect-hot-videos
{
  platform: 'douyin' | 'kuaishou' | 'weixin-video'
  category?: string
  count?: number
  dateRange?: { start: string, end: string }
}

// 返回：
{
  videos: [
    {
      id: string
      title: string
      author: string
      viewCount: number
      likeCount: number
      commentCount: number
      shareCount: number
      url: string
      tags: string[]
      publishTime: string
    }
  ]
}

// 爆款分析
POST /short-video/data/analyze-viral
{
  videoId: string
  platform: string
}

// 返回：
{
  viralScore: 95 // 爆款评分
  successFactors: [
    { factor: '开场吸引力', score: 0.92, description: '前3秒黄金开场' },
    { factor: '情感共鸣', score: 0.88, description: '引发强烈共鸣' },
    { factor: '内容节奏', score: 0.85, description: '节奏紧凑' }
  ]
  replicableMethods: [
    '使用反转开场吸引注意力',
    '植入情感故事引发共鸣',
    '结尾设置悬念引导互动'
  ]
}
```

---

## 🔌 AI 接口规划

### 第三方 AI 服务推荐

| 服务商 | 能力 | 费用 | 推荐指数 | 备注 |
|--------|------|------|---------|------|
| **DeepSeek** | 文案生成、脚本优化 | ¥0.001/千tokens | ⭐⭐⭐⭐⭐ | 性价比高，中文好 |
| **通义千问** | 脚本生成、标题优化 | ¥0.008/千tokens | ⭐⭐⭐⭐ | 阿里云，稳定 |
| **MiniMax** | **图生视频（推荐）** | ¥0.5/次 | ⭐⭐⭐⭐⭐ | 国内最好的图生视频 |
| **RunwayML** | 图生视频、视频编辑 | $0.05/秒 | ⭐⭐⭐⭐ | 国外服务，质量高 |
| **讯飞星火** | 语音合成、语音识别 | ¥0.01/次 | ⭐⭐⭐⭐ | 中文语音最好 |
| **Stable Diffusion** | 图像生成 | 自部署免费 | ⭐⭐⭐⭐⭐ | 开源，可控性强 |
| **剪映开放平台** | 字幕生成、视频编辑 | 免费 | ⭐⭐⭐⭐⭐ | 字节官方，免费 |
| **抖音开放平台** | 数据采集、自动发布 | 免费 | ⭐⭐⭐⭐⭐ | 官方 API |

### 重点推荐：MiniMax 图生视频

**为什么选择 MiniMax？**
1. ✅ **国内服务**：无需翻墙，速度快
2. ✅ **质量高**：生成质量接近 RunwayML
3. ✅ **价格低**：¥0.5/次，比 RunwayML 便宜 10 倍
4. ✅ **稳定性**：商业化产品，稳定可靠
5. ✅ **支持批量**：可批量生成

**MiniMax 接入示例**：
```typescript
// 后端实现
async img2video(imageUrl: string, duration: number): Promise<string> {
  const response = await axios.post('https://api.minimax.chat/v1/video_generation', {
    model: 'video-01',
    image_url: imageUrl,
    duration: duration, // 3-10 秒
    motion_type: 'auto', // 自动运镜
  }, {
    headers: {
      'Authorization': `Bearer ${process.env.MINIMAX_API_KEY}`,
      'Content-Type': 'application/json'
    }
  });

  return response.data.video_url;
}
```

### 成本估算

**单条短视频成本**（30 秒视频）：

| 步骤 | 服务 | 用量 | 单价 | 成本 |
|------|------|------|------|------|
| 脚本生成 | DeepSeek | 500 tokens | ¥0.001/千tokens | ¥0.0005 |
| 分镜生成 | DeepSeek | 1000 tokens | ¥0.001/千tokens | ¥0.001 |
| 图像生成（6 张） | Stable Diffusion | 6 次 | 自部署免费 | ¥0 |
| 图生视频（6 段） | MiniMax | 6 次 | ¥0.5/次 | ¥3 |
| 语音合成 | 讯飞星火 | 100 字 | ¥0.01/次 | ¥0.01 |
| 字幕生成 | 剪映 API | 1 次 | 免费 | ¥0 |
| 视频拼接 | FFmpeg | - | 自研免费 | ¥0 |
| **总计** | - | - | - | **¥3.01** |

**月成本**（日产 10 条）：
- AI 服务成本：¥3.01 × 10 × 30 = ¥903
- **BOS 存储成本**：¥3.1/月（300 条视频，约 25.8GB）
- **CDN 流量成本**：¥35/月（月播放 10,000 次）
- **总计**：¥903 + ¥3.1 + ¥35 = **¥941/月**

**结论**：成本极低，可规模化生产！

> **说明**：所有图片、视频、音频素材统一存储在**百度云对象存储（BOS）**，通过 CDN 加速访问。详见 [百度云存储集成方案](./BAIDU-BOS-STORAGE-INTEGRATION.md)

---

## 💾 数据库设计

### 核心表

> **说明**：所有媒体文件（图片、视频、音频）均存储在**百度云对象存储（BOS）**，数据库中保存的是 **BOS CDN URL**。

#### 1. `short_video_project` - 短视频项目表

```sql
CREATE TABLE short_video_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL COMMENT '项目标题',
    type VARCHAR(50) NOT NULL COMMENT '类型：viral_clone/daily/soft_ad',
    status VARCHAR(50) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/processing/completed/failed',

    -- 关联ID
    script_id BIGINT COMMENT '脚本ID',
    shot_list_id BIGINT COMMENT '分镜ID',

    -- 成果（BOS CDN URL）
    final_video_url VARCHAR(500) COMMENT '成片URL（BOS）',
    thumbnail_url VARCHAR(500) COMMENT '封面URL（BOS）',
    duration INT COMMENT '时长（秒）',

    -- 发布信息
    publish_title VARCHAR(255) COMMENT '发布标题',
    publish_platforms JSON COMMENT '发布平台',
    publish_time DATETIME COMMENT '发布时间',

    -- 审核信息
    review_status VARCHAR(50) COMMENT '审核状态：pending/passed/rejected',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_time DATETIME COMMENT '审核时间',
    review_comment TEXT COMMENT '审核意见',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) COMMENT '短视频项目表';
```

#### 2. `short_video_script` - 脚本表

```sql
CREATE TABLE short_video_script (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL COMMENT '脚本标题',
    content TEXT NOT NULL COMMENT '脚本内容',
    type VARCHAR(50) NOT NULL COMMENT '类型：viral_clone/daily/soft_ad',

    -- 生成信息
    generation_type VARCHAR(50) COMMENT '生成方式：ai/manual',
    reference_viral_id BIGINT COMMENT '参考爆款视频ID',
    theme VARCHAR(255) COMMENT '主题',
    style VARCHAR(50) COMMENT '风格：funny/emotional/educational',

    -- 元数据
    duration INT COMMENT '预估时长（秒）',
    word_count INT COMMENT '字数',
    tags JSON COMMENT '标签',

    -- AI生成参数
    ai_prompt TEXT COMMENT 'AI提示词',
    ai_model VARCHAR(100) COMMENT 'AI模型',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_type (type),
    INDEX idx_style (style)
) COMMENT '脚本表';
```

#### 3. `short_video_shot_list` - 分镜表

```sql
CREATE TABLE short_video_shot_list (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    script_id BIGINT NOT NULL COMMENT '脚本ID',
    shot_count INT NOT NULL COMMENT '分镜数量',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_script_id (script_id)
) COMMENT '分镜列表';
```

#### 4. `short_video_shot` - 分镜详情表

```sql
CREATE TABLE short_video_shot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shot_list_id BIGINT NOT NULL COMMENT '分镜列表ID',
    shot_number INT NOT NULL COMMENT '分镜序号',

    -- 分镜描述
    time_range VARCHAR(50) COMMENT '时间范围：0-3s',
    scene_description TEXT COMMENT '场景描述',
    camera_angle VARCHAR(100) COMMENT '机位：俯拍/平拍/仰拍',
    action VARCHAR(255) COMMENT '动作描述',
    dialogue TEXT COMMENT '台词',
    mood VARCHAR(100) COMMENT '情绪：神秘/搞笑/感动',

    -- 素材（BOS CDN URL）
    keyframe_url VARCHAR(500) COMMENT '关键帧图片URL（BOS）',
    video_url VARCHAR(500) COMMENT '视频素材URL（BOS）',
    audio_url VARCHAR(500) COMMENT '音频URL（BOS）',

    -- 元数据
    duration INT COMMENT '时长（秒）',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_shot_list_id (shot_list_id),
    INDEX idx_shot_number (shot_number)
) COMMENT '分镜详情表';
```

#### 5. `short_video_material` - 素材库表

```sql
CREATE TABLE short_video_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL COMMENT '类型：image/video/audio',
    url VARCHAR(500) NOT NULL COMMENT '素材URL（BOS CDN URL）',
    bos_key VARCHAR(500) COMMENT 'BOS对象Key（用于删除）',

    -- 关联
    shot_id BIGINT COMMENT '分镜ID',

    -- 元数据
    file_size BIGINT COMMENT '文件大小（字节）',
    duration INT COMMENT '时长（秒，视频/音频）',
    width INT COMMENT '宽度（图片/视频）',
    height INT COMMENT '高度（图片/视频）',
    format VARCHAR(50) COMMENT '格式：mp4/png/mp3',

    -- AI生成信息
    generation_type VARCHAR(50) COMMENT '生成方式：ai/upload',
    ai_prompt TEXT COMMENT 'AI提示词',
    ai_model VARCHAR(100) COMMENT 'AI模型',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_type (type),
    INDEX idx_shot_id (shot_id)
) COMMENT '素材库表';
```

#### 6. `hot_video_collection` - 热门视频采集表

```sql
CREATE TABLE hot_video_collection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform VARCHAR(50) NOT NULL COMMENT '平台：douyin/kuaishou/weixin',
    video_id VARCHAR(255) NOT NULL COMMENT '平台视频ID',

    -- 基础信息
    title VARCHAR(500) COMMENT '标题',
    author VARCHAR(255) COMMENT '作者',
    url VARCHAR(500) COMMENT '视频URL',
    cover_url VARCHAR(500) COMMENT '封面URL',

    -- 数据
    view_count BIGINT COMMENT '播放量',
    like_count BIGINT COMMENT '点赞数',
    comment_count BIGINT COMMENT '评论数',
    share_count BIGINT COMMENT '分享数',

    -- 分析
    viral_score DECIMAL(5,2) COMMENT '爆款评分',
    analyzed BOOLEAN DEFAULT FALSE COMMENT '是否已分析',

    -- 元数据
    tags JSON COMMENT '标签',
    category VARCHAR(100) COMMENT '分类',
    publish_time DATETIME COMMENT '发布时间',
    collect_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',

    UNIQUE KEY uk_platform_video (platform, video_id),
    INDEX idx_viral_score (viral_score),
    INDEX idx_analyzed (analyzed),
    INDEX idx_collect_time (collect_time)
) COMMENT '热门视频采集表';
```

---

## 🎨 前端页面设计

### 页面清单

| 页面 | 路由 | 功能 | 优先级 |
|------|------|------|--------|
| **脚本策划** | `/short-video/script` | 脚本生成、管理 | P0 |
| **分镜设计** | `/short-video/shot-list` | 分镜生成、编辑 | P0 |
| **素材生产** | `/short-video/material` | 素材生成、管理 | P0 |
| **视频剪辑** | `/short-video/edit` | 自动剪辑、预览 | P0 |
| **审核发布** | `/short-video/publish` | 审核、发布 | P1 |
| **项目管理** | `/short-video/project` | 项目列表、进度 | P1 |
| **数据分析** | `/short-video/analytics` | 热门采集、爆款分析 | P1 |
| **素材库** | `/short-video/library` | 素材管理 | P2 |

### 页面设计示例

#### ScriptPlanningPage（脚本策划）

```typescript
// frontend-react/src/pages/short-video/ScriptPlanningPage.tsx

功能区域：
┌────────────────────────────────────────────┐
│ 🎬 脚本策划                                 │
├────────────────────────────────────────────┤
│ [Tab切换]                                   │
│  ✅ AI生成  📄 脚本库  🔥 爆款分析           │
├────────────────────────────────────────────┤
│ 【AI生成 Tab】                              │
│                                            │
│ 生成类型：                                  │
│  ○ 爆款复刻  ○ 日常短视频  ○ 软广策划      │
│                                            │
│ [爆款复刻模式]                              │
│ 参考视频URL: [_________________]  [分析]   │
│                                            │
│ 主题/关键词: [_________________]           │
│ 风格: [搞笑 ▼]  时长: [30秒 ▼]            │
│                                            │
│ [🤖 AI生成脚本 (3个候选)]                   │
│                                            │
│ ┌──────────────────────────────────────┐  │
│ │ 脚本A（推荐）⭐⭐⭐⭐⭐                     │  │
│ │ 开场：你知道吗？99%的人...            │  │
│ │ 中间：其实只需要3个步骤...            │  │
│ │ 结尾：关注我，下期更精彩...           │  │
│ │ [选择此脚本] [编辑] [预览]            │  │
│ └──────────────────────────────────────┘  │
│                                            │
│ ┌──────────────────────────────────────┐  │
│ │ 脚本B ⭐⭐⭐⭐                            │  │
│ │ ...                                  │  │
│ └──────────────────────────────────────┘  │
└────────────────────────────────────────────┘
```

#### MaterialProductionPage（素材生产）

```typescript
// frontend-react/src/pages/short-video/MaterialProductionPage.tsx

功能区域：
┌────────────────────────────────────────────┐
│ 🎨 素材生产                                 │
├────────────────────────────────────────────┤
│ 项目：爆款复刻-XXX主题  [返回项目]          │
│ 进度：分镜 ✅ → 素材生产 ⏳ → 剪辑           │
├────────────────────────────────────────────┤
│ 分镜列表（6个）：                           │
│                                            │
│ ┌──────────────────────────────────────┐  │
│ │ 分镜1: 0-3s 开场（俯拍城市夜景）      │  │
│ │ 场景: 城市夜景，霓虹灯闪烁            │  │
│ │                                      │  │
│ │ 关键帧图片: [生成中...] ⏳            │  │
│ │ 视频素材:   [未生成]                 │  │
│ │ 配音:      [未生成]                  │  │
│ │                                      │  │
│ │ [🎨 生成关键帧] [🎬 生成视频] [🎤 生成配音] │  │
│ └──────────────────────────────────────┘  │
│                                            │
│ ┌──────────────────────────────────────┐  │
│ │ 分镜2: 3-10s 冲突                    │  │
│ │ 关键帧图片: ✅ [预览]                 │  │
│ │ 视频素材:   ✅ [预览] (5秒)           │  │
│ │ 配音:      ✅ [播放] (4.5秒)          │  │
│ │ [重新生成] [编辑]                     │  │
│ └──────────────────────────────────────┘  │
│                                            │
│ [💫 批量生成所有素材] [下一步：视频剪辑]    │
└────────────────────────────────────────────┘
```

---

## 📅 实施计划

### 第一阶段：基础能力建设（Week 1-2）

**目标**：搭建短视频模块基础架构 + 接入核心 AI 能力

**任务**：
1. ✅ 数据库表设计与创建
2. ✅ 后端 API 框架搭建
3. ✅ 接入 DeepSeek（脚本生成）
4. ✅ 接入 MiniMax（图生视频）
5. ✅ 前端页面框架搭建

**交付物**：
- 数据库 Schema
- 后端 API 接口文档
- 前端路由和基础页面

---

### 第二阶段：核心功能开发（Week 3-5）

**目标**：实现脚本→分镜→素材→成片全流程

**Week 3：脚本 + 分镜**
- [ ] 脚本生成 API（AI + 模板）
- [ ] 分镜生成 API（基于 LLM）
- [ ] ScriptPlanningPage 前端
- [ ] ShotListDesignPage 前端

**Week 4：素材生产**
- [ ] 图像生成 API（调用现有 text2img）
- [ ] 图生视频 API（MiniMax）
- [ ] 语音合成批量 API
- [ ] MaterialProductionPage 前端

**Week 5：视频剪辑**
- [ ] FFmpeg 视频拼接服务
- [ ] 字幕生成 API
- [ ] 音视频混合 API
- [ ] VideoEditingPage 前端

---

### 第三阶段：审核发布（Week 6-7）

**目标**：实现审核流程 + 自动发布

**Week 6：审核**
- [ ] 人工审核流程
- [ ] AI 审核辅助（敏感词、质量检测）
- [ ] 标题生成 API
- [ ] PublishManagementPage 前端

**Week 7：发布**
- [ ] 抖音开放平台接入
- [ ] 自动发布 API
- [ ] 定时发布调度
- [ ] 多平台分发

---

### 第四阶段：数据分析（Week 8）

**目标**：实现热门采集 + 爆款分析

**任务**：
- [ ] 热门视频采集 API（抖音数据）
- [ ] 爆款分析增强（调用现有能力）
- [ ] DataAnalysisPage 前端
- [ ] 数据可视化（趋势、热点）

---

### 第五阶段：优化迭代（Week 9-10）

**目标**：优化体验 + 提升效率

**任务**：
- [ ] 性能优化（批量处理、并发）
- [ ] 成本优化（缓存、复用）
- [ ] 用户体验优化（加载状态、错误处理）
- [ ] 文档完善（用户手册、API 文档）

---

## 💰 成本与收益分析

### 成本

| 项目 | 费用 | 说明 |
|------|------|------|
| **开发成本** | ¥150,000 | 2 名工程师 × 10 周 |
| **AI 服务费** | ¥1,000/月 | 日产 10 条，月产 300 条 |
| **服务器** | ¥2,000/月 | 4C8G × 2 台 |
| **带宽流量** | ¥500/月 | 视频存储与分发 |
| **总计（首年）** | **¥192,000** | 开发 + 运营 12 个月 |

### 收益

**人力节省**：
- 传统：1 名运营 + 1 名剪辑 = ¥20,000/月
- AI：1 名审核人员 = ¥8,000/月
- **节省**：¥12,000/月 = **¥144,000/年**

**效率提升**：
- 传统：1 天 1 条短视频
- AI：1 天 10 条短视频
- **效率**：提升 10 倍

**ROI**：
- 投入：¥192,000
- 节省：¥144,000/年
- **回本周期**：16 个月

---

## 🎯 下一步行动

### 立即行动（本周）

1. **注册 AI 服务账号**
   - ✅ DeepSeek（https://platform.deepseek.com/）
   - ✅ MiniMax（https://api.minimax.chat/）
   - ✅ 讯飞星火（https://xinghuo.xfyun.cn/）

2. **技术验证**
   - [ ] 测试 DeepSeek 脚本生成质量
   - [ ] 测试 MiniMax 图生视频效果
   - [ ] 测试 FFmpeg 视频拼接性能

3. **需求确认**
   - [ ] 确认业务优先级（爆款复刻 vs 日常视频）
   - [ ] 确认审核流程（全自动 vs 半自动）
   - [ ] 确认发布平台（抖音 vs 多平台）

### 下周安排

1. **数据库设计评审**
2. **API 接口设计评审**
3. **开始第一阶段开发**

---

**文档版本**: v1.0
**最后更新**: 2026-03-01
**维护人员**: Claude Code

**END OF DOCUMENT**
