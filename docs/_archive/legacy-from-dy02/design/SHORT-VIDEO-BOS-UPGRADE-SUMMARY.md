# 短视频模块 BOS 集成升级总结

**版本**: v1.0  
**日期**: 2026-03-01  
**状态**: 已完成

---

## 升级内容

### 1. 数据库迁移

**文件**: `sql/shortvideo/migration-bos-production.sql`

- **sv_plan_asset**：新增 `bos_key` 字段（用于 BOS 删除）
- **sv_project**：短视频项目表（对应设计文档 short_video_project）
- **sv_script**：脚本表
- **sv_shot_list**：分镜列表表
- **sv_shot**：分镜详情表（含 keyframe_url、video_url、audio_url 及对应 bos_key）
- **sv_material**：素材库表（含 url、bos_key）

**执行方式**：
```bash
psql -U your_user -d your_db -f sql/shortvideo/migration-bos-production.sql
```

### 2. 后端实体与 API

| 类型 | 路径 | 说明 |
|------|------|------|
| Entity | `module/shortvideo/entity/` | SvProject, SvScript, SvShotList, SvShot, SvMaterial |
| Entity | `module/shortvideo/entity/SvPlanAsset.java` | 新增 bosKey 字段 |
| Util | `module/shortvideo/util/ShortVideoPathHelper.java` | BOS 路径生成工具 |
| Controller | `module/shortvideo/controller/ShortVideoUploadController.java` | 素材上传 API |

### 3. 上传 API 清单

| 接口 | 路径规范 | 用途 |
|------|----------|------|
| `POST /api/v1/short-video/upload/keyframe` | `{userId}/{date}/{projectId}/keyframes/shot_XXX.jpg` | 关键帧图片 |
| `POST /api/v1/short-video/upload/video` | `{userId}/{date}/{projectId}/videos/shot_XXX.mp4` | 视频片段 |
| `POST /api/v1/short-video/upload/audio` | `{userId}/{date}/{projectId}/audios/voice_XXX.mp3` | 配音文件 |
| `POST /api/v1/short-video/upload/thumbnail` | `{userId}/{date}/{projectId}/thumbnails/cover_N.jpg` | 封面图片 |
| `POST /api/v1/short-video/upload/final-video` | `{userId}/{date}/{projectId}/videos/final.mp4` | 成片视频 |
| `POST /api/v1/short-video/upload/reference/character` | `{userId}/references/characters/{characterId}/` | 人物参考图 |
| `POST /api/v1/short-video/upload/reference/scene` | `{userId}/references/scenes/{sceneId}/` | 场景参考图 |

**文件限制**：
- 图片：10MB，支持 jpg/png/gif/webp
- 视频：500MB，支持 mp4/mov/avi/webm
- 音频：50MB，支持 mp3/wav/m4a

### 4. 前端 API

**文件**: `frontend-react/src/api/shortvideo.ts`

新增方法：
- `uploadKeyframe(projectId, shotNumber, file, date?)`
- `uploadVideo(projectId, shotNumber, file, date?)`
- `uploadAudio(projectId, shotNumber, file, date?)`
- `uploadThumbnail(projectId, index, file, date?)`
- `uploadFinalVideo(projectId, file, date?)`
- `uploadCharacterReference(characterId, file)`
- `uploadSceneReference(sceneId, file)`

---

## 依赖

- **BOS 配置**：需在「系统配置」中配置 `storage.bos.*`（endpoint、bucket、accessKey、secretKey、cdnDomain）
- **BosStorageService**：已存在，无需修改

---

## 后续开发建议

1. **Repository + Service**：为 SvProject、SvScript、SvShotList、SvShot、SvMaterial 创建 Repository 和 Service
2. **脚本/分镜 API**：按设计文档实现脚本生成、分镜生成、素材生产等业务 API
3. **AI 集成**：关键帧/视频/配音生成后，调用 BosStorageService.upload 上传到 BOS，返回 CDN URL 写入数据库
4. **前端页面**：ScriptPlanningPage、ShotListDesignPage、MaterialProductionPage 等，使用 `api/shortvideo.ts` 中的上传方法

---

## 参考文档

- [百度云 BOS 存储集成方案](./BAIDU-BOS-STORAGE-INTEGRATION.md)
- [短视频生产系统设计](./SHORT-VIDEO-PRODUCTION-SYSTEM-DESIGN.md)
- [电影级技术补充](./SHORT-VIDEO-CINEMA-GRADE-SUPPLEMENT.md)
