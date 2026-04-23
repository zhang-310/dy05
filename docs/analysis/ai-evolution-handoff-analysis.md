# AI 进化引擎移交逻辑分析

**日期**: 2026-04-21  
**问题**: 为什么爆款拆解没有移交到 AI 进化引擎？

---

## 问题现象

用户触发爆款拆解后，视频分析没有进入 AI 进化引擎（`EvolutionService`），而是直接走了深度拆解服务（`ViralVideoDeepAnalysisService`）。

---

## 核心代码分析

### 触发拆解的入口

**文件**: `ViralVideoServiceImpl.java:164-187`

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void triggerAnalysis(Long id, Long userId) {
    SvViralVideo viral = getViralVideo(id, userId);

    // 若仍关联站内 DouyinVideo（旧链路），可走进化引擎爆款拆解
    if (viral.getSourceVideoId() != null) {
        DouyinVideo video = douyinVideoRepository.findById(viral.getSourceVideoId()).orElse(null);
        if (video != null) {
            evolutionService.triggerViralAnalysis(viral.getSourceVideoId(), userId, video.getAccountId());
            log.info("触发爆款分析(进化引擎): viralId={}, sourceVideoId={}", id, viral.getSourceVideoId());
            return;  // ← 走进化引擎，提前返回
        }
    }

    // 账号采集 / 爆款库主体：走 LF-05 深度拆解（写入 deepAnalysisResult 等），与前端「拆解」一致
    if (viralVideoDeepAnalysisService != null) {
        viralVideoDeepAnalysisService.startDeepAnalyze(id, userId);
        log.info("已提交短视频深度拆解: viralId={}", id);
        return;  // ← 走深度拆解，提前返回
    }

    analyzeViralWithLlm(viral);  // ← 回退：简单 LLM 分析
}
```

---

## 关键判断条件

### 条件 1: `viral.getSourceVideoId() != null`

**判断逻辑**：
- 如果 `source_video_id` 字段有值 → 走进化引擎（旧链路）
- 如果 `source_video_id` 字段为空 → 走深度拆解（新链路）

### 数据库字段

**表**: `sv_viral_video`

```sql
CREATE TABLE IF NOT EXISTS sv_viral_video (
    id                   BIGSERIAL PRIMARY KEY,
    owner_id             BIGINT,
    source_video_id      BIGINT,        -- ← 关键字段：关联 dy_video 表
    douyin_video_id      VARCHAR(128),  -- 抖音视频 ID（字符串）
    title                VARCHAR(512),
    video_url            VARCHAR(512),
    -- ... 其他字段
);
```

**Entity**: `SvViralVideo.java:25-26`

```java
@Column(name = "source_video_id")
private Long sourceVideoId;  // ← 关联站内 DouyinVideo 表的 ID
```

---

## 两条链路对比

### 旧链路：进化引擎（EvolutionService）

**触发条件**: `source_video_id` 有值

**流程**:
```
1. 查询 dy_video 表（DouyinVideo）
2. 创建 ai_viral_analysis 记录
3. 异步执行 LLM 分析
4. 写入 ai_viral_analysis 表
5. 可选：入队到 ai_index_queue（知识库索引）
```

**特点**:
- ✅ 与知识库集成（可索引到知识库）
- ✅ 有独立的 `ai_viral_analysis` 表
- ✅ 支持进化引擎的统计和报告
- ❌ 依赖 `dy_video` 表（需要先同步抖音视频）
- ❌ 分析结果不在 `sv_viral_video` 表

**代码**: `EvolutionServiceImpl.java:93-110`

```java
@Override
@Transactional(rollbackFor = Exception.class)
public long triggerViralAnalysis(Long videoId, Long ownerId, Long accountId) {
    return viralRepo.findByVideoIdAndDeleted(videoId, 0).map(AiViralAnalysis::getId).orElseGet(() -> {
        DouyinVideo video = videoRepository.findByIdAndDeleted(videoId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "视频不存在"));

        AiViralAnalysis entity = new AiViralAnalysis();
        entity.setVideoId(videoId);  // ← 关联 dy_video.id
        entity.setOwnerId(ownerId);
        entity.setAccountId(accountId);
        entity.setViewCount(video.getViewCount());
        entity.setStatus(0);
        long savedId = viralRepo.save(entity).getId();

        // 异步执行 AI 分析
        asyncViralAnalysis(savedId, video);
        return savedId;
    });
}
```

### 新链路：深度拆解（ViralVideoDeepAnalysisService）

**触发条件**: `source_video_id` 为空

**流程**:
```
1. 直接基于 sv_viral_video 记录
2. 下载视频（yt-dlp）
3. ASR 转写 + 场景检测
4. BOS 上传（视频+关键帧）
5. LLM 深度拆解（多轮或单次）
6. 写入 sv_viral_video 表（deep_analysis_result）
```

**特点**:
- ✅ 不依赖 `dy_video` 表（独立运行）
- ✅ 支持视频下载和本地分析
- ✅ 结果直接写入 `sv_viral_video` 表
- ✅ 前端可直接展示（videoBosUrl、keyframeBosUrls）
- ❌ 不与知识库集成（不入队 ai_index_queue）
- ❌ 不在进化引擎统计中

**代码**: `ViralVideoDeepAnalysisServiceImpl.java:48-73`

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> startDeepAnalyze(Long viralVideoId, Long userId) {
    SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
    
    // 设置状态为 processing
    viral.setDeepAnalyzeStatus("processing");
    viral.setDeepAnalyzeStartedAt(new Timestamp(System.currentTimeMillis()));
    viralVideoRepository.save(viral);

    // 异步执行深度拆解
    deepAnalyzeAsyncRunner.runDeepAnalyzeAsync(viralVideoId, userId);

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("taskId", viralVideoId);
    m.put("status", "processing");
    return m;
}
```

---

## 为什么没有移交进化？

### 根本原因

**`source_video_id` 字段为空**

当前爆款视频的来源：
1. **账号采集**（`AccountVideoCollectService`）
2. **手动收藏**（前端点击"收藏"按钮）
3. **推荐爆款**（平台推荐）

这些来源都**不会设置 `source_video_id`**，因为它们不是从 `dy_video` 表同步来的。

### 数据流对比

#### 旧链路（会移交进化）

```
抖音 API 同步
    ↓
写入 dy_video 表（id=123）
    ↓
创建 sv_viral_video（source_video_id=123）
    ↓
触发拆解 → 检测到 source_video_id=123
    ↓
走进化引擎（EvolutionService）
```

#### 新链路（不会移交进化）

```
账号采集 / 手动收藏
    ↓
直接写入 sv_viral_video（source_video_id=NULL）
    ↓
触发拆解 → 检测到 source_video_id=NULL
    ↓
走深度拆解（ViralVideoDeepAnalysisService）
```

---

## 影响分析

### 不移交进化的影响

1. **知识库不更新**
   - 深度拆解结果不会入队到 `ai_index_queue`
   - 知识库不会索引这些爆款视频的分析结果
   - RAG 检索时无法召回这些知识

2. **进化引擎统计缺失**
   - `/api/v1/evolution/stats` 不包含这些拆解
   - 进化引擎报告中看不到这些数据

3. **数据分散**
   - 旧链路：`ai_viral_analysis` 表
   - 新链路：`sv_viral_video.deep_analysis_result` 字段
   - 两套数据无法统一查询

### 深度拆解的优势

1. **独立运行**
   - 不依赖 `dy_video` 表
   - 支持任意来源的视频（抖音、快手、小红书等）

2. **功能更强**
   - 视频下载 + ASR 转写
   - 场景检测 + 关键帧提取
   - BOS 上传（可播放）
   - 多轮结构化拆解

3. **前端友好**
   - 结果直接在 `sv_viral_video` 表
   - 无需跨表查询
   - 支持实时进度展示

---

## 解决方案

### 方案 1: 深度拆解完成后入队知识库（推荐）

**思路**: 在深度拆解完成后，将结果入队到 `ai_index_queue`

**实现位置**: `ViralDeepAnalyzeExecutor.java:1322-1338`

```java
private void persistDeepAnalysisSuccess(SvViralVideo viral, String mergedJson, ...) {
    viral.setDeepAnalysisResult(mergedJson);
    viral.setDeepAnalyzeStatus("completed");
    viral.setDeepAnalyzeFinishedAt(new Timestamp(System.currentTimeMillis()));
    viralVideoRepository.save(viral);
    
    // ← 新增：入队知识库
    if (evolutionService != null) {
        try {
            String content = buildKnowledgeContent(viral, mergedJson);
            evolutionService.enqueueIndex(
                "viral_video",           // sourceType
                viral.getId(),           // sourceId
                content,                 // content
                5,                       // priority
                viral.getTargetKbId()    // targetKbId（可选）
            );
            log.info("[深度分析] 已入队知识库: viralId={}", viral.getId());
        } catch (Exception e) {
            log.warn("[深度分析] 知识库入队失败: viralId={}, error=", viral.getId(), e.getMessage());
        }
    }
}

private String buildKnowledgeContent(SvViralVideo viral, String deepAnalysisJson) {
    // 构建知识库内容（标题+口播+拆解结论）
    StringBuilder sb = new StringBuilder();
    sb.append("【爆款视频】").append(viral.getTitle()).append("\n\n");
    
    if (viral.getTranscript() != null) {
        sb.append("【口播文案】\n").append(viral.getTranscript()).append("\n\n");
    }
    
    if (deepAnalysisJson != null) {
        try {
            JsonNode json = JSON.readTree(deepAnalysisJson);
            if (json.has("viralHypotheses")) {
                sb.append("【爆款假设】\n").append(json.get("viralHypotheses").toString()).append("\n\n");
            }
            if (json.has("remakeVariableTable")) {
                sb.append("【二创变量表】\n").append(json.get("remakeVariableTable").toString()).append("\n");
            }
        } catch (Exception e) {
            log.debug("解析 deepAnalysisJson 失败: {}", e.getMessage());
        }
    }
    
    return sb.toString();
}
```

**优点**:
- ✅ 最小改动
- ✅ 保留深度拆解的所有功能
- ✅ 知识库可以索引深度拆解结果
- ✅ 不影响现有流程

**缺点**:
- ❌ 进化引擎统计仍然缺失（因为没有写 `ai_viral_analysis` 表）

### 方案 2: 深度拆解完成后同步到进化引擎

**思路**: 深度拆解完成后，创建 `ai_viral_analysis` 记录

**实现位置**: `ViralDeepAnalyzeExecutor.java:1322-1338`

```java
private void persistDeepAnalysisSuccess(SvViralVideo viral, String mergedJson, ...) {
    viral.setDeepAnalysisResult(mergedJson);
    viral.setDeepAnalyzeStatus("completed");
    viralVideoRepository.save(viral);
    
    // ← 新增：同步到进化引擎
    if (evolutionService != null && viral.getSourceVideoId() == null) {
        try {
            // 从 deepAnalysisResult 提取字段
            JsonNode json = JSON.readTree(mergedJson);
            String report = extractField(json, "report", "");
            String successFactors = extractField(json, "viralHypotheses.emotionTrigger", "");
            String replicableMethods = extractField(json, "remakeVariableTable", "");
            Integer qualityScore = extractField(json, "viralScore", 0);
            
            // 创建 ai_viral_analysis 记录（不触发异步分析）
            createViralAnalysisRecord(viral, report, successFactors, replicableMethods, qualityScore);
            
            log.info("[深度分析] 已同步到进化引擎: viralId={}", viral.getId());
        } catch (Exception e) {
            log.warn("[深度分析] 进化引擎同步失败: viralId={}, error={}", viral.getId(), e.getMessage());
        }
    }
}
```

**优点**:
- ✅ 进化引擎统计完整
- ✅ 知识库可以索引
- ✅ 数据统一在 `ai_viral_analysis` 表

**缺点**:
- ❌ 数据冗余（`sv_viral_video` 和 `ai_viral_analysis` 都有）
- ❌ 需要维护两份数据的一致性

### 方案 3: 统一到深度拆解，废弃旧链路

**思路**: 所有爆款拆解都走深度拆解，进化引擎只负责知识库索引

**改动**:
1. 修改 `ViralVideoServiceImpl.triggerAnalysis()`，移除 `source_video_id` 判断
2. 所有拆解都走 `ViralVideoDeepAnalysisService`
3. 进化引擎改为从 `sv_viral_video` 表读取数据

**优点**:
- ✅ 统一链路，代码简洁
- ✅ 功能最强（视频下载、ASR、场景检测）
- ✅ 前端体验最好

**缺点**:
- ❌ 改动较大
- ❌ 需要迁移历史数据
- ❌ 需要重构进化引擎统计逻辑

---

## 推荐方案

### 短期（立即可用）：方案 1

在深度拆解完成后，将结果入队到知识库：

```java
// ViralDeepAnalyzeExecutor.java
private void persistDeepAnalysisSuccess(SvViralVideo viral, String mergedJson, ...) {
    // ... 现有代码 ...
    
    // 入队知识库
    if (evolutionService != null) {
        String content = buildKnowledgeContent(viral, mergedJson);
        evolutionService.enqueueIndex("viral_video", viral.getId(), content, 5, null);
    }
}
```

**工作量**: 1-2 小时  
**风险**: 低  
**收益**: 知识库可以索引深度拆解结果

### 长期（架构优化）：方案 3

统一到深度拆解，重构进化引擎：

1. 所有拆解走深度拆解
2. 进化引擎从 `sv_viral_video` 读取数据
3. 废弃 `ai_viral_analysis` 表（或改为历史归档）

**工作量**: 1-2 周  
**风险**: 中  
**收益**: 架构统一，功能最强

---

## 配置检查

### 确认当前使用的链路

```sql
-- 查看 source_video_id 分布
SELECT 
  COUNT(*) AS total,
  COUNT(source_video_id) AS has_source_video_id,
  COUNT(*) - COUNT(source_video_id) AS no_source_video_id
FROM sv_viral_video
WHERE deleted = 0;

-- 查看最近的拆解记录
SELECT 
  id,
  title,
  source_video_id,
  deep_analyze_status,
  deep_analysis_result IS NOT NULL AS has_deep_result
FROM sv_viral_video
WHERE deleted = 0
ORDER BY create_time DESC
LIMIT 10;
```

### 查看进化引擎记录

```sql
-- 查看 ai_viral_analysis 表
SELECT 
  COUNT(*) AS total,
  COUNT(CASE WHEN status = 1 THEN 1 END) AS completed,
  COUNT(CASE WHEN status = 0 THEN 1 END) AS pending,
  COUNT(CASE WHEN status = 2 THEN 1 END) AS failed
FROM ai_viral_analysis
WHERE deleted = 0;

-- 查看最近的进化引擎分析
SELECT 
  id,
  video_id,
  status,
  quality_score,
  create_time
FROM ai_viral_analysis
WHERE deleted = 0
ORDER BY create_time DESC
LIMIT 10;
```

---

## 总结

### 核心问题

**爆款视频的 `source_video_id` 字段为空**，导致触发拆解时走了深度拆解链路，而不是进化引擎链路。

### 两条链路

1. **旧链路（进化引擎）**: 依赖 `dy_video` 表，写入 `ai_viral_analysis`，可入队知识库
2. **新链路（深度拆解）**: 独立运行，写入 `sv_viral_video.deep_analysis_result`，功能更强

### 推荐方案

**短期**: 在深度拆解完成后入队知识库（方案 1）  
**长期**: 统一到深度拆解，重构进化引擎（方案 3）

### 立即行动

如果需要知识库索引深度拆解结果，可以立即实施方案 1，只需在 `ViralDeepAnalyzeExecutor.persistDeepAnalysisSuccess()` 中添加 10 行代码。
