# 策划文档可执行性分析

> **视角**: 代码实现者评估 Claude 策划文档是否到位、能否直接落地编码

**分析日期**: 2026-03-02  
**文档版本**: v3.3（v3.2 分析后策划已升级，附录 A 已覆盖主要适配项）

---

## 一、总体结论

| 维度 | v3.2 评分 | v3.3 评分 | 说明 |
|------|-----------|-----------|------|
| **策划完整性** | 8.5/10 | **9/10** | 新增附录 A、VideoGenerationException、Pika 完整实现 |
| **可直接执行** | 7/10 | **8.5/10** | 附录 A 明确 6 项适配方案，实现歧义大幅减少 |
| **需实现者决策** | 6 处 | **1 处** | ownerId 具体采用哪种方案（附录 A.3 已给选项） |

**结论**: v3.3 策划已到位，附录 A 直接回应此前分析，可直接按 Phase 1 实施。

---

## 二、策划到位之处 ✅

### 2.1 可直接照抄的代码

| 模块 | 文档位置 | 可执行性 |
|------|----------|----------|
| CameraType 枚举 | 4.1.1 | ✅ 完整 Java 代码，复制即用 |
| QualityLevel 枚举 | 4.1.2 | ✅ 完整，含 fromCode、分辨率方法 |
| VideoAspectRatio 枚举 | 4.1.3 | ✅ 完整 |
| generateWithTemplate() | 4.2 | ✅ 规则模板逻辑完整 |
| mapMoodToPrompt() | 4.2 | ✅ 情绪映射完整 |
| generateNegativePrompt() | 4.2 | ✅ 完整 |
| AiVideoProvider 接口 | 4.3.1 | ✅ record 定义完整，含兼容构造 |
| IntelligentModelRouter 核心逻辑 | 4.3.2 | ✅ 内容分析、路由链、降级逻辑完整 |
| KlingVideoProvider 适配 | 4.3.3 | ✅ 包装逻辑清晰 |

### 2.2 文件路径与任务拆分

- 实施清单给出具体路径，如 `src/main/java/.../domain/CameraType.java`
- Phase 1–8 任务拆分清晰，依赖关系可追溯

### 2.3 与现有代码的衔接说明

- 明确「适配现有 KlingVideoServiceImpl」
- 明确「改造 ShortVideoMaterialServiceImpl.img2videoBatch()」
- 标注 Kling 不支持 negative_prompt、aspectRatio、seed

---

## 三、策划需实现者适配的项 ⚠️

### 3.1 AiChatService 不存在 → 用 LlmClient

**策划写法**:
```java
@Resource
private AiChatService aiChatService;
String result = aiChatService.chat(systemPrompt, userInput);
```

**项目现状**: 无 `AiChatService`，有 `LlmClient`。

**实现方式**:
```java
@Resource
private LlmClient llmClient;
@Resource
private AiModelRepository aiModelRepository; // 或 ConfigService 解析模型列表

// 调用方式 (参考 SvShotListServiceImpl)
List<AiModel> models = resolveModels(); // 从配置解析
LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userInput);
String result = resp.success() && resp.content() != null ? resp.content().trim() : null;
```

**建议**: 策划中补充「AiChatService 对应 LlmClient，调用 chatWithFallback」。

---

### 3.2 CinematicKnowledgeService 为 Phase 5 → Phase 1 可选

**策划写法**:
```java
@Resource(name = "cinematicKnowledgeService")
private CinematicKnowledgeService knowledgeService;
// ...
knowledgeService.logGeneration(request, result, ...);
```

**问题**: Phase 1 不包含 CinematicKnowledgeService，直接 `@Resource` 会注入失败。

**实现方式**:
```java
@Autowired(required = false)
private CinematicKnowledgeService knowledgeService;

// 调用处
if (knowledgeService != null) {
    knowledgeService.logGeneration(...);
}
```

**建议**: 策划中注明「Phase 1 可省略知识库记录，或使用 optional 注入」。

---

### 3.3 KlingVideoProvider 缺少 ownerId 传递

**策划写法**:
```java
String videoUrl = klingVideoService.img2videoUrl(
    request.imageUrl(), request.duration(), request.prompt(),
    null  // ownerId
);
```

**项目现状**: `KlingVideoService.img2videoUrl(imageUrl, duration, prompt, ownerId)` 第 4 参数为 ownerId。

**问题**: `VideoGenerationRequest` 无 ownerId，但调用链上游（如 img2videoBatch）有 ownerId。

**实现方式**:
- 方案 A: 在 `VideoGenerationRequest` 中增加 `Long ownerId`（可选）
- 方案 B: 在 `AiVideoProvider.generateVideo()` 的上下文/ThreadLocal 中传递 ownerId
- 方案 C: 对 Kling 的计费/限流不依赖 ownerId 时，暂时传 null

**建议**: 策划中明确 ownerId 的传递路径（请求体 / 上下文 / 可忽略）。

---

### 3.4 任务描述与代码不一致

**实施清单 1.2**:
> 不依赖外部 NLP/Vision 服务，纯模板+规则

**主文档 4.2**:
> FHD/4K 级别可调用 LLM 将中文场景描述精炼为电影级英文 Prompt

**结论**: 任务描述与设计不一致。

**实现建议**: 以主文档为准，SD/HD 用模板，FHD/4K 可选 LLM；任务清单改为「SD/HD 纯模板，FHD/4K 可选 LLM 增强」。

---

### 3.5 运镜数量不一致

- 实施清单: 「24 种运镜」
- 4.1.1 CameraType: 实际为 **23 种**（STATIC 到 PULL_OUT）

**实现建议**: 实现时按枚举为准（23 种），或策划统一改为 23。

---

### 3.6 首尾帧与 Kling 的兼容

**现状**: 已实现 `sv_shot.end_frame_url`，FFmpeg 支持首尾帧。

**策划**: KlingVideoProvider 写 `supportsEndFrame() { return false }`。

**说明**: 当前 Kling API 若仅支持单图，则 `false` 正确；若后续支持首尾帧，需改为 `true` 并传 endFrameUrl。

**建议**: 策划中注明「Kling 当前不支持首尾帧，以官方文档为准」。

---

## 四、策划缺失或模糊的细节

### 4.1 第三方 API 规格

| 模型 | 策划提供 | 缺失 |
|------|----------|------|
| MiniMax | API URL、model 名 | 请求/响应 JSON 结构、错误码 |
| Runway | baseUrl、model | 认证方式、请求体格式 |
| Luma | baseUrl | 首尾帧参数名、格式 |

**建议**: 实现前查阅官方文档，或由策划补充「请求/响应示例」章节。

---

### 4.2 配置结构

策划有 `.env` 示例，但 `application.yml` 中 minimax/runway/luma 的完整结构未给出。

**建议**: 补充各 Provider 的 `@Value` 或 `@ConfigurationProperties` 示例。

---

### 4.3 错误码与异常

- 多模型全部失败时，应返回什么 HTTP 状态码？
- 业务错误码是否沿用 `docs/04-错误码注册表.md`？

**建议**: 策划中约定「视频生成失败」的 status code 与错误码。

---

## 五、实现者需决策的 6 项

| # | 决策点 | 选项 | 建议 |
|---|--------|------|------|
| 1 | ownerId 传递 | A: 加字段 B: 上下文 C: 传 null | 若 Kling 不限流，C 可接受 |
| 2 | Phase 1 是否做知识库 | A: 做 B: 不做，optional 注入 | B，Phase 5 再接入 |
| 3 | LLM 增强 Prompt | A: Phase 1 就做 B: 先纯模板 | B，先跑通主流程 |
| 4 | MiniMax/Runway 接入顺序 | A: Phase 1 全做 B: 先 Kling+FFmpeg | B，再逐步加 |
| 5 | 关键帧 768x1344 | A: Phase 1 改 B: 暂缓 | A，与文档一致 |
| 6 | 运镜数量表述 | 23 还是 24 | 以枚举为准，统一为 23 |

---

## 六、Phase 1 最小可执行清单（实现者视角）

按「策划到位程度」筛选，以下可直接执行：

1. ✅ 创建 CameraType、QualityLevel、VideoAspectRatio
2. ✅ 创建 CinematicPromptEngine（**用 LlmClient 替代 AiChatService**，Phase 1 可先禁用 LLM 分支）
3. ✅ 创建 AiVideoProvider 接口及 VideoGenerationRequest/Result
4. ✅ 创建 KlingVideoProvider（**ownerId 暂传 null**，或按需扩展 Request）
5. ⚠️ 创建 IntelligentModelRouter（**knowledgeService 用 @Autowired(required=false)**）
6. ✅ 创建 MultiModelVideoService（或直接使用 IntelligentModelRouter 的 generateWithFallback）
7. ✅ 改造 img2videoBatch 接入
8. ✅ 关键帧 512→768x1344
9. ✅ 数据库 ALTER、Controller 参数扩展

---

## 七、总结

| 问题 | 答案 |
|------|------|
| 策划是否到位？ | **基本到位**，架构、接口、核心逻辑可支撑实现 |
| 能否直接编码？ | **可以**，Phase 1 约 90% 可直接按文档实现 |
| 主要缺口？ | ① AiChatService→LlmClient ② 知识库可选 ③ ownerId 传递 ④ 第三方 API 细节 |
| 建议？ | 实现前按本文做 6 项决策，并补充 3.1–3.6 的策划说明 |

**策划质量**: 适合作为实施依据，实现时需做少量适配与约定补充。

---

## 八、v3.3 升级后更新 (2026-03-02)

策划已升级至 v3.3，**附录 A. 实施适配注意事项** 已覆盖本分析第三、五、六节的全部 6 项：

| 本分析条款 | 附录 A 对应 | 状态 |
|------------|-------------|------|
| 3.1 AiChatService → LlmClient | A.1 | ✅ 已明确 |
| 3.2 CinematicKnowledgeService 可选 | A.2 | ✅ 已明确 |
| 3.3 ownerId 传递 | A.3 | ✅ 已给 3 种方案 |
| 3.4 任务描述与 LLM 模式 | A.4 | ✅ 已澄清 |
| 3.5 运镜数量 | A.5 | ✅ 已明确 24 |
| 4.1 第三方 API 细节 | A.6 | ✅ 已提供官方文档链接 |

**v3.3 新增**: VideoGenerationException、PikaVideoProvider 完整代码、BPM 检测方案、v3.3 路线图。

**结论**: 策划可执行性已从 7/10 提升至 8.5/10，可直接实施。
