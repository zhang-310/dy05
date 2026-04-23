package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.config.BusinessParamConfig;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryCausalEngine;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.guiguiya.client.GuiguiyaHotClient;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.ContentMaterialService;
import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import cn.gaifan.douyinOperations.module.live.service.EmotionCurveEngine;
import cn.gaifan.douyinOperations.module.live.service.LivePromptBuilder;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptPromptService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播话术「发给 LLM 的 user prompt」统一拼接：与 {@link LiveScriptGenerationServiceImpl#generateWithLlm} 同源，
 * 供同步生成与 {@link LiveScriptStreamServiceImpl} 流式槽位共用（素材 / 效果反馈 / 情绪曲线 / RAG）。
 */
@Service
public class LiveScriptLlmUserPromptComposer {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptLlmUserPromptComposer.class);

    @Resource private LivePromptBuilder promptBuilder;
    @Resource private LiveScriptPromptService liveScriptPromptService;
    @Resource private LiveScriptRepository scriptRepository;
    @Resource private DyProductRepository productRepository;
    @Autowired(required = false) private ContentMaterialService contentMaterialService;
    @Autowired(required = false) private EmotionCurveEngine emotionCurveEngine;
    @Autowired(required = false) private BusinessParamConfig businessParamConfig;
    @Autowired(required = false) private GuiguiyaHotClient guiguiyaHotClient;
    @Autowired(required = false) private LivePromptContextBuilderImpl livePromptContextBuilder;
    @Autowired(required = false) private CrossSessionLearningService crossSessionLearningService;
    @Autowired(required = false) private IndustryCausalEngine causalEngine;
    @Autowired(required = false) private cn.gaifan.douyinOperations.module.live.service.LivePromptAbTestService promptAbTestService;

    @Value("${app.live.generation.causal-enrichment:false}")
    private boolean causalEnrichmentEnabled;

    public record AugmentedUserPrompt(String userPrompt, List<KnowledgeBaseService.SearchResult> ragRefs) {
        /** 兼容旧调用（无变体 ID） */
        public AugmentedUserPrompt(String userPrompt, List<KnowledgeBaseService.SearchResult> ragRefs, Long _ignored) {
            this(userPrompt, ragRefs);
        }
    }

    /**
     * 含 Prompt A/B 变体信息的扩展结果
     */
    public record AugmentedUserPromptWithVariant(
            String userPrompt,
            List<KnowledgeBaseService.SearchResult> ragRefs,
            /** 命中的 A/B 变体 ID（null 表示走默认 prompt） */
            Long abVariantId
    ) {}

    /**
     * 从 buildPrompt 起至 RAG 注入完毕，与 {@code generateWithLlm} 内该段逻辑一致（不含 LLM 调用）。
     */
    public AugmentedUserPrompt augmentAfterBasePrompt(String scriptType, LiveSession session, DyPersona persona, LiveAiGenerateVO vo) {
        var result = augmentAfterBasePromptWithVariant(scriptType, session, persona, vo);
        return new AugmentedUserPrompt(result.userPrompt(), result.ragRefs());
    }

    /**
     * 含 A/B 变体信息的版本（供生成后归因记录使用）
     */
    public AugmentedUserPromptWithVariant augmentAfterBasePromptWithVariant(String scriptType, LiveSession session, DyPersona persona, LiveAiGenerateVO vo) {
        if (vo == null) {
            vo = new LiveAiGenerateVO();
        }
        String prompt = promptBuilder.buildPrompt(scriptType, session, persona, vo);

        if (contentMaterialService != null && vo.getMaterialType() != null && !vo.getMaterialType().isBlank()) {
            String materialPrompt = contentMaterialService.buildMaterialPrompt(vo.getMaterialType(), null);
            if (materialPrompt != null && !materialPrompt.isBlank()) {
                prompt = prompt + "\n" + materialPrompt;
            }
        }

        String feedbackHint = buildEffectivenessFeedbackHint(session.getId(), scriptType);
        if (feedbackHint != null && !feedbackHint.isBlank()) {
            prompt = prompt + "\n" + feedbackHint;
        }

        // 因果引擎增强提示（可选）：将因果推理结果注入 prompt，提升"可解释建议"质量
        if (causalEnrichmentEnabled && causalEngine != null && vo != null) {
            String causalHint = buildCausalEnrichmentHint(scriptType, session, persona, vo);
            if (causalHint != null && !causalHint.isBlank()) {
                prompt = prompt + "\n" + causalHint;
            }
        }

        if (emotionCurveEngine != null && vo.getIpType() != null) {
            int totalSlots = (int) scriptRepository.countBySessionIdAndDeleted(session.getId(), 0);
            int currentSlot = vo.getSlotIndex() != null ? vo.getSlotIndex() : (totalSlots > 0 ? totalSlots : 0);
            int total = Math.max(totalSlots, 1);
            String emotionSegment = emotionCurveEngine.buildEmotionPromptSegment(vo.getIpType(), currentSlot, total);
            if (!emotionSegment.isBlank()) {
                prompt = prompt + "\n" + emotionSegment;
            }
        }

        boolean useRag = vo.getUseKbRef() == null || Boolean.TRUE.equals(vo.getUseKbRef());
        DyProduct productForRag = vo.getProductId() != null && scriptTypeUsesProductForRag(scriptType)
                ? productRepository.findById(vo.getProductId()).orElse(null) : null;
        LiveScriptPromptService.RagContextResult ragResult = useRag
                ? liveScriptPromptService.buildRagContext(session.getUserId(), productForRag, scriptType,
                promptBuilder.resolveStyle(vo), prompt.length(), vo.getRequirement()) : null;
        List<KnowledgeBaseService.SearchResult> ragRefs = ragResult != null ? ragResult.refs() : List.of();
        if (ragResult != null && ragResult.xml() != null && !ragResult.xml().isBlank()) {
            prompt = prompt + "\n\n" + ragResult.xml() + "\n请参考以上案例的表达方式，生成原创话术。\n";
        }

        // P0-2: 竞品差异化 — 从同品类洞察生成差异化话术方向
        if (livePromptContextBuilder != null) {
            String category = resolveProductCategory(session, vo);
            String competitorCtx = livePromptContextBuilder.buildCompetitorDifferentiationContext(category);
            if (StringUtils.hasText(competitorCtx)) {
                prompt = prompt + "\n\n### 竞品差异化方向\n" + competitorCtx;
            }
        }

        // P0-3: 受众画像 — 从人设 targetAudience 字段注入粉丝画像
        if (livePromptContextBuilder != null && persona != null
                && StringUtils.hasText(persona.getTargetAudience())) {
            String audienceCtx = livePromptContextBuilder.buildAudienceProfileContext(persona.getTargetAudience());
            if (StringUtils.hasText(audienceCtx)) {
                prompt = prompt + "\n\n" + audienceCtx;
            }
        }

        // P0-4: 跨场次学习记忆 — 注入历史场次的学习洞察
        if (crossSessionLearningService != null && session.getUserId() != null) {
            String liveFormat = session.getLiveFormat();
            String crossCtx = crossSessionLearningService.loadCrossSessionContext(session.getUserId(), liveFormat);
            if (StringUtils.hasText(crossCtx)) {
                prompt = prompt + "\n\n" + crossCtx;
            }
        }
        if (businessParamConfig != null && businessParamConfig.getLiveInventory() != null
                && businessParamConfig.getLiveInventory().isPromptHintEnabled()
                && productForRag != null && productForRag.getInventory() != null) {
            long inv = productForRag.getInventory();
            var li = businessParamConfig.getLiveInventory();
            if (inv <= li.getCriticalStockThreshold()) {
                prompt = prompt + "\n【库存提醒】当前讲解商品库存极低（系统记录约 " + inv
                        + "），话术须强调限量/即将售罄并引导立刻下单，避免超卖承诺。\n";
            } else if (inv <= li.getLowStockThreshold()) {
                prompt = prompt + "\n【库存提醒】当前讲解商品库存偏紧（系统记录约 " + inv
                        + "），可适当营造稀缺感；勿编造与系统不符的具体库存数字。\n";
            }
        }
        // ── 多轮上下文注入（P2 多轮上下文生成）──────────────────────────────
        if (vo.getPriorSlotsContext() != null && !vo.getPriorSlotsContext().isBlank()) {
            prompt = prompt + "\n\n【已生成话术摘要·请保持衔接一致性，避免与以下内容重复】\n"
                    + vo.getPriorSlotsContext();
        }

        // ── Prompt A/B 测试：注入变体指令 ──────────────────────────────────
        Long abVariantId = null;
        if (promptAbTestService != null && session.getUserId() != null) {
            try {
                String liveFormat = session.getLiveFormat();
                var variants = promptAbTestService.getActiveVariants(
                        session.getUserId(), session.getId(), scriptType, liveFormat);
                if (!variants.isEmpty()) {
                    var selectedVariant = promptAbTestService.pickVariant(variants);
                    if (selectedVariant != null) {
                        abVariantId = selectedVariant.variantId();
                        if (StringUtils.hasText(selectedVariant.userPromptAppend())) {
                            prompt = prompt + "\n\n【A/B实验变体】" + selectedVariant.userPromptAppend();
                        }
                        log.debug("[PromptAB] 命中变体 variantId={} name={}",
                                selectedVariant.variantId(), selectedVariant.variantName());
                    }
                }
            } catch (Exception e) {
                log.debug("[PromptAB] A/B 变体注入跳过: {}", e.getMessage());
            }
        }

        return new AugmentedUserPromptWithVariant(prompt, ragRefs, abVariantId);
    }

    /** LF-02：与 product 类似、需要商品上下文的 RAG */
    private static boolean scriptTypeUsesProductForRag(String scriptType) {
        if (scriptType == null) {
            return false;
        }
        return "product".equals(scriptType) || "closing_deal".equals(scriptType) || "pain_point".equals(scriptType)
                || "testimony".equals(scriptType) || "deep_sell".equals(scriptType);
    }

    /** 留人策略：按槽位位置把节点要求追加进 requirement（须在 buildPrompt 之前调用） */
    public void injectRetentionNode(LiveAiGenerateVO vo, int slotIndex) {
        if (businessParamConfig == null || vo == null) {
            return;
        }
        var rf = businessParamConfig.getRetention();
        int suspenseInterval = rf.getSuspenseMinutes();
        int climaxInterval = rf.getMiniClimaxMinutes();
        int tipInterval = rf.getPracticalTipMinutes();

        int estimatedMinute = slotIndex * 10;
        StringBuilder retentionHint = new StringBuilder();
        if (suspenseInterval > 0 && estimatedMinute > 0 && estimatedMinute % suspenseInterval == 0) {
            retentionHint.append("【留人节点·悬念】请在本段话术中制造一个悬念（如\"等下要说的更劲爆\"\"先别走，后面有大招\"）。");
        }
        if (climaxInterval > 0 && estimatedMinute > 0 && estimatedMinute % climaxInterval == 0) {
            retentionHint.append("【留人节点·小高潮】请在本段话术中制造一个情绪小高潮（爆笑/感动/震惊瞬间）。");
        }
        if (tipInterval > 0 && estimatedMinute > 0 && estimatedMinute % tipInterval == 0) {
            retentionHint.append("【留人节点·干货】请在本段话术中输出一个实用技巧或干货知识点。");
        }
        if (!retentionHint.isEmpty()) {
            String existing = vo.getRequirement() != null ? vo.getRequirement() : "";
            vo.setRequirement(existing + "\n" + retentionHint);
        }
    }

    /** 热点词：仅当 hotKeywords 仍为 null 时拉取注入（与生成服务原逻辑一致） */
    public void enrichHotKeywordsIfEmpty(LiveAiGenerateVO vo) {
        if (vo == null) {
            return;
        }
        if (vo.getHotKeywords() != null) {
            return;
        }
        if (guiguiyaHotClient == null) {
            return;
        }
        try {
            List<HotItemVO> hotList = guiguiyaHotClient.fetchDouyinHot();
            if (hotList != null && !hotList.isEmpty()) {
                List<String> words = hotList.stream()
                        .limit(5)
                        .map(HotItemVO::getWord)
                        .filter(w -> w != null && !w.isBlank())
                        .toList();
                if (!words.isEmpty()) {
                    vo.setHotKeywords(words);
                    log.debug("热点驱动：自动注入热搜词 {}", words);
                }
            }
        } catch (Exception e) {
            log.debug("热点驱动拉取失败，跳过: {}", e.getMessage());
        }
    }

    /**
     * 效果反馈闭环：同场次历史效果分 → 改进/参考提示（仅依赖 sessionId + scriptType）。
     */
    public String buildEffectivenessFeedbackHint(Long sessionId, String scriptType) {
        try {
            List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
            if (scripts.isEmpty()) {
                return null;
            }

            List<LiveScript> scored = scripts.stream()
                    .filter(s -> s.getEffectivenessScore() != null)
                    .filter(s -> scriptType == null || scriptType.equals(s.getScriptType()))
                    .toList();
            if (scored.isEmpty()) {
                return null;
            }

            // 阈值对齐 live_script.effectiveness_score 的 0-100 归因尺度
            // 低效阈值 40（原 6.0 对应 0-10 尺度），高效阈值 75（原 8.0）
            double lowThreshold = businessParamConfig != null && businessParamConfig.getAttribution() != null
                    && businessParamConfig.getAttribution().getLowScoreThreshold() > 0
                    ? businessParamConfig.getAttribution().getLowScoreThreshold()
                    : 40.0;
            double highThreshold = 75.0;
            List<String> lowIssues = new ArrayList<>();
            List<String> highStrengths = new ArrayList<>();
            for (LiveScript s : scored) {
                double score = s.getEffectivenessScore().doubleValue();
                if (score < lowThreshold && s.getAiSuggestion() != null && !s.getAiSuggestion().isBlank()) {
                    lowIssues.add(s.getAiSuggestion().length() > 100 ? s.getAiSuggestion().substring(0, 100) : s.getAiSuggestion());
                } else if (score >= highThreshold && s.getScriptContent() != null && s.getScriptContent().length() > 20) {
                    String preview = s.getScriptContent().substring(0, Math.min(80, s.getScriptContent().length()));
                    highStrengths.add(String.format("效果%.0f分：%s…", score, preview));
                }
            }

            if (lowIssues.isEmpty() && highStrengths.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            if (!lowIssues.isEmpty()) {
                sb.append("\n【效果反馈·待改进】以下是同场次低效话术的问题，请避免重蹈覆辙：\n");
                lowIssues.stream().limit(3).forEach(issue -> sb.append("- ").append(issue).append("\n"));
            }
            if (!highStrengths.isEmpty()) {
                sb.append("\n【效果反馈·可参考】以下同场次话术效果优秀，请借鉴其成功要素：\n");
                highStrengths.stream().limit(3).forEach(s -> sb.append("- ").append(s).append("\n"));
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("效果反馈查询跳过: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 因果引擎增强提示：将 {@link IndustryCausalEngine#infer} 的预测结果注入 prompt。
     * <p>将历史效果统计从「看数字」升级为「看因果可解释建议」，辅助 LLM 生成更具针对性的话术。
     * 仅在 {@code app.live.generation.causal-enrichment=true} 且引擎可用时执行。
     */
    String buildCausalEnrichmentHint(String scriptType, LiveSession session, DyPersona persona, LiveAiGenerateVO vo) {
        try {
            java.util.Map<String, Object> input = new java.util.HashMap<>();
            input.put("scriptType", scriptType != null ? scriptType : "product");
            if (persona != null) {
                if (persona.getPersonaName() != null) input.put("hostCode", persona.getPersonaName());
                if (persona.getPersonaType() != null) input.put("persona", persona.getPersonaType());
            }
            if (session != null) {
                if (session.getSessionType() != null) input.put("sessionType", session.getSessionType());
                if (session.getLiveFormat() != null) input.put("liveFormat", session.getLiveFormat());
            }
            if (vo != null && vo.getProductId() != null) {
                // 时段推断（基于 scheduledTime）
                input.put("productType", "profit");
            }

            IndustryCausalEngine.CausalInferenceResult result = causalEngine.infer(input);
            if (result == null || result.explanation() == null) return null;

            StringBuilder sb = new StringBuilder("\n【因果策略建议·参考】");
            sb.append(String.format("预计转化贡献因子 %.2f，", result.expectedConversionRate()));
            if (result.keyFactors() != null && !result.keyFactors().isEmpty()) {
                sb.append("关键正向因素：").append(String.join("、", result.keyFactors().stream().limit(3).toList())).append("。");
            }
            if (result.riskPoints() != null && !result.riskPoints().isEmpty()) {
                sb.append("需规避风险：").append(String.join("、", result.riskPoints().stream().limit(2).toList())).append("。");
            }
            sb.append("请在话术中强化以上正向因素、规避风险点。");
            return sb.toString();
        } catch (Exception e) {
            log.debug("[Composer] 因果引擎注入跳过: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从场次关联商品中解析品类，用于竞品差异化上下文。
     * 优先取 VO 中的 productId 对应商品品类，其次取场次第一个商品品类。
     */
    private String resolveProductCategory(LiveSession session, LiveAiGenerateVO vo) {
        try {
            if (vo != null && vo.getProductId() != null) {
                return productRepository.findById(vo.getProductId())
                        .map(p -> p.getProductCategory() != null ? p.getProductCategory() : "护肤")
                        .orElse("护肤");
            }
        } catch (Exception e) {
            log.debug("[Composer] 解析商品品类失败，使用默认品类: {}", e.getMessage());
        }
        return "护肤";
    }
}
