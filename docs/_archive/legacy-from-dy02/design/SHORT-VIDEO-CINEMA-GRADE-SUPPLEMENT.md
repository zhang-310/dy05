# 短视频电影级自动化生产系统 - 技术方案补充

**版本**: v2.0 (电影级质量版)
**日期**: 2026-03-01
**核心升级**: 支持人物参考 + 场景参考 → 高质量图像 → 电影级视频

---

## 🎬 核心需求升级

### 从"普通 AI 生成"到"电影级质量"

#### 传统方案的问题
```
文字描述 → AI 生成图片 → 图生视频
❌ 人物不一致（每次生成的人物都不同）
❌ 场景不可控（无法精确控制场景风格）
❌ 质量不稳定（随机性太大）
❌ 缺乏导演思维（无法精确控制画面）
```

#### 电影级方案（升级版）
```
文字描述 + 人物参考图 + 场景参考图 → 高质量图像生成
                ↓
        首尾帧关键图片（精确控制）
                ↓
        图生视频（电影级运镜）
                ↓
        短视频片段（质量可控）
```

---

## 🔥 核心技术升级

### 1. 图像生成升级：支持参考图

#### 方案 A：ComfyUI 工作流（推荐）⭐⭐⭐⭐⭐

**为什么选择 ComfyUI？**
- ✅ **人物一致性**：IP-Adapter、InstantID 保证同一人物
- ✅ **场景控制**：ControlNet (depth/canny/pose) 精确控制场景
- ✅ **质量最高**：完全可控的生成流程
- ✅ **成本最低**：自部署，免费使用
- ✅ **灵活性强**：可以组合多种控制方式

**ComfyUI 工作流示例**：

```
输入：
├── 文字提示词："一个中国女孩站在咖啡厅里，微笑着，电影级光影"
├── 人物参考图：girl_reference.jpg（IP-Adapter）
├── 场景参考图：cafe_scene.jpg（ControlNet depth）
└── 姿态参考图：pose_standing.jpg（ControlNet pose）

↓ ComfyUI 工作流

输出：
└── 高质量关键帧图片（人物一致 + 场景精确 + 姿态控制）
```

**ComfyUI 核心节点**：

1. **IP-Adapter**（人物一致性）
   - 用途：保证同一人物在不同镜头一致
   - 模型：`ip-adapter-faceid_sd15.bin`
   - 效果：⭐⭐⭐⭐⭐（脸部特征 100% 一致）

2. **InstantID**（更强的人物一致性）
   - 用途：基于单张照片生成一致的人物
   - 模型：`instantid_sd15.safetensors`
   - 效果：⭐⭐⭐⭐⭐（比 IP-Adapter 更强）

3. **ControlNet**（场景/姿态/构图控制）
   - `depth`：场景深度控制
   - `canny`：边缘/线条控制
   - `pose`：人物姿态控制
   - `lineart`：线稿控制
   - 效果：⭐⭐⭐⭐⭐（精确到像素级）

**ComfyUI 工作流配置**：

```json
{
  "workflow_name": "电影级关键帧生成",
  "nodes": [
    {
      "type": "CheckpointLoaderSimple",
      "model": "realisticVisionV60B1_v51VAE.safetensors"
    },
    {
      "type": "IPAdapterFaceID",
      "reference_image": "character_reference.jpg",
      "weight": 0.9
    },
    {
      "type": "ControlNetApply",
      "control_net": "control_v11p_sd15_depth",
      "image": "scene_reference.jpg",
      "strength": 0.8
    },
    {
      "type": "KSampler",
      "steps": 30,
      "cfg": 8,
      "sampler": "dpmpp_2m_sde",
      "scheduler": "karras"
    }
  ]
}
```

**部署方案**：

```bash
# 服务器配置要求
GPU: NVIDIA RTX 4090 24GB（推荐）或 A100 40GB
CPU: 16 核
内存: 64GB
硬盘: 500GB SSD（存储模型）

# 安装 ComfyUI
git clone https://github.com/comfyanonymous/ComfyUI.git
cd ComfyUI
pip install -r requirements.txt

# 下载模型
models/
├── checkpoints/
│   └── realisticVisionV60B1_v51VAE.safetensors (5GB)
├── ipadapter/
│   └── ip-adapter-faceid_sd15.bin (700MB)
├── instantid/
│   └── instantid_sd15.safetensors (1.5GB)
└── controlnet/
    ├── control_v11p_sd15_depth.pth (1.4GB)
    ├── control_v11p_sd15_canny.pth (1.4GB)
    └── control_v11p_sd15_openpose.pth (1.4GB)

# 总存储需求：约 15GB
```

---

#### 方案 B：Midjourney + 参考图（备选）

**优点**：
- ✅ 质量极高（顶级 AI 绘画）
- ✅ 支持参考图（`--cref` 人物参考，`--sref` 风格参考）
- ✅ 无需部署

**缺点**：
- ❌ 成本较高（$30/月 或 $0.5/张）
- ❌ 速度较慢（排队时间）
- ❌ 人物一致性不如 ComfyUI

**Midjourney 参考图示例**：

```
提示词：
a Chinese girl standing in a coffee shop, smiling, cinematic lighting,
high quality, 8k --cref https://example.com/girl.jpg --cw 100
--sref https://example.com/cafe_scene.jpg --sw 50

参数说明：
--cref：人物参考图 URL
--cw：人物权重（0-100，100 最高）
--sref：风格参考图 URL
--sw：风格权重（0-1000）
```

---

### 2. 图生视频升级：电影级运镜

#### 方案 A：Runway Gen-3（顶级质量）⭐⭐⭐⭐⭐

**为什么选择 Runway Gen-3？**
- ✅ **质量最高**：业界顶级，接近真实视频
- ✅ **运镜自然**：支持复杂运镜（推拉摇移）
- ✅ **一致性好**：基于关键帧生成，保持一致
- ✅ **时长灵活**：支持 1-10 秒

**价格**：
- $0.05/秒（生成）
- 单段 5 秒视频 = $0.25
- 6 段视频（30 秒短视频）= **$1.5/条**

**API 调用示例**：

```typescript
// 后端实现
async img2video_runway(
  imageUrl: string,
  duration: number,
  motion: 'zoom-in' | 'pan-right' | 'dolly-forward'
): Promise<string> {
  const response = await axios.post('https://api.runwayml.com/v1/gen3/image_to_video', {
    image_url: imageUrl,
    duration: duration,
    motion_bucket_id: this.getMotionBucketId(motion),
    fps: 24,
    quality: 'high'
  }, {
    headers: {
      'Authorization': `Bearer ${process.env.RUNWAY_API_KEY}`,
      'Content-Type': 'application/json'
    }
  });

  return response.data.video_url;
}

getMotionBucketId(motion: string): number {
  const motionMap = {
    'zoom-in': 127,      // 推镜
    'pan-right': 100,    // 右移
    'dolly-forward': 150 // 前移
  };
  return motionMap[motion] || 100;
}
```

---

#### 方案 B：Kling（快手可灵）⭐⭐⭐⭐⭐

**为什么选择 Kling？**
- ✅ **国内服务**：无需翻墙，速度快
- ✅ **质量高**：接近 Runway，甚至更好
- ✅ **价格低**：¥0.3-0.5/次（比 Runway 便宜 5 倍）
- ✅ **支持中文**：提示词优化更好
- ✅ **长视频**：支持 1-10 秒

**价格**：
- 5 秒视频：¥0.4/次
- 6 段视频（30 秒）= **¥2.4/条**

**API 调用示例**：

```typescript
// 后端实现
async img2video_kling(
  imageUrl: string,
  duration: number,
  prompt?: string
): Promise<string> {
  const response = await axios.post('https://api.kling.kuaishou.com/v1/videos/image2video', {
    image_url: imageUrl,
    duration: duration,
    prompt: prompt || '自然运镜，电影级质感',
    mode: 'professional', // standard / professional
    fps: 24
  }, {
    headers: {
      'Authorization': `Bearer ${process.env.KLING_API_KEY}`,
      'Content-Type': 'application/json'
    }
  });

  // Kling 是异步生成，需要轮询
  const taskId = response.data.task_id;
  return await this.pollKlingTask(taskId);
}

async pollKlingTask(taskId: string): Promise<string> {
  while (true) {
    const status = await axios.get(`https://api.kling.kuaishou.com/v1/videos/status/${taskId}`);
    if (status.data.state === 'succeeded') {
      return status.data.video_url;
    }
    if (status.data.state === 'failed') {
      throw new Error(status.data.error);
    }
    await sleep(5000); // 等待 5 秒
  }
}
```

---

#### 方案 C：Pika 1.5（创意运镜）⭐⭐⭐⭐

**特色**：
- ✅ **创意运镜**：支持夸张、创意的运镜效果
- ✅ **参数丰富**：camera motion、movement strength
- ✅ **质量稳定**：生成质量一致性好

**价格**：
- $0.5/次（5 秒）
- 稍贵于 Runway

**适用场景**：
- 需要夸张运镜的创意视频
- 广告、宣传片

---

### 3. 运镜方案（电影级）

**镜头语言库**：

```typescript
// 定义运镜类型
enum CameraMotion {
  // 基础运镜
  STATIC = 'static',           // 静止镜头
  ZOOM_IN = 'zoom-in',         // 推镜（zoom in）
  ZOOM_OUT = 'zoom-out',       // 拉镜（zoom out）
  PAN_LEFT = 'pan-left',       // 左摇
  PAN_RIGHT = 'pan-right',     // 右摇
  TILT_UP = 'tilt-up',         // 上仰
  TILT_DOWN = 'tilt-down',     // 下俯

  // 高级运镜
  DOLLY_IN = 'dolly-forward',  // 推轨（dolly in）
  DOLLY_OUT = 'dolly-back',    // 拉轨（dolly out）
  CRANE_UP = 'crane-up',       // 升降机上升
  CRANE_DOWN = 'crane-down',   // 升降机下降
  ORBIT = 'orbit',             // 环绕
  TRACK_LEFT = 'track-left',   // 轨道左移
  TRACK_RIGHT = 'track-right', // 轨道右移

  // 创意运镜
  WHIP_PAN = 'whip-pan',       // 快速摇镜
  DUTCH_ANGLE = 'dutch-angle', // 荷兰角（倾斜）
  VERTIGO = 'vertigo-effect'   // 希区柯克式（dolly zoom）
}

// 镜头语言推荐
interface ShotRecommendation {
  shotType: string;          // 分镜类型
  cameraMotion: CameraMotion; // 推荐运镜
  reason: string;            // 推荐理由
}

const shotMotionMap: Record<string, ShotRecommendation> = {
  '开场': {
    shotType: 'establishing-shot',
    cameraMotion: CameraMotion.DOLLY_IN,
    reason: '推镜建立场景，吸引注意力'
  },
  '人物特写': {
    shotType: 'close-up',
    cameraMotion: CameraMotion.ZOOM_IN,
    reason: '推镜聚焦人物情绪'
  },
  '冲突场景': {
    shotType: 'medium-shot',
    cameraMotion: CameraMotion.WHIP_PAN,
    reason: '快速摇镜营造紧张感'
  },
  '情感高潮': {
    shotType: 'extreme-close-up',
    cameraMotion: CameraMotion.STATIC,
    reason: '静止镜头强调情感'
  },
  '结尾': {
    shotType: 'wide-shot',
    cameraMotion: CameraMotion.DOLLY_OUT,
    reason: '拉镜结束，留白想象'
  }
};
```

---

## 🎨 完整工作流程（电影级）

### 流程图

```
1. 脚本策划
   ├── 输入：主题 + 爆款参考
   ├── AI 生成脚本（DeepSeek）
   └── 输出：完整脚本 + 情绪线

2. 分镜设计
   ├── 输入：脚本
   ├── AI 生成分镜列表（6-8 个镜头）
   ├── 自动匹配运镜方式
   └── 输出：分镜脚本（包含场景描述 + 运镜）

3. 人物/场景素材准备
   ├── 上传人物参考图（主角照片）→ 保存到 BOS
   ├── 上传场景参考图（可选）→ 保存到 BOS
   └── 素材库管理（BOS 路径：references/characters/, references/scenes/）

4. 关键帧生成（ComfyUI）
   ├── 输入：分镜描述 + 人物参考 + 场景参考
   ├── ComfyUI 工作流：
   │   ├── IP-Adapter（人物一致性）
   │   ├── ControlNet（场景控制）
   │   └── 高质量模型（RealisticVision）
   └── 输出：6-8 张关键帧图片（人物一致）→ 自动上传到 BOS

5. 视频片段生成（Kling/Runway）
   ├── 输入：关键帧图片（BOS CDN URL）+ 运镜参数
   ├── 批量生成：
   │   ├── 分镜1：推镜 5秒
   │   ├── 分镜2：右摇 4秒
   │   ├── 分镜3：静止 3秒
   │   └── ...
   └── 输出：6-8 段视频片段（电影级运镜）→ 自动上传到 BOS

6. 配音生成（讯飞星火）
   ├── 输入：脚本台词
   ├── 选择音色（温柔女声/磁性男声）
   └── 输出：高质量配音 mp3 → 自动上传到 BOS

7. 自动剪辑成片（FFmpeg）
   ├── 输入：视频片段（BOS URL）+ 配音（BOS URL）+ 背景音乐
   ├── 自动操作：
   │   ├── 视频拼接（转场效果）
   │   ├── 字幕生成（剪映 API）
   │   ├── 音视频混合
   │   └── 调色/特效（可选）
   └── 输出：完整成片 mp4 → 自动上传到 BOS

8. 人工审核
   ├── 预览成片（BOS CDN 加速播放）
   ├── 调整修改（重新生成某段）
   └── 审核通过

9. 发布
   ├── AI 生成标题（3 个候选）
   ├── AI 生成封面（ComfyUI）→ 上传到 BOS
   ├── 选择发布平台（抖音/视频号）
   └── 定时发布（直接使用 BOS CDN URL）
```

> **重要说明**：所有媒体文件（人物参考图、场景参考图、关键帧、视频片段、配音、成片）均**自动上传到百度云对象存储（BOS）**，并通过 **CDN 加速访问**。详见 [百度云存储集成方案](./BAIDU-BOS-STORAGE-INTEGRATION.md)

---

## 💰 成本对比（电影级 vs 普通）

### 单条 30 秒短视频成本对比

| 步骤 | 普通方案 | 电影级方案 | 质量提升 |
|------|---------|-----------|---------|
| **脚本生成** | DeepSeek ¥0.001 | DeepSeek ¥0.001 | 相同 |
| **分镜生成** | DeepSeek ¥0.001 | DeepSeek ¥0.001 | 相同 |
| **图像生成** | SD 免费 | **ComfyUI ¥0** | ⭐⭐⭐⭐⭐ |
| **图生视频** | MiniMax ¥3 | **Kling ¥2.4** | ⭐⭐⭐⭐⭐ |
| **配音** | 讯飞 ¥0.01 | 讯飞 ¥0.01 | 相同 |
| **字幕** | 剪映 免费 | 剪映 免费 | 相同 |
| **剪辑** | FFmpeg 免费 | FFmpeg 免费 | 相同 |
| **BOS 存储** | - | **¥0.01/条** | CDN 加速 |
| **总成本** | **¥3.01** | **¥2.42** | **更便宜！** |

**结论**：
- ✅ **电影级方案成本更低**（¥2.42 vs ¥3.01）
- ✅ **质量大幅提升**（人物一致 + 场景可控 + 电影级运镜）
- ✅ **可控性更强**（导演级控制）
- ✅ **访问速度快**（BOS + CDN 加速）

> **BOS 存储成本说明**：单条视频约 86MB（关键帧 + 视频片段 + 配音 + 成片），月存储 ¥0.01/条。详见 [百度云存储集成方案](./BAIDU-BOS-STORAGE-INTEGRATION.md)

---

## 🛠️ 技术实施方案

### 后端架构

```typescript
// 核心服务模块

1. ImageGenerationService（图像生成服务）
   ├── comfyui_generate()         // ComfyUI 工作流调用
   ├── apply_character_reference() // 应用人物参考
   ├── apply_scene_control()       // 应用场景控制
   ├── batch_generate()            // 批量生成关键帧
   └── upload_to_bos()             // 自动上传到 BOS

2. VideoGenerationService（视频生成服务）
   ├── kling_img2video()           // Kling 图生视频
   ├── runway_img2video()          // Runway 图生视频（备选）
   ├── apply_camera_motion()       // 应用运镜参数
   ├── batch_generate_videos()     // 批量生成视频片段
   └── upload_to_bos()             // 自动上传到 BOS

3. VideoEditService（视频剪辑服务）
   ├── merge_videos()              // FFmpeg 视频拼接
   ├── add_subtitles()             // 字幕合成
   ├── mix_audio_video()           // 音视频混合
   ├── add_transitions()           // 添加转场效果
   └── upload_to_bos()             // 成片上传到 BOS

4. MaterialLibraryService（素材管理服务）
   ├── upload_character_reference() // 上传人物参考图（BOS）
   ├── upload_scene_reference()     // 上传场景参考图（BOS）
   ├── manage_assets()              // 素材库管理（BOS）
   └── version_control()            // 版本控制

5. BosStorageService（百度云存储服务）
   ├── upload()                     // 通用上传
   ├── uploadKeyframe()             // 上传关键帧
   ├── uploadVideoClip()            // 上传视频片段
   ├── uploadFinalVideo()           // 上传成片
   ├── uploadAudio()                // 上传音频
   ├── delete()                     // 删除文件
   └── generatePresignedUrl()       // 生成临时访问URL
```

> **集成说明**：所有服务生成的媒体文件均通过 `BosStorageService` 自动上传到百度云对象存储，返回 BOS CDN URL 供后续使用。详见 [百度云存储集成方案](./BAIDU-BOS-STORAGE-INTEGRATION.md)

---

### ComfyUI API 封装

```typescript
// backend/src/services/ComfyUIService.ts

interface ComfyUIWorkflowParams {
  prompt: string;                   // 提示词
  characterReference?: string;      // 人物参考图 URL
  sceneReference?: string;          // 场景参考图 URL
  poseReference?: string;           // 姿态参考图 URL
  width: number;                    // 宽度
  height: number;                   // 高度
  steps: number;                    // 采样步数
  cfg: number;                      // CFG Scale
}

class ComfyUIService {
  private comfyuiUrl = process.env.COMFYUI_URL || 'http://localhost:8188';

  /**
   * 生成电影级关键帧
   */
  async generateKeyframe(params: ComfyUIWorkflowParams): Promise<string> {
    // 构建工作流
    const workflow = this.buildWorkflow(params);

    // 提交任务
    const response = await axios.post(`${this.comfyuiUrl}/prompt`, {
      prompt: workflow
    });

    const taskId = response.data.prompt_id;

    // 轮询结果
    return await this.pollResult(taskId);
  }

  /**
   * 构建 ComfyUI 工作流
   */
  private buildWorkflow(params: ComfyUIWorkflowParams): any {
    return {
      "1": {
        "class_type": "CheckpointLoaderSimple",
        "inputs": {
          "ckpt_name": "realisticVisionV60B1_v51VAE.safetensors"
        }
      },
      "2": {
        "class_type": "CLIPTextEncode",
        "inputs": {
          "text": params.prompt,
          "clip": ["1", 1]
        }
      },
      // IP-Adapter（人物一致性）
      "3": params.characterReference ? {
        "class_type": "IPAdapterApplyFaceID",
        "inputs": {
          "image": this.loadImage(params.characterReference),
          "weight": 0.9,
          "model": ["1", 0]
        }
      } : null,
      // ControlNet（场景控制）
      "4": params.sceneReference ? {
        "class_type": "ControlNetApply",
        "inputs": {
          "image": this.loadImage(params.sceneReference),
          "strength": 0.8,
          "conditioning": ["2", 0],
          "control_net": this.loadControlNet("depth")
        }
      } : null,
      // KSampler（采样）
      "5": {
        "class_type": "KSampler",
        "inputs": {
          "seed": Math.floor(Math.random() * 1000000000),
          "steps": params.steps || 30,
          "cfg": params.cfg || 8,
          "sampler_name": "dpmpp_2m_sde",
          "scheduler": "karras",
          "denoise": 1,
          "model": ["3", 0] || ["1", 0],
          "positive": ["4", 0] || ["2", 0],
          "negative": ["2", 1],
          "latent_image": ["6", 0]
        }
      },
      // EmptyLatentImage
      "6": {
        "class_type": "EmptyLatentImage",
        "inputs": {
          "width": params.width,
          "height": params.height,
          "batch_size": 1
        }
      },
      // VAEDecode
      "7": {
        "class_type": "VAEDecode",
        "inputs": {
          "samples": ["5", 0],
          "vae": ["1", 2]
        }
      },
      // SaveImage
      "8": {
        "class_type": "SaveImage",
        "inputs": {
          "images": ["7", 0],
          "filename_prefix": "keyframe_"
        }
      }
    };
  }

  /**
   * 轮询生成结果
   */
  private async pollResult(taskId: string): Promise<string> {
    while (true) {
      const history = await axios.get(`${this.comfyuiUrl}/history/${taskId}`);

      if (history.data[taskId]?.status?.completed) {
        const outputs = history.data[taskId].outputs;
        const imageBuffer = this.extractImageBuffer(outputs);

        // 上传到 BOS
        const bosService = new BosStorageService();
        const result = await bosService.upload(
          imageBuffer,
          `keyframe_${taskId}.jpg`,
          {
            folder: 'temp/comfyui',
            isPublic: true
          }
        );

        return result.cdnUrl; // 返回 BOS CDN URL
      }

      await sleep(2000); // 等待 2 秒
    }
  }
}
```

> **重要提示**：ComfyUI 生成的图片会自动上传到百度云 BOS，并返回 CDN 加速 URL。

---

### Kling API 封装

```typescript
// backend/src/services/KlingVideoService.ts

interface KlingVideoParams {
  imageUrl: string;
  duration: number;              // 1-10 秒
  motion: CameraMotion;          // 运镜类型
  prompt?: string;               // 额外提示词
  mode: 'standard' | 'professional'; // 标准/专业模式
}

class KlingVideoService {
  private apiKey = process.env.KLING_API_KEY;
  private apiUrl = 'https://api.kling.kuaishou.com/v1/videos';

  /**
   * 图生视频
   */
  async img2video(params: KlingVideoParams): Promise<string> {
    // 构建提示词（包含运镜描述）
    const fullPrompt = this.buildMotionPrompt(params.motion, params.prompt);

    // 提交任务
    const response = await axios.post(`${this.apiUrl}/image2video`, {
      image_url: params.imageUrl,  // BOS CDN URL
      duration: params.duration,
      prompt: fullPrompt,
      mode: params.mode,
      fps: 24,
      cfg_scale: 0.5
    }, {
      headers: {
        'Authorization': `Bearer ${this.apiKey}`,
        'Content-Type': 'application/json'
      }
    });

    const taskId = response.data.task_id;

    // 轮询结果（返回视频URL）
    const videoUrl = await this.pollTask(taskId);

    // 下载视频并上传到 BOS
    const videoBuffer = await this.downloadVideo(videoUrl);
    const bosService = new BosStorageService();
    const result = await bosService.upload(
      videoBuffer,
      `video_${taskId}.mp4`,
      {
        folder: 'temp/kling',
        contentType: 'video/mp4',
        isPublic: true
      }
    );

    return result.cdnUrl; // 返回 BOS CDN URL
  }

  /**
   * 构建运镜提示词
   */
  private buildMotionPrompt(motion: CameraMotion, extraPrompt?: string): string {
    const motionDescriptions: Record<CameraMotion, string> = {
      [CameraMotion.STATIC]: '静止镜头，电影级质感',
      [CameraMotion.ZOOM_IN]: '缓慢推镜，聚焦主体，电影级光影',
      [CameraMotion.ZOOM_OUT]: '缓慢拉镜，展现全景，电影级构图',
      [CameraMotion.PAN_RIGHT]: '平滑右摇，电影级运镜',
      [CameraMotion.PAN_LEFT]: '平滑左摇，电影级运镜',
      [CameraMotion.DOLLY_IN]: '推轨向前，电影级运镜',
      [CameraMotion.DOLLY_OUT]: '推轨后退，电影级运镜',
      [CameraMotion.CRANE_UP]: '升降机上升，电影级航拍',
      [CameraMotion.WHIP_PAN]: '快速甩镜，动感强烈',
      // ... 其他运镜
    };

    const basePrompt = motionDescriptions[motion] || '电影级运镜';
    return extraPrompt ? `${basePrompt}, ${extraPrompt}` : basePrompt;
  }

  /**
   * 批量生成视频片段
   */
  async batchGenerate(shots: Array<{
    imageUrl: string;
    motion: CameraMotion;
    duration: number;
  }>): Promise<string[]> {
    // 并发生成（限制并发数为 3）
    const results: string[] = [];
    for (let i = 0; i < shots.length; i += 3) {
      const batch = shots.slice(i, i + 3);
      const promises = batch.map(shot => this.img2video({
        imageUrl: shot.imageUrl,
        duration: shot.duration,
        motion: shot.motion,
        mode: 'professional'
      }));

      const batchResults = await Promise.all(promises);
      results.push(...batchResults);
    }

    return results;
  }

  /**
   * 下载视频文件
   */
  private async downloadVideo(videoUrl: string): Promise<Buffer> {
    const response = await axios.get(videoUrl, {
      responseType: 'arraybuffer'
    });
    return Buffer.from(response.data);
  }
}
```

> **重要提示**：Kling 生成的视频会自动下载并上传到百度云 BOS，返回 CDN 加速 URL，提升访问速度。

---

## 📋 交付给 Cursor 的开发清单

### 第一阶段：基础架构（Week 1）

**后端任务**：
- [ ] 创建数据库表（6 张核心表）
- [ ] 搭建 NestJS/Express 后端框架
- [ ] 封装 ComfyUI API 服务
- [ ] 封装 Kling API 服务
- [ ] 封装 DeepSeek API 服务（脚本生成）

**前端任务**：
- [ ] 创建路由（8 个页面）
- [ ] 搭建基础页面框架
- [ ] 集成 MUI 组件库

**环境部署**：
- [ ] ComfyUI 服务器搭建（GPU 服务器）
- [ ] 下载和配置模型（15GB）
- [ ] 测试 ComfyUI API

---

### 第二阶段：核心功能（Week 2-4）

**Week 2：脚本 + 分镜 + 素材管理**
- [ ] 脚本生成 API（DeepSeek）
- [ ] 分镜生成 API（AI 分析脚本）
- [ ] 人物/场景参考图上传（集成 BOS）
- [ ] ScriptPlanningPage 前端
- [ ] ShotListDesignPage 前端
- [ ] MaterialLibraryPage 前端（素材管理 + BOS 集成）

**Week 3：关键帧生成**
- [ ] ComfyUI 工作流封装
- [ ] IP-Adapter 人物一致性
- [ ] ControlNet 场景控制
- [ ] 批量生成关键帧 API（自动上传 BOS）
- [ ] MaterialProductionPage 前端（关键帧生成 + 预览）

**Week 4：视频生成**
- [ ] Kling 图生视频 API（自动上传 BOS）
- [ ] 运镜参数配置
- [ ] 批量生成视频片段
- [ ] 视频预览功能（BOS CDN 加速）
- [ ] MaterialProductionPage 前端（视频生成 + 播放）

---

### 第三阶段：剪辑发布（Week 5-6）

**Week 5：自动剪辑**
- [ ] FFmpeg 视频拼接（从 BOS 下载，处理后上传 BOS）
- [ ] 转场效果（fade/cut/slide）
- [ ] 字幕生成（剪映 API）
- [ ] 音视频混合（成片上传 BOS）
- [ ] VideoEditingPage 前端（预览 BOS 视频）

**Week 6：审核发布**
- [ ] 人工审核流程（播放 BOS 视频）
- [ ] 标题生成 API
- [ ] 封面生成（ComfyUI）→ 上传 BOS
- [ ] 抖音开放平台接入（使用 BOS CDN URL）
- [ ] PublishManagementPage 前端

---

## 📊 服务器配置建议

### ComfyUI 服务器（必须）

```yaml
配置方案 A（推荐）：
GPU: NVIDIA RTX 4090 24GB
CPU: AMD Ryzen 9 5950X (16 核)
内存: 64GB DDR4
硬盘: 500GB NVMe SSD
月租: ¥3,000 - ¥4,000

配置方案 B（性价比）：
GPU: NVIDIA RTX 3090 24GB
CPU: Intel i7-12700K (12 核)
内存: 32GB DDR4
硬盘: 500GB SSD
月租: ¥2,000 - ¥2,500

配置方案 C（云服务器）：
阿里云 GPU 实例：ecs.gn7i-c16g1.4xlarge
GPU: NVIDIA A10 24GB × 1
CPU: 16 核
内存: 60GB
按量付费: ¥18/小时
包月: ¥10,000/月（不推荐，太贵）
```

**推荐方案**：自己购买 RTX 4090 服务器（¥3,000/月）

---

## 🎯 总结

### 电影级方案的核心优势

| 维度 | 普通方案 | 电影级方案 | 提升 |
|------|---------|-----------|------|
| **人物一致性** | ❌ 每次不同 | ✅ 100% 一致（IP-Adapter） | ⭐⭐⭐⭐⭐ |
| **场景可控性** | ❌ 随机生成 | ✅ 精确控制（ControlNet） | ⭐⭐⭐⭐⭐ |
| **视频质量** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐（Kling/Runway） | +200% |
| **运镜专业度** | ⭐⭐ | ⭐⭐⭐⭐⭐（电影级） | +300% |
| **成本** | ¥3.01/条 | ¥2.42/条 | -20% |
| **访问速度** | 普通 | ⭐⭐⭐⭐⭐（BOS CDN） | CDN 加速 |

### 关键技术选型

1. **ComfyUI**（图像生成）
   - 人物一致性：IP-Adapter / InstantID
   - 场景控制：ControlNet (depth/canny/pose)
   - 成本：免费（自部署）

2. **Kling**（图生视频）
   - 质量：接近 Runway
   - 成本：¥0.4/段（5秒）
   - 速度：5-10 分钟/段

3. **DeepSeek**（脚本生成）
   - 成本：¥0.001/千tokens
   - 质量：接近 GPT-4

### 立即行动

1. **注册账号**：
   - Kling：https://kling.kuaishou.com/
   - DeepSeek：https://platform.deepseek.com/
   - **百度云 BOS**：https://cloud.baidu.com/product/bos.html

2. **购买/租赁 GPU 服务器**：
   - RTX 4090 24GB（推荐）

3. **下载模型**：
   - Realistic Vision V6
   - IP-Adapter FaceID
   - ControlNet 系列

4. **配置 BOS 存储**：
   - 创建 Bucket（dy01-prod-media）
   - 配置 CDN 加速
   - 设置访问权限和防盗链

5. **开始开发**（交给 Cursor）：
   - 使用本文档作为技术方案
   - 按照开发清单逐步实现
   - 集成百度云 BOS 存储服务

---

**文档版本**: v2.1 (电影级 + BOS 存储集成)
**最后更新**: 2026-03-01
**维护人员**: Claude Code

**准备好交给 Cursor 开发了！所有媒体文件统一存储到百度云 BOS！** 🚀
