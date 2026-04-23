# Phase 1 实现计划 - 可执行任务清单

> **依据**: SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md v3.3、IMPLEMENTATION_CHECKLIST.md  
> **验证**: 28 项改进已通过 Claude 全面验证  
> **执行者**: 代码实现 Agent

---

## 一、执行顺序与依赖

```
Step 1 (无依赖)     Step 2 (依赖 Step 1)      Step 3 (依赖 Step 2)
─────────────────────────────────────────────────────────────────
CameraType          AiVideoProvider            KlingVideoProvider
QualityLevel        VideoGenerationRequest     (包装现有 Kling)
VideoAspectRatio    VideoGenerationResult
VideoGenerationException
                         ↓
                    CinematicPromptEngine
                    (用 LlmClient, 附录 A.1)
                         ↓
                    IntelligentModelRouter
                    (knowledgeService 可选, 附录 A.2)
                         ↓
                    改造 img2videoBatch
                    改造 generateOneKeyframe
                    改造 Controller
                    DB 迁移
                         ↓
                    前端 CameraControlPanel
                    QualitySelector
                    API 类型更新
```

---

## 二、任务清单（按执行顺序）

### Step 1: 域模型与异常类（无依赖）

| # | 文件 | 操作 | 参考 |
|---|------|------|------|
| 1.1 | `module/ai/domain/CameraType.java` | 新建 | 主文档 4.1.1，完整代码 |
| 1.2 | `module/ai/domain/QualityLevel.java` | 新建 | 主文档 4.1.2 |
| 1.3 | `module/ai/domain/VideoAspectRatio.java` | 新建 | 主文档 4.1.3 |
| 1.4 | `module/ai/exception/VideoGenerationException.java` | 新建 | 主文档 4.3.1 前 |

### Step 2: 统一接口与 Prompt 引擎

| # | 文件 | 操作 | 参考 |
|---|------|------|------|
| 2.1 | `module/ai/service/AiVideoProvider.java` | 新建 | 主文档 4.3.1 |
| 2.2 | `module/ai/service/CinematicPromptEngine.java` | 新建 | 主文档 4.2，**附录 A.1 用 LlmClient** |
| 2.3 | `module/ai/service/IntelligentModelRouter.java` | 新建 | 主文档 4.3.2，**附录 A.2 knowledgeService 可选** |

### Step 3: Kling Provider 适配

| # | 文件 | 操作 | 参考 |
|---|------|------|------|
| 3.1 | `module/ai/service/impl/KlingVideoProvider.java` | 新建 | 主文档 4.3.3，**附录 A.3 ownerId 暂传 null** |

### Step 4: 改造现有代码

| # | 文件 | 操作 | 参考 |
|---|------|------|------|
| 4.1 | `ShortVideoMaterialServiceImpl.img2videoBatch()` | 改造 | 注入 IntelligentModelRouter + CinematicPromptEngine |
| 4.2 | `ShortVideoMaterialServiceImpl.generateOneKeyframe()` | 改造 | 512x512 → 768x1344 |
| 4.3 | `ShortVideoMaterialController.img2videoBatch()` | 改造 | 新增 quality, aspectRatio, cameraType, mood, action |
| 4.4 | `ShortVideoMaterialService.Img2VideoInput` | 改造 | 新增上述字段 |

### Step 5: 数据库迁移

| # | 文件 | 操作 |
|---|------|------|
| 5.1 | `sql/shortvideo/migration-phase1-cinematic.sql` | 新建 |

```sql
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS camera_type VARCHAR(50);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS camera_params TEXT;
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS quality_level VARCHAR(20);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS ai_model VARCHAR(50);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS quality_score DECIMAL(5,2);
ALTER TABLE sv_material ADD COLUMN IF NOT EXISTS post_processing_config TEXT;
ALTER TABLE sv_material ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(50);
```

### Step 6: 前端（可选，Phase 1 最小可延后）

| # | 文件 | 操作 | 参考 |
|---|------|------|------|
| 6.1 | `CameraControlPanel.tsx` | 新建 | 24 种运镜，主文档 6.2 |
| 6.2 | `QualitySelector.tsx` | 新建 | SD/HD/FHD/4K |
| 6.3 | `MaterialProductionPage.tsx` | 改造 | 集成运镜+质量选择 |
| 6.4 | `api/shortvideo.ts` | 改造 | img2videoBatch 新增参数 |

---

## 三、实施适配检查（附录 A）

执行时务必应用：

| 条款 | 应用位置 | 操作 |
|------|----------|------|
| A.1 | CinematicPromptEngine | `@Autowired(required=false) LlmClient llmClient`，用 `chatWithFallback` |
| A.2 | IntelligentModelRouter | `@Autowired(required=false) CinematicKnowledgeService` |
| A.3 | KlingVideoProvider | ownerId 传 null（或从 RequestContext 获取） |
| A.4 | 任务描述 | SD/HD 纯模板，FHD/4K 可选 LLM |
| A.5 | CameraType | 严格 24 种，与前端 CAMERA_TYPES 对齐 |
| A.6 | MiniMax/Runway | Phase 1 可仅做 Kling，后续按官方文档补充 |

---

## 四、Phase 1 最小范围（推荐首轮）

**不含**: MiniMax/Runway/Luma Provider、前端组件

**包含**: Step 1 + 2 + 3 + 4 + 5.1

**验收**:
- `mvn compile` 通过
- `img2videoBatch` 调用 `intelligentModelRouter.generateWithSmartRouting()`
- 关键帧 768x1344
- 数据库迁移执行成功

---

## 五、执行指令模板

**给实现 Agent 的 Prompt**:

```
实施 Phase 1 核心升级（最小范围）。

按 docs/PHASE1_IMPLEMENTATION_PLAN.md 顺序执行：
1. 创建 CameraType、QualityLevel、VideoAspectRatio、VideoGenerationException
2. 创建 AiVideoProvider、CinematicPromptEngine（用 LlmClient，附录 A.1）、IntelligentModelRouter（knowledgeService 可选）
3. 创建 KlingVideoProvider 包装现有 KlingVideoService
4. 改造 ShortVideoMaterialServiceImpl 和 Controller
5. 创建并执行数据库迁移

参考主文档 docs/SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md 第 4.1-4.3 节完整代码。
应用附录 A 的 6 项适配注意事项。
```

---

**文档版本**: v1.0  
**创建日期**: 2026-03-02
