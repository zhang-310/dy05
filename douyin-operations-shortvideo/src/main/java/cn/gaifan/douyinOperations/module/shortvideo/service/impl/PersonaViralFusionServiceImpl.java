package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.HotspotWindowService;
import cn.gaifan.douyinOperations.module.shortvideo.service.PersonaViralFusionService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LF-05 人设融合：变量表 + transcript + 场景描述；多租户仅查询当前用户人设。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonaViralFusionServiceImpl implements PersonaViralFusionService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SvHotTopicRepository hotTopicRepository;
    private final DyPersonaRepository personaRepository;
    private final AiModelRepository aiModelRepository;
    private final ViralVideoService viralVideoService;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private HotspotWindowService hotspotWindowService;

    @Autowired(required = false)
    private ViolationWordService violationWordService;

    @Autowired(required = false)
    private DyProductRepository productRepository;

    @Override
    public List<Map<String, Object>> matchPersonas(Long viralVideoId, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        List<DyPersona> personas = personaRepository.findByOwnerIdAndDeleted(userId, 0);
        if (personas.isEmpty()) {
            return List.of();
        }

        AiModel model = pickDefaultTextModel();
        if (llmClient == null || model == null) {
            return personas.stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("personaId", p.getId());
                m.put("personaName", p.getPersonaName() != null ? p.getPersonaName() : "");
                m.put("matchScore", 0.5);
                m.put("matchReason", "AI 匹配服务不可用");
                return m;
            }).toList();
        }

        StringBuilder personaList = new StringBuilder();
        for (DyPersona p : personas) {
            String desc = p.getDescription() != null ? p.getDescription() : "";
            if (desc.length() > 100) {
                desc = desc.substring(0, 100);
            }
            personaList.append(String.format("- ID=%d, 名称=%s, 描述=%s%n",
                    p.getId(), p.getPersonaName() != null ? p.getPersonaName() : "", desc));
        }

        ViralEvidenceHelper.EvidenceSnapshot evidence = ViralEvidenceHelper.resolveEvidence(
                viral.getDeepAnalysisResult(), viral.getTranscript(), viral.getSceneDescriptions());
        String analysisSnippet = buildStructuredDeepAnalysisSnippet(viral, evidence);
        if (analysisSnippet.length() > 500) {
            analysisSnippet = analysisSnippet.substring(0, 500);
        }

        String prompt = String.format("""
                以下是一个爆款短视频：
                标题: %s
                分析: %s

                以下是可用的主播人设：
                %s

                请为这个爆款视频匹配最合适的人设，输出 JSON 数组（按匹配度排序）：
                [{"personaId": 数字, "matchScore": 0-1, "matchReason": "匹配原因"}]

                匹配维度：内容调性、目标受众、产品品类、表达风格。
                只输出 JSON。
                """,
                viral.getTitle(),
                analysisSnippet,
                personaList);

        try {
            var resp = llmClient.chat(model, "你是人设匹配专家。", prompt);
            if (resp == null || !resp.success() || resp.content() == null) {
                return List.of();
            }
            String raw = stripJsonFence(resp.content());
            JsonNode arr = JSON.readTree(raw);
            if (!arr.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (JsonNode n : arr) {
                long pid = n.path("personaId").asLong(0);
                DyPersona p = personas.stream()
                        .filter(x -> x.getId() != null && x.getId() == pid)
                        .findFirst()
                        .orElse(null);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("personaId", pid);
                m.put("personaName", p != null && p.getPersonaName() != null ? p.getPersonaName() : "");
                m.put("matchScore", n.path("matchScore").asDouble(0));
                m.put("matchReason", n.path("matchReason").asText(""));
                out.add(m);
            }
            return out;
        } catch (Exception e) {
            log.warn("[人设匹配] 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Map<String, Object> generatePersonaFusedScript(Long viralVideoId, Long personaId, String remakeType, Long userId) {
        return generatePersonaFusedScript(viralVideoId, personaId, remakeType, null, userId);
    }

    @Override
    public Map<String, Object> generatePersonaFusedScript(Long viralVideoId, Long personaId, String remakeType,
                                                          PersonaFusionOptions options, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        DyPersona persona = personaRepository.findByIdAndDeleted(personaId, 0).orElse(null);
        if (persona == null || !userId.equals(persona.getOwnerId())) {
            return Map.of("error", "爆款视频或人设不存在");
        }
        AiModel model = pickDefaultTextModel();
        if (llmClient == null || model == null) {
            return Map.of("error", "AI 服务不可用");
        }

        DyProduct product = resolvePersonaFusionProduct(options, userId);
        if (options != null && options.productId() != null && product == null) {
            return Map.of("error", "关联商品不存在或无权访问");
        }

        ViralEvidenceHelper.EvidenceSnapshot evidence = ViralEvidenceHelper.resolveEvidence(
                viral.getDeepAnalysisResult(), viral.getTranscript(), viral.getSceneDescriptions());
        String variableTableHint = "";
        if (viral.getRemakeVariableTable() != null && !viral.getRemakeVariableTable().isBlank()) {
            variableTableHint = "\n\n【可替换变量表（来自深度分析）】\n" + viral.getRemakeVariableTable();
        }
        String transcriptHint = buildTranscriptHint(viral, evidence);
        String sceneHint = buildSceneHint(viral, evidence);
        String pageConstraintHint = buildPersonaFusionConstraintHint(options, product);

        String analysisSnippet = buildStructuredDeepAnalysisSnippet(viral, evidence);
        if (analysisSnippet.length() > 2000) {
            analysisSnippet = analysisSnippet.substring(0, 2000);
        }

        String prompt = String.format("""
                你是短视频二创脚本专家。请基于以下爆款视频的深度分析结果，用指定人设风格进行二次创作。

                【原始爆款】
                标题: %s
                爆款分析: %s
                %s%s%s

                【目标人设】
                名称: %s
                描述: %s
                说话风格: %s
                口癖/标志性表达: （请在脚本中融入人设的标志性表达）

                【二创类型】%s

                %s

                %s

                【二创核心原则】
                - **mustKeep 项必须保留**：保留原视频的结构节奏、情绪曲线、hook 类型
                - **replaceable 项按人设替换**：将原视频的人设/产品/场景/口头禅替换为目标人设的对应元素
                - 若参考素材标记为“推演”，只能把它当作结构假设，不得伪称来自真实 ASR / 真实抽帧
                - 脚本必须口语化、有真实感、像人设本人说的话

                请输出结构化脚本 JSON：
                {
                  "title": "二创标题（15字内，吸引眼球）",
                  "hook": {
                    "duration": "前3秒",
                    "text": "开头台词/画面描述（用人设的语气）",
                    "technique": "悬念/冲突/提问/反转",
                    "keptFrom": "保留了原视频的什么结构"
                  },
                  "body": [
                    {
                      "segment": 1,
                      "duration": "X秒",
                      "text": "台词/旁白（人设风格）",
                      "visual": "画面描述",
                      "cameraAngle": "特写/中景/全景",
                      "replacedVariable": "替换了原视频的什么变量"
                    }
                  ],
                  "cta": {
                    "text": "收尾台词 + CTA（人设风格）",
                    "action": "引导动作（点赞/关注/评论/购买）"
                  },
                  "variableReplacements": [
                    {"variable": "变量名", "original": "原值", "replaced": "替换后的值", "reason": "替换原因"}
                  ],
                  "totalDuration": "预估总时长",
                  "bgmSuggestion": "BGM 风格建议",
                  "personaConsistencyNote": "人设一致性说明",
                  "viralPotentialScore": 0
                }
                只输出 JSON。
                """,
                viral.getTitle(),
                analysisSnippet,
                variableTableHint,
                transcriptHint,
                sceneHint,
                persona.getPersonaName(),
                persona.getDescription() != null ? persona.getDescription() : "",
                persona.getTone() != null ? persona.getTone() : "自然",
                remakeType != null ? remakeType : "form_imitation",
                pageConstraintHint,
                buildComplianceConstraint(userId));

        try {
            var resp = llmClient.chat(model,
                    "你是短视频二创脚本专家，擅长将爆款视频融合主播人设进行二次创作。核心方法：保留爆款结构+替换人设变量。",
                    prompt);
            if (resp != null && resp.success() && resp.content() != null) {
                Map<String, Object> r = new LinkedHashMap<>();
                String scriptText = resp.content();
                r.put("script", scriptText);
                r.put("tokensUsed", resp.tokensUsed());
                r.put("viralVideoId", viralVideoId);
                r.put("personaId", personaId);
                r.put("remakeType", remakeType != null ? remakeType : "form_imitation");
                r.put("usedVariableTable", viral.getRemakeVariableTable() != null);
                attachPersonaFusionConstraints(r, options, product);
                attachViolationCheck(r, scriptText, userId);
                return r;
            }
        } catch (Exception e) {
            log.error("[人设融合生成] 失败: {}", e.getMessage());
        }
        return Map.of("error", "生成失败");
    }

    private DyProduct resolvePersonaFusionProduct(PersonaFusionOptions options, Long userId) {
        if (options == null || options.productId() == null || productRepository == null) {
            return null;
        }
        return productRepository.findByIdAndDeleted(options.productId(), 0)
                .filter(product -> userId != null && userId.equals(product.getUserId()))
                .orElse(null);
    }

    private String buildPersonaFusionConstraintHint(PersonaFusionOptions options, DyProduct product) {
        if (options == null) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        if (product != null) {
            lines.add("【关联商品】");
            lines.add("- 商品ID: " + product.getId());
            lines.add("- 商品名称: " + safeText(product.getProductName()));
            if (StringUtils.hasText(product.getProductCategory())) {
                lines.add("- 商品品类: " + product.getProductCategory());
            }
            if (StringUtils.hasText(product.getAiSellingPoints())) {
                lines.add("- AI 卖点: " + truncate(product.getAiSellingPoints(), 500));
            } else if (StringUtils.hasText(product.getDescription())) {
                lines.add("- 商品描述: " + truncate(product.getDescription(), 500));
            }
            if (product.getPrice() != null) {
                lines.add("- 商品价格: " + product.getPrice());
            }
            lines.add("要求：脚本必须自然植入该商品卖点，不得硬广堆砌。");
        }
        if (StringUtils.hasText(options.topic())) {
            lines.add("【话题/场景约束】" + truncate(options.topic(), 200));
        }
        if (options.durationSeconds() != null && options.durationSeconds() > 0) {
            lines.add("【目标时长】约 " + options.durationSeconds() + " 秒，请按该时长控制段落数量和节奏。");
        }
        if (options.count() != null && options.count() > 1) {
            lines.add("【生成数量】请在 JSON 中增加 alternatives 数组，给出 " + options.count() + " 个差异化备选方案。");
        }
        if (lines.isEmpty()) {
            return "";
        }
        return String.join("\n", lines);
    }

    private void attachPersonaFusionConstraints(Map<String, Object> result, PersonaFusionOptions options, DyProduct product) {
        if (options == null) {
            return;
        }
        Map<String, Object> constraints = new LinkedHashMap<>();
        if (product != null) {
            constraints.put("productId", product.getId());
            constraints.put("productName", product.getProductName() != null ? product.getProductName() : "");
            constraints.put("productCategory", product.getProductCategory() != null ? product.getProductCategory() : "");
        }
        if (StringUtils.hasText(options.topic())) {
            constraints.put("topic", options.topic().trim());
        }
        if (options.durationSeconds() != null) {
            constraints.put("durationSeconds", options.durationSeconds());
        }
        if (options.count() != null) {
            constraints.put("count", options.count());
        }
        if (!constraints.isEmpty()) {
            result.put("constraintsApplied", constraints);
        }
    }

    private String safeText(String text) {
        return text != null ? text : "";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    @Override
    public Map<String, Object> generateHotspotFusedScript(Long hotTopicId, Long personaId, Long productId, Long userId) {
        SvHotTopic topic = hotTopicRepository.findById(hotTopicId).orElse(null);
        DyPersona persona = personaRepository.findByIdAndDeleted(personaId, 0).orElse(null);
        if (topic == null || persona == null || !userId.equals(persona.getOwnerId())) {
            return Map.of("error", "热点或人设不存在");
        }
        AiModel model = pickDefaultTextModel();
        if (llmClient == null || model == null) {
            return Map.of("error", "AI 服务不可用");
        }

        String windowAdvice = "";
        if (hotspotWindowService != null && topic.getCreateTime() != null) {
            var tier = hotspotWindowService.classify(topic.getCreateTime());
            windowAdvice = "当前热点处于【" + tier.getLabel() + "】阶段：" + tier.getStrategy();
        }

        String productHint = "";
        if (productId != null) {
            productHint = "\n\n【植入产品】ID=" + productId + "（请在脚本中自然植入该产品）";
        }

        String prompt = String.format("""
                你是热点二创专家。请将以下热点话题融合主播人设，创作一条短视频脚本。

                【热点话题】
                标题: %s
                热度: %s
                %s

                【目标人设】
                名称: %s
                描述: %s
                %s

                创作要求：
                1. 借势热点但不硬蹭，用人设风格自然切入
                2. 前3秒必须有强 hook
                3. 如有产品植入，要自然不突兀
                4. 口语化、有真实感

                %s

                输出 JSON 格式（同 generatePersonaFusedScript 的结构，含 variableReplacements）。
                只输出 JSON。
                """,
                topic.getTitle(),
                topic.getHeatScore(),
                windowAdvice,
                persona.getPersonaName(),
                persona.getDescription() != null ? persona.getDescription() : "",
                productHint,
                buildComplianceConstraint(userId));

        try {
            var resp = llmClient.chat(model, "你是热点话题二创专家。", prompt);
            if (resp != null && resp.success() && resp.content() != null) {
                Map<String, Object> r = new LinkedHashMap<>();
                String scriptText = resp.content();
                r.put("script", scriptText);
                r.put("hotTopicId", hotTopicId);
                r.put("personaId", personaId);
                r.put("productId", productId != null ? productId : 0L);
                r.put("hotspotTier", windowAdvice);
                attachViolationCheck(r, scriptText, userId);
                return r;
            }
        } catch (Exception e) {
            log.error("[热点融合生成] 失败: {}", e.getMessage());
        }
        return Map.of("error", "生成失败");
    }

    /**
     * 构建合规约束 prompt 片段：注入禁用词列表，让 LLM 在生成阶段就规避违规词。
     */
    private String buildComplianceConstraint(Long userId) {
        if (violationWordService == null) {
            return "";
        }
        try {
            var words = violationWordService.listActive();
            if (words == null || words.isEmpty()) {
                return "";
            }
            // 最多取前 80 个禁用词，避免 prompt 过长
            List<String> forbidden = words.stream()
                    .map(w -> w.getWord() != null ? w.getWord() : "")
                    .filter(w -> !w.isBlank())
                    .limit(80)
                    .toList();
            if (forbidden.isEmpty()) {
                return "";
            }
            return "【合规约束 — 严禁使用以下词汇】\n"
                    + "脚本中不得出现以下禁用词或其变体，违反将无法通过审核：\n"
                    + String.join("、", forbidden) + "\n";
        } catch (Exception e) {
            log.debug("[合规前置] 获取禁用词失败: {}", e.getMessage());
            return "";
        }
    }

    /** 优先 deepAnalysisResult 列，抽取 evidence / viralHypotheses / remakeVariableTable / structure。 */
    private String buildStructuredDeepAnalysisSnippet(SvViralVideo viral, ViralEvidenceHelper.EvidenceSnapshot evidence) {
        String raw = StringUtils.hasText(viral.getDeepAnalysisResult())
                ? viral.getDeepAnalysisResult()
                : viral.getAnalysisResult();
        StringBuilder sb = new StringBuilder();
        appendEvidenceSnippet(sb, evidence);
        if (!StringUtils.hasText(raw)) {
            String summary = sb.toString();
            return StringUtils.hasText(summary) ? summary : "无";
        }
        try {
            JsonNode root = JSON.readTree(raw);
            if (root.has("viralHypotheses")) {
                sb.append("【爆款因子】\n").append(root.get("viralHypotheses").toString()).append("\n");
            }
            if (root.has("remakeVariableTable")) {
                sb.append("【变量表】\n").append(root.get("remakeVariableTable").toString()).append("\n");
            }
            if (root.has("structure")) {
                sb.append("【叙事结构】\n").append(root.get("structure").toString()).append("\n");
            }
            String s = sb.toString();
            if (StringUtils.hasText(s)) {
                return s.length() > 2000 ? s.substring(0, 2000) : s;
            }
        } catch (Exception e) {
            log.debug("[人设融合] 深度 JSON 解析降级为截断原文: {}", e.getMessage());
        }
        if (sb.length() > 0) {
            sb.append("【分析原文】\n");
            sb.append(raw.length() > 800 ? raw.substring(0, 800) : raw);
            return sb.toString();
        }
        return raw.length() > 800 ? raw.substring(0, 800) : raw;
    }

    private String buildTranscriptHint(SvViralVideo viral, ViralEvidenceHelper.EvidenceSnapshot evidence) {
        if (!StringUtils.hasText(viral.getTranscript())) {
            return "";
        }
        return "\n\n【" + transcriptHintTitle(evidence) + "】\n"
                + viral.getTranscript().substring(0, Math.min(500, viral.getTranscript().length()));
    }

    private String buildSceneHint(SvViralVideo viral, ViralEvidenceHelper.EvidenceSnapshot evidence) {
        if (!StringUtils.hasText(viral.getSceneDescriptions())) {
            return "";
        }
        return "\n\n【" + sceneHintTitle(evidence) + "】\n"
                + viral.getSceneDescriptions().substring(0, Math.min(500, viral.getSceneDescriptions().length()));
    }

    private String transcriptHintTitle(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(evidence.transcriptLevel())) {
            case "empirical" -> "原视频口播文案（实证转写）";
            case "inferred" -> "原视频口播文案（推演稿，非 ASR 实录）";
            default -> "原视频口播文案";
        };
    }

    private String sceneHintTitle(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(evidence.sceneLevel())) {
            case "empirical" -> "原视频场景描述（实证画面拆解）";
            case "inferred" -> "原视频场景描述（推演场景，非真实抽帧）";
            default -> "原视频场景描述";
        };
    }

    private void appendEvidenceSnippet(StringBuilder sb, ViralEvidenceHelper.EvidenceSnapshot evidence) {
        if (!evidence.hasDisclosure()) {
            return;
        }
        sb.append("【证据口径】\n");
        appendEvidenceSnippetLine(sb, "总体", ViralEvidenceHelper.evidenceDisplayLabel(evidence.overallLevel()));
        appendEvidenceSnippetLine(sb, "口播", evidencePromptNote(evidence.transcriptLevel(), "实证转写", "推演稿，非 ASR 实录"));
        appendEvidenceSnippetLine(sb, "场景", evidencePromptNote(evidence.sceneLevel(), "实证画面拆解", "推演场景，非真实抽帧"));
        appendEvidenceSnippetLine(sb, "评论", evidencePromptNote(evidence.commentLevel(), "实证样本", "推演/弱证据"));
        if (Boolean.TRUE.equals(evidence.hasCommentSamples())) {
            appendEvidenceSnippetLine(sb, "评论样本", "已采集");
        } else if (Boolean.FALSE.equals(evidence.hasCommentSamples())) {
            appendEvidenceSnippetLine(sb, "评论样本", "未采集");
        }
        if (evidence.hasInferenceRisk()) {
            sb.append("注意：带“推演”标签的字段只能视为结构假设，不得当作真实 ASR/抽帧事实。\n");
        }
    }

    private void appendEvidenceSnippetLine(StringBuilder sb, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        sb.append("- ").append(label).append("=").append(value).append("\n");
    }

    private String evidencePromptNote(String level, String empiricalText, String inferredText) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(level)) {
            case "empirical" -> "实证（" + empiricalText + "）";
            case "inferred" -> "推演（" + inferredText + "）";
            case "missing" -> "缺失";
            default -> null;
        };
    }

    private void attachViolationCheck(Map<String, Object> result, String scriptText, Long userId) {
        if (violationWordService == null || scriptText == null || scriptText.isBlank()) {
            return;
        }
        try {
            ViolationCheckResultVO check = violationWordService.check(scriptText, "video", userId);
            result.put("violationCheck", check);
        } catch (Exception e) {
            log.warn("[融合脚本] 合规检测跳过: {}", e.getMessage());
        }
    }

    private AiModel pickDefaultTextModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        return models.isEmpty() ? null : models.get(0);
    }

    private static String stripJsonFence(String content) {
        if (content == null) {
            return "";
        }
        String t = content.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) {
                t = t.substring(nl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }
}
