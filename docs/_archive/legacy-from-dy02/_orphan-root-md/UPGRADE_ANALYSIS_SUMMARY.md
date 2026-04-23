# 电影级短视频质量升级 - 分析总结

> 基于现有代码与升级文档的差距分析，供决策参考

**分析日期**: 2026-03-02  
**参考文档**: SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md (v3.3), IMPLEMENTATION_CHECKLIST.md (v3.3)

---

## 一、当前系统现状

### 1.1 已有能力 ✅

| 模块 | 现状 | 说明 |
|------|------|------|
| **Kling 图生视频** | ✅ 已实现 | `KlingVideoServiceImpl`，支持 img2video / img2videoUrl |
| **FFmpeg 降级** | ✅ 已实现 | `VideoGenerationServiceImpl`，首尾帧 zoompan/xfade 运镜 |
| **批量图生视频** | ✅ 已实现 | `ShortVideoMaterialServiceImpl.img2videoBatch` |
| **首帧+尾帧** | ✅ 已实现 | sv_shot.end_frame_url，首帧→尾帧运镜 |
| **BOS 存储** | ✅ 已实现 | 关键帧/视频上传 CDN |
| **关键帧生成** | ✅ 已实现 | 文生图/ComfyUI/图生图，每镜首帧+尾帧 |
| **配音 TTS** | ✅ 已实现 | 可灵/讯飞等 |
| **自动剪辑** | ✅ 已实现 | autoCompose 视频拼接+配音+字幕 |

### 1.2 当前短板 ❌

| 问题 | 影响 |
|------|------|
| **Prompt 固定** | 所有镜头统一 "自然运镜，电影级质感"，无法按场景/运镜定制 |
| **单一模型** | 仅 Kling + FFmpeg，无 MiniMax/Runway 备用 |
| **无运镜选择** | 用户无法选择推轨/摇臂/环绕等专业运镜 |
| **无质量档位** | 无 SD/HD/FHD/4K 选择，输出固定 |
| **无后期处理** | 无调色、稳定、降噪、锐化等 |
| **无质量检测** | 无清晰度/噪点/色彩等自动评分 |

---

## 二、升级文档规划概览

### 2.1 四阶段任务总览

| 阶段 | 周期 | 核心任务 | 任务数 |
|------|------|----------|--------|
| **Phase 1** | 2 周 | 智能 Prompt、多模型、运镜扩展、前端控制 | 11 |
| **Phase 2** | 2 周 | 后期流水线、调色、质量检测、前端编辑器 | 7 |
| **Phase 3** | 1 周 | 故事板、预设库、批量优化、进度提示 | 4 |
| **Phase 4** | 1 周 | 并发、缓存、CDN、监控 | 4 |

### 2.2 新增/变更文件清单

**后端 (Java)**  
- `CinematicPromptEngine.java` - 智能 Prompt 引擎  
- `CameraType.java` - 运镜类型枚举  
- `QualityLevel.java` - 质量级别枚举  
- `MultiModelVideoService.java` - 多模型策略  
- `EnhancedKlingVideoService.java` - 增强 Kling  
- `MiniMaxVideoServiceImpl.java` - MiniMax 集成  
- `RunwayVideoServiceImpl.java` - Runway 集成（可选）  
- `VideoPostProcessingService.java` - 后期流水线  
- `ColorGradingService.java` - 色彩调色  
- `VideoQualityCheckService.java` - 质量检测  
- `ParallelVideoGenerationService.java` - 并发生成  
- `VideoCacheService.java` - 缓存服务  

**前端 (React)**  
- `CameraControlPanel.tsx` - 运镜控制面板  
- `QualitySelector.tsx` - 质量选择器  
- `PostProductionEditor.tsx` - 后期编辑器  
- `ColorGradingPanel.tsx` - 调色面板  
- `StoryboardEditor.tsx` - 故事板编辑器  
- `cinematicPresets.ts` - 预设库  

**数据库**  
- `sv_shot`: camera_type, camera_params, quality_level, ai_model, quality_score  
- `sv_material`: post_processing_config, quality_metrics  

---

## 三、升级必要性评估

### 3.1 建议优先实施（高价值、可复用）

| 项目 | 理由 | 预估工作量 |
|------|------|------------|
| **1. 智能 Prompt 引擎** | 直接提升生成质量，无新依赖 | 2-3 天 |
| **2. 运镜类型扩展** | 用户可选的差异化能力 | 1-2 天 |
| **3. 质量档位选择** | 成本/速度/质量权衡 | 1 天 |
| **4. 前端运镜/质量选择** | 用户可见，体验提升 | 2 天 |

### 3.2 建议中期实施（依赖外部 API）

| 项目 | 理由 | 预估工作量 |
|------|------|------|
| **5. MiniMax 集成** | 国内备用，稳定性好 | 1-2 天 |
| **6. 多模型策略** | 失败自动降级 | 1-2 天 |
| **7. Kling 增强** | mode: max/pro，完整 Prompt | 1 天 |

### 3.3 建议可选/延后（投入大）

| 项目 | 理由 | 预估工作量 |
|------|------|------|
| **8. 后期流水线** | 需 FFmpeg 调色/稳定/降噪 | 3-5 天 |
| **9. 质量检测** | 需 OpenCV/算法 | 2-3 天 |
| **10. 故事板编辑器** | 前端重构 | 3-5 天 |
| **11. 并发/缓存** | 性能优化 | 2-3 天 |

---

## 四、最小可行升级（MVP）建议

若希望**快速见效**，建议先做 Phase 1 的「核心 Prompt + 运镜」部分：

### 4.1 MVP 范围（约 1 周）

1. **CinematicPromptEngine**  
   - 根据 sceneDescription + action 生成更丰富的 Prompt  
   - 无需 GPT-4V，先做简单规则增强  

2. **CameraType 枚举**  
   - 扩展 6–10 种常用运镜（DOLLY_IN, CRANE_UP, ORBIT 等）  
   - 每种对应不同 Prompt 模板  

3. **接入现有流程**  
   - `img2videoBatch` 接收 `cameraType` 参数  
   - 调用 `CinematicPromptEngine.generatePrompt()` 替代固定文案  

4. **前端**  
   - 素材生产页增加运镜下拉选择  

### 4.2 数据库变更（MVP）

```sql
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS camera_type VARCHAR(50);
ALTER TABLE sv_shot ADD COLUMN IF NOT EXISTS quality_level VARCHAR(20);
```

### 4.3 不纳入 MVP 的部分

- MiniMax / Runway 集成（需 API Key）  
- 后期调色 / 稳定 / 降噪  
- 质量自动检测  
- 故事板编辑器  

---

## 五、实施顺序建议

```
┌─────────────────────────────────────────────────────────────┐
│ 第 1 周：Prompt + 运镜（MVP）                                │
│  • CinematicPromptEngine（简化版）                           │
│  • CameraType 枚举 + 接入 img2videoBatch                     │
│  • 前端运镜选择                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 第 2 周：多模型 + 质量档位                                   │
│  • QualityLevel 枚举                                        │
│  • MiniMax 集成（如有 API Key）                              │
│  • MultiModelVideoService 降级策略                           │
│  • 前端质量选择                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 第 3–4 周：后期（按需）                                      │
│  • 调色 LUT / 基础稳定                                      │
│  • 质量检测（可选）                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、依赖与配置

| 依赖 | 当前 | 升级后 | 说明 |
|------|------|--------|------|
| Kling API | ✅ 已有 | 增强 | mode: pro/max |
| MiniMax API | ❌ | 新增 | 需 api-key、group-id |
| Runway API | ❌ | 可选 | 需 api-key |
| FFmpeg | ✅ 已有 | 扩展 | 调色、稳定、降噪 |
| OpenCV | ❌ | 可选 | 质量检测 |
| Redis | ✅ 已有 | 扩展 | 视频缓存 |

---

## 七、总结

| 维度 | 结论 |
|------|------|
| **必须升级** | 智能 Prompt、运镜类型、质量档位（直接影响体验） |
| **建议升级** | 多模型策略、MiniMax 备用（提升稳定性） |
| **可选升级** | 后期流水线、质量检测、故事板（投入大） |
| **建议起点** | MVP：1 周内完成 Prompt + 运镜 + 前端选择 |

**完整文档**：  
- 技术方案：`docs/SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md`  
- 执行清单：`docs/IMPLEMENTATION_CHECKLIST.md`  
- 使用说明：`docs/README_UPGRADE.md`  
