package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.ai.service.QueryRewriteService;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LivePromptBuilder;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.common.event.LiveScriptGeneratedEvent;
import cn.gaifan.douyinOperations.module.live.vo.EmotionalScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.live.vo.SimilarityItemVO;
import cn.gaifan.douyinOperations.module.live.vo.SkeletonSlotVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class LiveAiServiceImpl implements LiveAiService {

    private static final Logger log = LoggerFactory.getLogger(LiveAiServiceImpl.class);

    @Resource private LiveSessionRepository sessionRepository;
    @Resource private LiveScriptRepository scriptRepository;
    @Resource private DyPersonaRepository personaRepository;
    @Resource private DyProductRepository productRepository;
    @Resource private LiveProductRepository liveProductRepository;
    @Resource private ProductService productService;
    @Resource private DyProductScriptRepository productScriptRepository;
    @Resource private ViolationWordService violationWordService;
    @Resource private LlmClient llmClient;
    @Resource private AiModelRepository aiModelRepository;
    @Resource private LiveScriptService liveScriptService;
    @Resource private LivePromptBuilder promptBuilder;
    @Resource private LiveAiModelHelper modelHelper;
    @Resource private ApplicationEventPublisher eventPublisher;
    @Resource private KnowledgeBaseService knowledgeBaseService;
    @Resource private LiveKnowledgeBaseAccessResolver knowledgeBaseAccessResolver;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private QueryRewriteService queryRewriteService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @org.springframework.beans.factory.annotation.Value("${app.ai.kb.rag.enabled:true}")
    private boolean ragEnabled;
    @org.springframework.beans.factory.annotation.Value("${app.ai.kb.rag.top-k:8}")
    private int ragTopK;
    @org.springframework.beans.factory.annotation.Value("${app.ai.kb.rag.min-score:0.5}")
    private double ragMinScore;
    @org.springframework.beans.factory.annotation.Value("${app.ai.kb.rag.max-context-chars:2000}")
    private int ragMaxContextChars;
    @org.springframework.beans.factory.annotation.Value("${app.ai.kb.rag.token-budget-ratio:0.25}")
    private double ragTokenBudgetRatio;
    @org.springframework.beans.factory.annotation.Value("${app.ai.kb.rag.parallel-timeout-sec:5}")
    private int ragParallelTimeoutSec;

    @Override
    @Retry(name = "liveAiRetry")
    public LiveAiResultVO generateOpening(LiveAiGenerateVO vo) {
        return doGenerate(vo, "opening");
    }

    @Override
    @Retry(name = "liveAiRetry")
    public LiveAiResultVO generateProduct(LiveAiGenerateVO vo) {
        if (vo.getProductId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "商品介绍话术需要指定商品 ID");
        }
        return doGenerate(vo, "product");
    }

    @Override
    @Retry(name = "liveAiRetry")
    public LiveAiResultVO generateTransition(LiveAiGenerateVO vo) {
        return doGenerate(vo, "transition");
    }

    @Override
    @Retry(name = "liveAiRetry")
    public LiveAiResultVO generateClosing(LiveAiGenerateVO vo) {
        return doGenerate(vo, "closing");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveAiFullResultVO generateFull(LiveAiGenerateVO vo) {
        liveScriptService.ensureScriptSlotsForSession(vo.getSessionId());
        List<LiveScript> slots = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0);
        if (slots.isEmpty()) {
            LiveAiFullResultVO r = new LiveAiFullResultVO();
            r.setResults(new ArrayList<>());
            r.setConsumption(0);
            return r;
        }

        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : session.getPersonaId();
        DyPersona persona = personaId != null ? personaRepository.findByIdAndDeleted(personaId, 0).orElse(null) : null;
        String globalStyle = promptBuilder.resolveStyle(vo);

        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(vo.getSessionId()).stream()
                .sorted((a, b) -> Integer.compare(a.getPosition() != null ? a.getPosition() : 0, b.getPosition() != null ? b.getPosition() : 0))
                .toList();
        java.util.Map<Long, DyProduct> productMap = liveProducts.isEmpty() ? java.util.Collections.emptyMap()
                : productRepository.findAllById(liveProducts.stream().map(LiveProduct::getProductId).toList())
                        .stream().collect(Collectors.toMap(DyProduct::getId, p -> p));

        double consumption = 0;
        for (int i = 0; i < slots.size(); i++) {
            LiveScript slot = slots.get(i);
            SlotResult sr = generateAndUpdateSlotWithConsumption(slot, session, persona, vo, globalStyle, liveProducts, productMap, i);
            consumption += sr.consumption();
        }

        // 分析成篇：通读整场话术，优化衔接与语气（计 1）
        analyzeAndRefineFullScript(vo.getSessionId(), session.getUserId());
        consumption += 1.0;

        LiveAiFullResultVO full = new LiveAiFullResultVO();
        full.setResults(scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0).stream()
                .map(s -> {
                    LiveAiResultVO r = new LiveAiResultVO();
                    r.setContent(s.getScriptContent());
                    r.setScriptType(s.getScriptType());
                    r.setViolationCheck(doViolationCheck(s.getScriptContent(), session.getUserId(), "live"));
                    return r;
                })
                .collect(Collectors.toList()));
        full.setConsumption(consumption);
        return full;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveAiFullResultVO generateFullWithProgress(LiveAiGenerateVO vo, LiveAiService.FullGenerateProgressCallback progressCallback) {
        liveScriptService.ensureScriptSlotsForSession(vo.getSessionId());
        List<LiveScript> slots = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0);
        if (slots.isEmpty()) {
            LiveAiFullResultVO r = new LiveAiFullResultVO();
            r.setResults(new ArrayList<>());
            r.setConsumption(0);
            return r;
        }

        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : session.getPersonaId();
        DyPersona persona = personaId != null ? personaRepository.findByIdAndDeleted(personaId, 0).orElse(null) : null;
        String globalStyle = promptBuilder.resolveStyle(vo);

        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(vo.getSessionId()).stream()
                .sorted((a, b) -> Integer.compare(a.getPosition() != null ? a.getPosition() : 0, b.getPosition() != null ? b.getPosition() : 0))
                .toList();
        java.util.Map<Long, DyProduct> productMap = liveProducts.isEmpty() ? java.util.Collections.emptyMap()
                : productRepository.findAllById(liveProducts.stream().map(LiveProduct::getProductId).toList())
                        .stream().collect(Collectors.toMap(DyProduct::getId, p -> p));

        double consumption = 0;
        int total = slots.size() + 2; // +1 成篇优化 +1 完成
        for (int i = 0; i < slots.size(); i++) {
            LiveScript slot = slots.get(i);
            SlotResult sr = generateAndUpdateSlotWithConsumption(slot, session, persona, vo, globalStyle, liveProducts, productMap, i);
            consumption += sr.consumption();
            String slotLabel = slotLabel(slot.getScriptType(), i, liveProducts.size());
            progressCallback.onProgress(i + 1, total, slotLabel);
        }

        progressCallback.onProgress(slots.size() + 1, total, "成篇优化");
        analyzeAndRefineFullScript(vo.getSessionId(), session.getUserId());
        consumption += 1.0;
        progressCallback.onProgress(total, total, "完成");

        LiveAiFullResultVO full = new LiveAiFullResultVO();
        full.setResults(scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0).stream()
                .map(s -> {
                    LiveAiResultVO r = new LiveAiResultVO();
                    r.setContent(s.getScriptContent());
                    r.setScriptType(s.getScriptType());
                    r.setViolationCheck(doViolationCheck(s.getScriptContent(), session.getUserId(), "live"));
                    return r;
                })
                .collect(Collectors.toList()));
        full.setConsumption(consumption);
        return full;
    }

    private static String slotLabel(String scriptType, int index, int productCount) {
        if (scriptType == null) return "自定义";
        return switch (scriptType) {
            case "opening" -> "开场";
            case "product" -> "产品" + ((index + 1) / 2);
            case "transition" -> "转场" + (index / 2);
            case "closing" -> "收尾";
            default -> scriptType;
        };
    }

    private record SlotResult(LiveAiResultVO result, double consumption) {}

    /** 按槽位需求逐段生成，更新已有 script；返回结果与消费系数（引用=0，AI=1） */
    private SlotResult generateAndUpdateSlotWithConsumption(LiveScript slot, LiveSession session, DyPersona persona,
            LiveAiGenerateVO baseVo, String globalStyle, List<LiveProduct> liveProducts,
            java.util.Map<Long, DyProduct> productMap, int slotIndex) {
        LiveAiResultVO r = generateAndUpdateSlot(slot, session, persona, baseVo, globalStyle, liveProducts, productMap, slotIndex);
        double consumption = (slot.getReferencedScriptId() != null) ? 0.0 : 1.0;
        return new SlotResult(r, consumption);
    }

    /** 按槽位需求逐段生成，更新已有 script */
    private LiveAiResultVO generateAndUpdateSlot(LiveScript slot, LiveSession session, DyPersona persona,
            LiveAiGenerateVO baseVo, String globalStyle, List<LiveProduct> liveProducts,
            java.util.Map<Long, DyProduct> productMap, int slotIndex) {
        String scriptType = slot.getScriptType() != null ? slot.getScriptType() : "custom";
        Long productId = slot.getProductId();
        String requirement = slot.getRequirement() != null && !slot.getRequirement().isBlank()
                ? slot.getRequirement() : defaultRequirement(scriptType);
        Integer durationSec = slot.getDurationLimitSec() != null && slot.getDurationLimitSec() > 0
                ? slot.getDurationLimitSec() : null;
        String style = slot.getStyle() != null && !slot.getStyle().isBlank() ? slot.getStyle() : globalStyle;

        LiveAiGenerateVO vo = new LiveAiGenerateVO();
        vo.setSessionId(session.getId());
        vo.setPersonaId(baseVo.getPersonaId());
        vo.setStyle(style);
        vo.setExtraPrompt(baseVo.getExtraPrompt());
        vo.setRequirement(requirement);
        vo.setDurationLimitSec(durationSec);
        vo.setProductId(productId);
        vo.setUseKbRef(baseVo.getUseKbRef());

        if ("product".equals(scriptType) && productId != null) {
            LiveProduct lp = liveProducts.stream().filter(p -> productId.equals(p.getProductId())).findFirst().orElse(null);
            // 产品分类：优先用 LiveProduct 已选分类，否则自动推断（用于话术时长调整）
            String productType = (lp != null && lp.getProductType() != null && !lp.getProductType().isBlank())
                    ? lp.getProductType() : productService.inferProductType(productId);
            vo.setProductType(productType);
            if (lp != null && "product".equals(lp.getScriptSource()) && lp.getProductScriptId() != null) {
                var psOpt = productScriptRepository.findById(lp.getProductScriptId());
                if (psOpt.isPresent()) {
                    var ps = psOpt.get();
                    String content = ps.getScriptContent();
                    if (content != null && !content.isBlank()) {
                        slot.setScriptContent(content);
                        slot.setAiGenerated(0);
                        slot.setGenerationStatus("success");
                        slot.setReferencedScriptId(ps.getId());
                        slot.setReferencedScriptSnapshot(buildReferencedSnapshot(ps));
                        scriptRepository.save(slot);
                        eventPublisher.publishEvent(new LiveScriptGeneratedEvent(this, session.getId(), slot.getId(), "product", session.getUserId()));
                        LiveAiResultVO r = new LiveAiResultVO();
                        r.setContent(content);
                        r.setScriptType("product");
                        r.setViolationCheck(doViolationCheck(content, session.getUserId(), "live"));
                        return r;
                    }
                }
            }
        }
        if ("transition".equals(scriptType) && slotIndex >= 2) {
            int fromIdx = (slotIndex - 2) / 2;
            int toIdx = fromIdx + 1;
            if (fromIdx >= 0 && toIdx < liveProducts.size()) {
                DyProduct from = productMap.get(liveProducts.get(fromIdx).getProductId());
                DyProduct to = productMap.get(liveProducts.get(toIdx).getProductId());
                if (from != null && to != null) {
                    vo.setFromProductName(from.getProductName());
                    vo.setToProductName(to.getProductName());
                }
            }
        }

        try {
            String content = generateWithLlm(scriptType, session, persona, vo).content();
            slot.setScriptContent(content);
            slot.setStyle(style);
            slot.setRequirement(requirement);
            slot.setDurationLimitSec(durationSec);
            slot.setAiGenerated(1);
            slot.setGenerationStatus("success");
            if (productId != null) slot.setProductId(productId);
            slot.setReferencedScriptId(null);
            slot.setReferencedScriptSnapshot(null);
            scriptRepository.save(slot);
            eventPublisher.publishEvent(new LiveScriptGeneratedEvent(this, session.getId(), slot.getId(), scriptType, session.getUserId()));
            LiveAiResultVO r = new LiveAiResultVO();
            r.setContent(content);
            r.setScriptType(scriptType);
            r.setViolationCheck(doViolationCheck(content, session.getUserId(), "live"));
            return r;
        } catch (Exception e) {
            log.warn("话术生成失败 slotId={} type={}, 标记 failed: {}", slot.getId(), scriptType, e.getMessage());
            slot.setScriptContent("");
            slot.setGenerationStatus("failed");
            scriptRepository.save(slot);
            LiveAiResultVO r = new LiveAiResultVO();
            r.setContent("");
            r.setScriptType(scriptType);
            LiveAiResultVO.ViolationCheckResult check = new LiveAiResultVO.ViolationCheckResult();
            check.setPassed(false);
            check.setViolationCount(0);
            r.setViolationCheck(check);
            return r;
        }
    }

    private String defaultRequirement(String scriptType) {
        return switch (scriptType) {
            case "opening" -> "开场白";
            case "product" -> "产品介绍";
            case "transition" -> "转场";
            case "closing" -> "收尾";
            default -> "互动引导";
        };
    }

    /** 构建产品话术引用快照 JSON */
    private String buildReferencedSnapshot(DyProductScript ps) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ps.getId());
            m.put("version", ps.getVersion() != null ? ps.getVersion() : 1);
            m.put("content", ps.getScriptContent());
            m.put("style", ps.getStyle());
            m.put("scriptType", ps.getScriptType());
            return new ObjectMapper().writeValueAsString(m);
        } catch (Exception e) {
            log.warn("构建引用快照失败: {}", e.getMessage());
            return null;
        }
    }

    /** 分析成篇：通读整场话术，优化衔接与语气，回写各段 */
    private void analyzeAndRefineFullScript(Long sessionId, Long userId) {
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (scripts.size() < 2) return;
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models == null || models.isEmpty()) return;

        StringBuilder full = new StringBuilder();
        for (int i = 0; i < scripts.size(); i++) {
            full.append("【第").append(i + 1).append("段】\n");
            full.append(scripts.get(i).getScriptContent() != null ? scripts.get(i).getScriptContent() : "").append("\n\n");
        }
        String systemPrompt = """
            你是直播话术成篇优化专家。通读整场话术，优化衔接与语气统一。
            原则：① 保持每段核心意图和卖点不变 ② 只优化段与段之间的衔接、语气统一 ③ 避免过度改写
            输出格式：每段以【第N段】开头，后跟该段话术，段与段之间空一行。段落数量与顺序必须与输入一致。
            """;
        String userPrompt = "请优化以下整场话术的衔接与语气，保持各段核心内容不变：\n\n" + full;

        try {
            LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
            if (!resp.success() || resp.content() == null || resp.content().isBlank()) return;
            java.util.List<String> refined = parseRefinedSegments(resp.content().trim(), scripts.size());
            for (int i = 0; i < scripts.size() && i < refined.size(); i++) {
                String text = refined.get(i).trim();
                if (!text.isBlank()) {
                    scripts.get(i).setScriptContent(text);
                    scriptRepository.save(scripts.get(i));
                }
            }
        } catch (Exception e) {
            log.warn("分析成篇失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    private java.util.List<String> parseRefinedSegments(String content, int expectedCount) {
        java.util.List<String> segments = new ArrayList<>();
        String[] parts = content.split("【第\\d+段】");
        for (String p : parts) {
            String s = p.trim();
            if (!s.isBlank()) segments.add(s);
        }
        if (segments.size() >= expectedCount) {
            return segments.subList(0, expectedCount);
        }
        return segments;
    }

    /** BR-11：单条失败不回滚，标记 generation_status=failed，继续生成 */
    private LiveAiResultVO doGenerateWithFallback(LiveAiGenerateVO vo, String scriptType, Long productIdForScript) {
        try {
            return doGenerate(vo, scriptType);
        } catch (Exception e) {
            log.warn("话术生成失败 type={} productId={}, 标记 failed: {}", scriptType, vo.getProductId(), e.getMessage());
            LiveSession session = sessionRepository.findById(vo.getSessionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
            LiveScript script = new LiveScript();
            script.setSessionId(session.getId());
            script.setScriptContent("");
            script.setScriptType(scriptType);
            script.setStyle(vo.getStyle());
            script.setAiGenerated(1);
            applyNewScriptDefaults(script, session.getUserId(), "failed");
            script.setGenerationStatus("failed");
            script.setSequenceNo(getNextSequenceNo(session.getId()));
            if (productIdForScript != null) script.setProductId(productIdForScript);
            scriptRepository.save(script);
            LiveAiResultVO result = new LiveAiResultVO();
            result.setContent("");
            result.setScriptType(scriptType);
            LiveAiResultVO.ViolationCheckResult check = new LiveAiResultVO.ViolationCheckResult();
            check.setPassed(false);
            check.setViolationCount(0);
            result.setViolationCheck(check);
            return result;
        }
    }

    @Override
    public LiveAiResultVO.ViolationCheckResult checkViolation(Long userId, Long scriptId) {
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        return doViolationCheck(script.getScriptContent(), userId, "live");
    }

    @Override
    public LiveAiResultVO.ViolationCheckResult checkViolationByContent(String content, Long userId) {
        if (content == null || content.isBlank()) {
            LiveAiResultVO.ViolationCheckResult r = new LiveAiResultVO.ViolationCheckResult();
            r.setPassed(false);
            r.setViolationCount(0);
            r.setViolations(List.of());
            return r;
        }
        return doViolationCheck(content, userId, "live");
    }

    @Override
    public String generateForSlot(Long scriptId, String requirementOverride, Integer durationSecOverride) {
        LiveScript slot = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        LiveSession session = sessionRepository.findById(slot.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        Long personaId = session.getPersonaId();
        DyPersona persona = personaId != null ? personaRepository.findByIdAndDeleted(personaId, 0).orElse(null) : null;
        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(slot.getSessionId()).stream()
                .sorted((a, b) -> Integer.compare(a.getPosition() != null ? a.getPosition() : 0, b.getPosition() != null ? b.getPosition() : 0))
                .toList();
        java.util.Map<Long, DyProduct> productMap = liveProducts.isEmpty() ? java.util.Collections.emptyMap()
                : productRepository.findAllById(liveProducts.stream().map(LiveProduct::getProductId).toList())
                        .stream().collect(Collectors.toMap(DyProduct::getId, p -> p));

        String scriptType = slot.getScriptType() != null ? slot.getScriptType() : "custom";
        String requirement = (requirementOverride != null && !requirementOverride.isBlank())
                ? requirementOverride
                : (slot.getRequirement() != null && !slot.getRequirement().isBlank() ? slot.getRequirement() : defaultRequirement(scriptType));
        Integer durationSec = (durationSecOverride != null && durationSecOverride > 0)
                ? durationSecOverride
                : (slot.getDurationLimitSec() != null && slot.getDurationLimitSec() > 0 ? slot.getDurationLimitSec() : null);
        String style = slot.getStyle() != null && !slot.getStyle().isBlank() ? slot.getStyle() : null;
        if (style == null && session.getScriptStyle() != null && !session.getScriptStyle().isBlank()) {
            style = session.getScriptStyle();
        }

        LiveAiGenerateVO vo = new LiveAiGenerateVO();
        vo.setSessionId(session.getId());
        vo.setPersonaId(personaId);
        vo.setStyle(style);
        vo.setRequirement(requirement);
        vo.setDurationLimitSec(durationSec);
        vo.setProductId(slot.getProductId());

        List<LiveScript> allSlots = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(slot.getSessionId(), 0);
        int slotIndex = -1;
        for (int i = 0; i < allSlots.size(); i++) {
            if (allSlots.get(i).getId().equals(scriptId)) {
                slotIndex = i;
                break;
            }
        }
        if ("transition".equals(scriptType) && slotIndex >= 2) {
            int fromIdx = (slotIndex - 2) / 2;
            int toIdx = fromIdx + 1;
            if (fromIdx >= 0 && toIdx < liveProducts.size()) {
                DyProduct from = productMap.get(liveProducts.get(fromIdx).getProductId());
                DyProduct to = productMap.get(liveProducts.get(toIdx).getProductId());
                if (from != null && to != null) {
                    vo.setFromProductName(from.getProductName());
                    vo.setToProductName(to.getProductName());
                }
            }
        }
        // 产品分类：用于话术时长调整
        if ("product".equals(scriptType) && slot.getProductId() != null) {
            LiveProduct lp = liveProducts.stream().filter(p -> slot.getProductId().equals(p.getProductId())).findFirst().orElse(null);
            String productType = (lp != null && lp.getProductType() != null && !lp.getProductType().isBlank())
                    ? lp.getProductType() : productService.inferProductType(slot.getProductId());
            vo.setProductType(productType);
        }

        return generateWithLlm(scriptType, session, persona, vo).content();
    }

    @Override
    public String refineScript(Long scriptId, String userQuestion, Long userId) {
        return refineScript(scriptId, userQuestion, userId, null);
    }

    @Override
    public String refineScript(Long scriptId, String userQuestion, Long userId, Long modelId) {
        if (scriptId == null || scriptId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        }
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "修改要求不能为空");
        }
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        String content = script.getScriptContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术内容为空，无法修改");
        }
        List<AiModel> models = resolveAvailableModels(modelId);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }
        String durationInstruction = buildDurationInstruction(script);
        String scriptContext = buildScriptContext(script);
        String opsContext = buildLiveRefineOpsContext(userId, script, userQuestion + " " + content);
        String systemPrompt = """
            你是直播话术修改助手。用户提出修改要求时，在保持原有优点（卖点、感染力）基础上精准调整。
            常见要求：时长压缩、风格切换（促销/亲切/专业）、意图转换（介绍→促单）、语气调整。
            原则：① 保留有效信息 ② 按用户要求调整 ③ 严格遵守段落时长上限 ④ 保持口语化、无违禁词 ⑤ 必须遵守下方 douyin 与 douyin_weigui 官方规则引用
            输出：只输出修改后话术正文，不要输出标题、解释、建议、Markdown 或引号。
            """;
        String userPrompt = String.format(
                "%s\n%s%s\n用户要求：%s\n\n当前话术：\n%s\n\n请按要求修改，只输出可直接替换原文的话术正文：",
                scriptContext, durationInstruction, opsContext, userQuestion.trim(), content);
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, resp.errorMsg() != null ? resp.errorMsg() : "AI 修改失败");
        }
        return resp.content().trim();
    }

    @Override
    public String suggestImprovement(Long scriptId, Long userId, Long modelId) {
        if (scriptId == null || scriptId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        }
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        LiveSession session = sessionRepository.findById(script.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        if (userId != null && session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此话术");
        }
        String content = script.getScriptContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术内容为空，无法改进");
        }
        List<AiModel> models = resolveAvailableModels(modelId);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }
        String durationInstruction = buildDurationInstruction(script);
        String scriptContext = buildScriptContext(script);
        String opsContext = buildLiveRefineOpsContext(userId, script, content);
        String systemPrompt = """
            你是直播话术改写助手。你的任务是直接生成「可替换原文」的改写稿，不是写分析报告。
            改写目标：提升开场留人、互动承接、卖点表达、促单转化和合规安全；减少模板腔和空泛口号。
            约束：保留真实卖点，不编造功效，不使用绝对化/夸大承诺，严格遵守段落时长上限，必须遵守下方 douyin 与 douyin_weigui 官方规则引用。
            输出：只输出改写后的话术正文。禁止输出改进建议、原因、标题、编号、Markdown、引号或解释。
            """;
        String userPrompt = String.format(
                "%s%s%s\n当前话术：\n%s\n\n请重写为更自然、更适合直播间直接口播的改进版，只输出可直接替换原文的话术正文：",
                scriptContext, durationInstruction, opsContext, content);
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, resp.errorMsg() != null ? resp.errorMsg() : "AI 改进失败");
        }
        return resp.content().trim();
    }

    private List<AiModel> resolveAvailableModels(Long modelId) {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models == null || models.isEmpty()) {
            return models;
        }
        if (modelId != null && modelId > 0) {
            return models.stream()
                    .filter(model -> modelId.equals(model.getId()))
                    .findFirst()
                    .map(List::of)
                    .orElse(models);
        }
        return models;
    }

    private String buildScriptContext(LiveScript script) {
        StringBuilder sb = new StringBuilder();
        sb.append("段落类型：").append(typeLabel(script.getScriptType())).append("\n");
        if (script.getRequirement() != null && !script.getRequirement().isBlank()) {
            sb.append("段落意图：").append(script.getRequirement().trim()).append("\n");
        }
        if (script.getStyle() != null && !script.getStyle().isBlank()) {
            sb.append("段落风格：").append(script.getStyle().trim()).append("\n");
        }
        return sb.toString();
    }

    private String buildLiveRefineOpsContext(Long userId, LiveScript script, String query) {
        if (operationalStrategyKnowledgeService == null || userId == null || userId <= 0) {
            return "";
        }
        try {
            String materialType = script != null && script.getScriptType() != null ? script.getScriptType() : "live_refine";
            OperationalStrategyKnowledgeService.PromptContext context =
                    operationalStrategyKnowledgeService.buildLiveGenerationContext(
                            userId,
                            String.join(" ", query != null ? query : "", "直播话术微调 商品时长 违规规则 官方规则"),
                            materialType,
                            2400);
            return context != null && context.hasText() ? context.promptBlock() : "";
        } catch (Exception e) {
            log.debug("直播微调官方知识上下文构建跳过: {}", e.getMessage());
            return "";
        }
    }

    private String buildDurationInstruction(LiveScript script) {
        Integer seconds = script.getDurationLimitSec();
        if (seconds == null || seconds <= 0) {
            return "";
        }
        int minChars = Math.max(8, seconds * 3);
        int maxChars = Math.max(minChars, seconds * 4);
        return "硬性时长限制：" + seconds + "秒以内，按每秒约3-4个中文字估算，正文建议控制在"
                + minChars + "-" + maxChars + "个中文字内；如果信息过多，优先保留关键卖点、互动钩子和合规表达。\n";
    }

    private String typeLabel(String scriptType) {
        return switch (scriptType != null ? scriptType : "custom") {
            case "opening" -> "开场话术";
            case "product" -> "产品话术";
            case "transition" -> "转场话术";
            case "closing" -> "收尾话术";
            case "chat" -> "聊家常话术";
            case "interaction" -> "互动引导话术";
            case "welfare" -> "福利话术";
            case "closing_deal" -> "逼单促单话术";
            case "hold_back" -> "憋单蓄水话术";
            case "emotional" -> "情绪价值话术";
            case "rapid_intro" -> "快速过品话术";
            case "deep_sell" -> "深度单品话术";
            case "pain_point" -> "痛点放大话术";
            case "testimony" -> "用户证言话术";
            default -> "自定义话术";
        };
    }

    @Override
    public String chatForScript(Long scriptId, String userMessage, Long userId) {
        if (scriptId == null || scriptId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请输入您的需求");
        }
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        LiveSession session = sessionRepository.findById(script.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        List<LiveProduct> products = liveProductRepository.findBySessionId(session.getId()).stream()
                .sorted(java.util.Comparator.comparing(p -> (p.getPosition() != null ? p.getPosition() : 0)))
                .toList();
        String prevProduct = null;
        String nextProduct = null;
        String currentProductName = null;
        String currentProductTypeLabel = null;
        String productTypeHint = "";

        String scriptType = script.getScriptType() != null ? script.getScriptType() : "custom";
        if ("product".equals(scriptType) && script.getProductId() != null) {
            int idx = -1;
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getProductId().equals(script.getProductId())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                LiveProduct p = products.get(idx);
                currentProductName = p.getProductName();
                currentProductTypeLabel = promptBuilder.formatProductTypeLabel(p.getProductType());
                productTypeHint = promptBuilder.getProductTypePromptHint(p.getProductType());
                if (idx > 0) prevProduct = products.get(idx - 1).getProductName();
                if (idx < products.size() - 1) nextProduct = products.get(idx + 1).getProductName();
            }
        } else if ("transition".equals(scriptType) && script.getProductId() != null) {
            int idx = -1;
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getProductId().equals(script.getProductId())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0 && idx < products.size() - 1) {
                prevProduct = products.get(idx).getProductName();
                nextProduct = products.get(idx + 1).getProductName();
            }
        } else if ("opening".equals(scriptType) && !products.isEmpty()) {
            nextProduct = products.get(0).getProductName();
        } else if ("closing".equals(scriptType) && !products.isEmpty()) {
            prevProduct = products.get(products.size() - 1).getProductName();
        }

        String typeLabel = "opening".equals(scriptType) ? "开场话术" : "product".equals(scriptType) ? "产品话术"
                : "transition".equals(scriptType) ? "转场话术" : "closing".equals(scriptType) ? "结尾话术" : scriptType;
        String req = script.getRequirement() != null && !script.getRequirement().isBlank() ? script.getRequirement() : "";
        String dur = script.getDurationLimitSec() != null && script.getDurationLimitSec() > 0
                ? script.getDurationLimitSec() + "秒" : "";
        String current = script.getScriptContent();
        boolean hasContent = current != null && !current.isBlank() && !"[待填写]".equals(current.trim());

        String systemPrompt = """
            你是直播话术写作助手，输出「即用型」话术供操作人微调。
            结构化条件：人设、场景、话术类型、时长、考虑维度(拉停留/互动/促单/种草/引流/成交)。
            产品类型：爆品→限时抢购/库存紧张；利润品→品质/价值感；亏品→引流/福利；平价品→性价比。
            多品场景：与前后产品话术差异化，避免句式雷同。
            若用户提供「上文对话」，则在其基础上继续优化，保留有效内容。
            输出：只输出话术正文，无标题无解释，可直接粘贴。
            """;
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("直播主题：").append(session.getLiveTitle() != null ? session.getLiveTitle() : "直播").append("\n");
        userPrompt.append("当前段落：").append(typeLabel);
        if (currentProductName != null) userPrompt.append("，当前产品：").append(currentProductName);
        if (currentProductTypeLabel != null) userPrompt.append("，产品类型：").append(currentProductTypeLabel);
        if (!productTypeHint.isBlank()) userPrompt.append("（").append(productTypeHint).append("）");
        if (prevProduct != null) userPrompt.append("，前一个产品：").append(prevProduct);
        if (nextProduct != null) userPrompt.append("，后一个产品：").append(nextProduct);
        if (!req.isBlank()) userPrompt.append("，需求：").append(req);
        if (!dur.isBlank()) userPrompt.append("，时长限制：").append(dur);
        userPrompt.append("\n");
        if (hasContent) {
            userPrompt.append("当前话术：\n").append(current).append("\n");
        }
        userPrompt.append("\n用户需求：").append(userMessage.trim());
        userPrompt.append("\n\n请根据用户需求").append(hasContent ? "修改" : "生成").append("话术。");
        userPrompt.append("输出即用型正文，操作人将直接粘贴并微调。只输出话术正文，无标题无解释：");

        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt.toString());
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, resp.errorMsg() != null ? resp.errorMsg() : "AI 生成失败");
        }
        return resp.content().trim();
    }

    @Override
    public java.util.Map<Long, String> batchChatForScript(List<Long> scriptIds, String userMessage, Long userId) {
        if (scriptIds == null || scriptIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 列表不能为空");
        }
        if (scriptIds.size() > 20) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "单次最多批量处理 20 段话术");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请输入您的需求");
        }
        java.util.Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (Long scriptId : scriptIds) {
            result.put(scriptId, chatForScript(scriptId, userMessage, userId));
        }
        return result;
    }

    @Override
    public List<SimilarityItemVO> checkSimilarity(Long sessionId, Long userId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次 ID 无效");
        }
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (scripts.size() < 2) return List.of();

        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scripts.size(); i++) {
            LiveScript s = scripts.get(i);
            String type = "opening".equals(s.getScriptType()) ? "开场" : "product".equals(s.getScriptType()) ? "产品" : "transition".equals(s.getScriptType()) ? "转场" : "closing".equals(s.getScriptType()) ? "结尾" : s.getScriptType();
            String content = s.getScriptContent();
            if (content == null || content.isBlank() || "[待填写]".equals(content.trim())) continue;
            sb.append("【").append(i + 1).append("】ID:").append(s.getId()).append(" 类型:").append(type).append("\n").append(content).append("\n\n");
        }
        String systemPrompt = """
            你是直播话术质量分析师。找出相似度过高、表达雷同的段落对，给出可执行的差异化建议。
            判断标准：句式雷同、卖点表述相似、引导话术重复等。
            suggestion 要具体可执行，如「产品2可强调限时，产品1强调品质」「转场可加入互动引导」。
            输出格式，每行一条：scriptId1|scriptId2|similarityLevel|suggestion
            similarityLevel 为 high 或 medium。只输出有问题的段落对，无问题则输出空。
            """;
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, "话术列表：\n\n" + sb + "\n请分析并输出相似段落对（格式：scriptId1|scriptId2|high或medium|具体差异化建议）：");
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) return List.of();

        List<SimilarityItemVO> result = new ArrayList<>();
        for (String line : resp.content().trim().split("\n")) {
            String[] parts = line.split("\\|");
            if (parts.length >= 4) {
                try {
                    SimilarityItemVO vo = new SimilarityItemVO();
                    vo.setScriptId1(Long.parseLong(parts[0].trim()));
                    vo.setScriptId2(Long.parseLong(parts[1].trim()));
                    vo.setSimilarityLevel(parts[2].trim().toLowerCase());
                    vo.setSuggestion(parts[3].trim());
                    vo.setType1(getTypeLabel(scripts, vo.getScriptId1()));
                    vo.setType2(getTypeLabel(scripts, vo.getScriptId2()));
                    result.add(vo);
                } catch (Exception e) { log.debug("相似度解析行失败: {}", e.getMessage()); }
            }
        }
        return result;
    }

    private String getTypeLabel(List<LiveScript> scripts, Long id) {
        return scripts.stream().filter(s -> s.getId().equals(id)).findFirst()
                .map(s -> "opening".equals(s.getScriptType()) ? "开场" : "product".equals(s.getScriptType()) ? "产品" : "transition".equals(s.getScriptType()) ? "转场" : "closing".equals(s.getScriptType()) ? "结尾" : s.getScriptType())
                .orElse("");
    }

    @Override
    public List<SkeletonSlotVO> generateSkeleton(Long sessionId, Long userId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次 ID 无效");
        }
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        liveScriptService.ensureScriptSlotsForSession(sessionId);
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (scripts.isEmpty()) return List.of();

        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        String scriptList = scripts.stream()
                .map(s -> "ID:" + s.getId() + " 类型:" + ("opening".equals(s.getScriptType()) ? "开场" : "product".equals(s.getScriptType()) ? "产品" : "transition".equals(s.getScriptType()) ? "转场" : "closing".equals(s.getScriptType()) ? "结尾" : s.getScriptType()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        String systemPrompt = """
            你是直播话术策划师。为每段话术生成「可执行」的一句话摘要和建议时长。
            summary：具体可展开的要点，如「开场：欢迎+点关注+预告福利」「产品1：卖点+限时抢购」。
            durationSec：开场60-90、产品120-180、转场20-40、结尾60-90，按类型合理设定。
            输出格式，每行一条：scriptId|summary|durationSec。按 scriptId 顺序输出。
            """;
        String userPrompt = "直播主题：" + (session.getLiveTitle() != null ? session.getLiveTitle() : "直播") + "\n\n话术槽位：\n" + scriptList + "\n\n请生成骨架（每行：scriptId|具体可展开的摘要|建议秒数）：";
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            return scripts.stream().map(s -> {
                SkeletonSlotVO vo = new SkeletonSlotVO();
                vo.setScriptId(s.getId());
                vo.setScriptType(s.getScriptType());
                vo.setSummary("");
                vo.setSuggestedDurationSec(60);
                return vo;
            }).toList();
        }

        List<SkeletonSlotVO> result = new ArrayList<>();
        java.util.Map<Long, SkeletonSlotVO> byId = new java.util.HashMap<>();
        for (LiveScript s : scripts) {
            SkeletonSlotVO vo = new SkeletonSlotVO();
            vo.setScriptId(s.getId());
            vo.setScriptType(s.getScriptType());
            vo.setSummary("");
            vo.setSuggestedDurationSec(60);
            byId.put(s.getId(), vo);
            result.add(vo);
        }
        for (String line : resp.content().trim().split("\n")) {
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {
                try {
                    Long id = Long.parseLong(parts[0].trim());
                    SkeletonSlotVO vo = byId.get(id);
                    if (vo != null) {
                        vo.setSummary(parts[1].trim());
                        vo.setSuggestedDurationSec(Integer.parseInt(parts[2].trim().replaceAll("\\D", "")));
                        if (vo.getSuggestedDurationSec() <= 0) vo.setSuggestedDurationSec(60);
                    }
                } catch (Exception e) { log.debug("骨架解析行失败: {}", e.getMessage()); }
            }
        }
        return result;
    }

    // ─── 核心生成逻辑 ──────────────────────────────────────

    private LiveAiResultVO doGenerate(LiveAiGenerateVO vo, String scriptType) {
        if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.DOUYIN_OPS,
                            FeatureCode.DOUYIN_LIVE_SCRIPT,
                            "直播话术生成 sessionId=" + vo.getSessionId() + " type=" + scriptType,
                            DeliveryProduct.DOUYIN_OPS
                    ));
        }
        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : session.getPersonaId();
        DyPersona persona = null;
        if (personaId != null) {
            persona = personaRepository.findByIdAndDeleted(personaId, 0).orElse(null);
        }

        // 产品分类：单条生成时无 LiveProduct，用 inferProductType 推断
        if ("product".equals(scriptType) && vo.getProductId() != null
                && (vo.getProductType() == null || vo.getProductType().isBlank())) {
            vo.setProductType(productService.inferProductType(vo.getProductId()));
        }

        // 尝试 LLM 生成，失败则 fallback 到模板
        GenerateResult gen = generateWithLlm(scriptType, session, persona, vo);
        String content = gen.content();

        // 保存到 live_script
        LiveScript script = new LiveScript();
        script.setSessionId(session.getId());
        script.setScriptContent(content);
        script.setScriptType(scriptType);
        script.setStyle(vo.getStyle());
        script.setAiGenerated(1);
        applyNewScriptDefaults(script, session.getUserId(), "success");
        script.setSequenceNo(getNextSequenceNo(session.getId()));
        if ("product".equals(scriptType) && vo.getProductId() != null) {
            script.setProductId(vo.getProductId());
        }
        scriptRepository.save(script);

        // 违规检测（scope=live，使用场次所属用户）
        LiveAiResultVO.ViolationCheckResult checkResult = doViolationCheck(content, session.getUserId(), "live");

        LiveAiResultVO result = new LiveAiResultVO();
        result.setContent(content);
        result.setScriptType(scriptType);
        result.setViolationCheck(checkResult);
        result.setRagRefs(toRagRefVOs(gen.ragRefs()));
        result.setOfficialReferences(resolveOfficialReferences(session.getUserId(), scriptType, content, vo.getMaterialType()));
        return result;
    }

    private static final int RAG_REF_PREVIEW_LEN = 120;

    private static List<LiveAiResultVO.RagRefVO> toRagRefVOs(List<KnowledgeBaseService.SearchResult> refs) {
        if (refs == null || refs.isEmpty()) return null;
        List<LiveAiResultVO.RagRefVO> list = new ArrayList<>();
        for (KnowledgeBaseService.SearchResult r : refs) {
            LiveAiResultVO.RagRefVO vo = new LiveAiResultVO.RagRefVO();
            vo.setDocId(r.docId());
            vo.setChunkId(r.chunkId());
            vo.setTitle(r.title());
            String content = r.content();
            vo.setContentPreview(content != null && content.length() > RAG_REF_PREVIEW_LEN ? content.substring(0, RAG_REF_PREVIEW_LEN) + "…" : content);
            vo.setScore(r.score());
            list.add(vo);
        }
        return list;
    }

    private record GenerateResult(String content, List<KnowledgeBaseService.SearchResult> ragRefs) {}

    private GenerateResult generateWithLlm(String scriptType, LiveSession session, DyPersona persona, LiveAiGenerateVO vo) {
        AiModel model = findAvailableModel();
        if (model == null) {
            log.warn("无可用 AI 模型，使用模板生成话术");
            return new GenerateResult(promptBuilder.buildScriptTemplate(scriptType, session, persona, vo), List.of());
        }

        String prompt = promptBuilder.buildPrompt(scriptType, session, persona, vo);
        boolean useRag = vo.getUseKbRef() == null || Boolean.TRUE.equals(vo.getUseKbRef());
        cn.gaifan.douyinOperations.module.product.entity.DyProduct productForRag = "product".equals(scriptType) && vo.getProductId() != null
                ? productRepository.findById(vo.getProductId()).orElse(null) : null;
        RagContextResult ragResult = useRag ? buildRagContext(session.getUserId(), productForRag, scriptType, promptBuilder.resolveStyle(vo), prompt.length(), null) : null;
        List<KnowledgeBaseService.SearchResult> ragRefs = ragResult != null ? ragResult.refs() : List.of();
        if (ragResult != null && ragResult.xml() != null && !ragResult.xml().isBlank()) {
            prompt = prompt + "\n\n" + ragResult.xml() + "\n请参考以上案例的表达方式，生成原创话术。\n";
        }
        try {
            LlmClient.LlmResponse response = llmClient.chat(model, LivePromptBuilder.SYSTEM_PROMPT, prompt);
            if (response.success() && response.content() != null && !response.content().isBlank()) {
                if (response.tokensUsed() > 0) {
                    aiModelRepository.incrementQuotaUsed(model.getId(), response.tokensUsed());
                }
                log.info("LLM 话术生成成功: type={}, model={}, tokens={}", scriptType, model.getModelVersion(), response.tokensUsed());
                return new GenerateResult(response.content().trim(), ragRefs);
            }
            log.warn("LLM 生成失败: {}, fallback 到模板", response.errorMsg());
        } catch (Exception e) {
            log.error("LLM 调用异常, fallback 到模板", e);
        }

        return new GenerateResult(promptBuilder.buildScriptTemplate(scriptType, session, persona, vo), ragRefs);
    }

    private record RagContextResult(String xml, List<KnowledgeBaseService.SearchResult> refs) {}

    /** RAG：从 huashu 知识库检索参考话术，拼成 XML 注入 Prompt，并返回用于归因的 refs */
    private RagContextResult buildRagContext(Long userId, cn.gaifan.douyinOperations.module.product.entity.DyProduct product, String scriptType, String style, int promptLength, List<String> kbCategories) {
        if (!ragEnabled || userId == null) return null;
        LiveKnowledgeBaseAccessResolver.ResolvedKnowledgeBase resolvedKb =
                knowledgeBaseAccessResolver.resolveHuashu(userId);
        if (resolvedKb == null) return null;
        if (resolvedKb.sharedFallback()) {
            log.debug("Live RAG 使用显式共享 huashu 知识库: requestUserId={}, kbOwnerId={}, kbId={}",
                    userId, resolvedKb.accessUserId(), resolvedKb.kbId());
        }
        final Long kbId = resolvedKb.kbId();
        final Long ragUserId = resolvedKb.accessUserId();
        int budget = (int) Math.max(200, ragMaxContextChars * ragTokenBudgetRatio);
        if (promptLength > 0) budget = Math.min(ragMaxContextChars, (int) (ragMaxContextChars - promptLength * 0.3));
        if (budget < 200) return null;

        List<String> queries = buildCategoryQueries(product, scriptType, style);
        if (queryRewriteService != null) {
            List<String> extra = new ArrayList<>();
            for (String q : queries) {
                if (q == null || q.isBlank()) continue;
                try {
                    List<String> rewritten = queryRewriteService.rewrite(userId, q);
                    if (rewritten != null) extra.addAll(rewritten);
                } catch (Exception e) { log.debug("queryRewrite失败 query={}: {}", q, e.getMessage()); }
            }
            queries = new ArrayList<>(queries);
            queries.addAll(extra);
        }
        queries = queries.stream().filter(q -> q != null && !q.isBlank()).distinct().limit(12).toList();
        if (queries.isEmpty()) return null;

        Set<Long> seenChunkIds = ConcurrentHashMap.newKeySet();
        List<KnowledgeBaseService.SearchResult> allResults = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = queries.stream()
                .map(q -> CompletableFuture.runAsync(() -> {
                    List<KnowledgeBaseService.SearchResult> list = knowledgeBaseService.hybridSearch(kbId, q, ragTopK + 5, ragUserId, null, true);
                    if (list != null) {
                        for (KnowledgeBaseService.SearchResult r : list) {
                            Long cid = r.chunkId() != null ? r.chunkId() : r.docId();
                            if (cid != null && seenChunkIds.add(cid)) allResults.add(r);
                        }
                    }
                }))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(ragParallelTimeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("RAG 并行检索部分超时，已收集 {} 条", allResults.size());
        } catch (Exception e) { log.debug("RAG并行检索异常: {}", e.getMessage()); }

        List<KnowledgeBaseService.SearchResult> filtered = allResults.stream()
                .filter(r -> r.score() >= ragMinScore)
                .filter(r -> {
                    // 如果指定了分类过滤，则只保留匹配的结果
                    if (kbCategories == null || kbCategories.isEmpty()) return true;
                    if (r.labels() == null || r.labels().isEmpty()) return false;
                    // 检查是否有匹配的分类标签（支持 type: 和 cat: 两种前缀）
                    for (String label : r.labels()) {
                        if (label != null) {
                            String category = null;
                            if (label.startsWith("type:")) {
                                category = label.replace("type:", "");
                            } else if (label.startsWith("cat:")) {
                                category = label.replace("cat:", "");
                            }
                            if (category != null && kbCategories.contains(category)) return true;
                        }
                    }
                    return false;
                })
                .sorted(Comparator.comparingDouble(KnowledgeBaseService.SearchResult::score).reversed())
                .limit(ragTopK)
                .toList();
        if (filtered.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("<reference_scripts>\n<note>仅参考风格和技巧，禁止照搬内容</note>\n");
        int totalLen = 0;
        List<KnowledgeBaseService.SearchResult> includedRefs = new ArrayList<>();
        for (int i = 0; i < filtered.size(); i++) {
            KnowledgeBaseService.SearchResult r = filtered.get(i);
            if (totalLen + r.content().length() > budget) break;
            String category = (r.labels() != null && !r.labels().isEmpty())
                    ? r.labels().stream().filter(l -> l != null && l.startsWith("type:")).findFirst().orElse("").replace("type:", "")
                    : "话术";
            sb.append(String.format("<script id=\"%d\" category=\"%s\" score=\"%.2f\">", i + 1, category, r.score()));
            sb.append(r.content());
            sb.append("</script>\n");
            totalLen += r.content().length();
            includedRefs.add(r);
        }
        sb.append("</reference_scripts>");
        return new RagContextResult(sb.toString(), includedRefs);
    }

    private AiModel findAvailableModel() {
        AiModel configured = modelHelper.findAvailableModel(null);
        if (configured != null) return configured;
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) return null;
        return models.stream()
                .filter(m -> m.getQuotaLimit() == null || m.getQuotaLimit() == 0 || m.getQuotaUsed() < m.getQuotaLimit())
                .findFirst()
                .orElse(models.get(0));
    }

    // ─── 工具方法 ──────────────────────────────────────

    private LiveAiResultVO.ViolationCheckResult doViolationCheck(String content, Long userId, String scope) {
        ViolationCheckResultVO checkResult = violationWordService.check(content, scope, userId);
        LiveAiResultVO.ViolationCheckResult result = new LiveAiResultVO.ViolationCheckResult();
        result.setPassed(!checkResult.isHasViolation());
        result.setViolationCount(checkResult.getTotalCount());
        if (checkResult.getViolations() != null) {
            result.setViolations(checkResult.getViolations().stream()
                    .map(v -> v.getWord() + "(" + v.getReason() + ")")
                    .collect(Collectors.toList()));
        }
        applyOfficialRuleGate(result, resolveOfficialViolationReferences(userId, content));
        return result;
    }

    private void applyNewScriptDefaults(LiveScript script, Long userId, String generationStatus) {
        if (script.getUserId() == null) script.setUserId(userId);
        if (script.getDeleted() == null) script.setDeleted(0);
        if (script.getExecuted() == null) script.setExecuted(0);
        if (script.getViolationChecked() == null) script.setViolationChecked(0);
        if (script.getAiGenerated() == null) script.setAiGenerated(0);
        if (script.getGenerationStatus() == null) script.setGenerationStatus(generationStatus);
    }

    private List<LiveAiResultVO.OfficialReferenceVO> resolveOfficialReferences(
            Long userId, String scriptType, String content, String materialType) {
        if (operationalStrategyKnowledgeService == null || userId == null || userId <= 0) {
            return List.of();
        }
        try {
            String query = String.join(" ",
                    scriptType != null ? scriptType : "直播话术",
                    content != null ? content : "",
                    "直播排品 商品时长 官方规则 违规风险");
            OperationalStrategyKnowledgeService.PromptContext context =
                    operationalStrategyKnowledgeService.buildLiveGenerationContext(userId, query, materialType, 1600);
            return toOfficialReferenceVOs(context != null ? context.officialReferences() : List.of());
        } catch (Exception e) {
            log.debug("直播官方规则引用检索跳过: {}", e.getMessage());
            return List.of();
        }
    }

    private List<LiveAiResultVO.OfficialReferenceVO> resolveOfficialViolationReferences(Long userId, String content) {
        if (operationalStrategyKnowledgeService == null || userId == null || userId <= 0) {
            return List.of();
        }
        try {
            OperationalStrategyKnowledgeService.PromptContext context =
                    operationalStrategyKnowledgeService.buildLiveGenerationContext(
                            userId,
                            String.join(" ", content != null ? content : "", "直播违规 绝对化用语 虚假宣传 douyin_weigui 官方规则"),
                            "violation_check",
                            1600);
            OperationalStrategyKnowledgeService.PromptContext ruleContext =
                    operationalStrategyKnowledgeService.buildViolationRuleContext(
                            userId,
                            String.join(" ", content != null ? content : "", "直播违规 直播话术违规 官方规则"),
                            "live_violation_check",
                            1600);
            List<OperationalStrategyKnowledgeService.OfficialReference> refs =
                    context != null && context.officialReferences() != null ? context.officialReferences() : List.of();
            List<OperationalStrategyKnowledgeService.OfficialReference> merged = new ArrayList<>(refs);
            if (ruleContext != null && ruleContext.officialReferences() != null) {
                merged.addAll(ruleContext.officialReferences());
            }
            return toOfficialReferenceVOs(merged.stream()
                    .filter(ref -> "douyin_weigui".equals(ref.kbName()) || "violation_rule".equals(ref.refType()))
                    .collect(Collectors.toMap(
                            ref -> String.valueOf(ref.docId()) + ":" + String.valueOf(ref.chunkId()),
                            ref -> ref,
                            (a, b) -> a,
                            LinkedHashMap::new))
                    .values().stream()
                    .toList());
        } catch (Exception e) {
            log.debug("直播违规官方规则引用检索跳过: {}", e.getMessage());
            return List.of();
        }
    }

    private List<LiveAiResultVO.OfficialReferenceVO> toOfficialReferenceVOs(
            List<OperationalStrategyKnowledgeService.OfficialReference> refs) {
        if (refs == null || refs.isEmpty()) return List.of();
        return refs.stream().map(ref -> {
            LiveAiResultVO.OfficialReferenceVO vo = new LiveAiResultVO.OfficialReferenceVO();
            vo.setKbName(ref.kbName());
            vo.setRefType(ref.refType());
            vo.setDocId(ref.docId());
            vo.setChunkId(ref.chunkId());
            vo.setTitle(ref.title());
            vo.setContentPreview(ref.contentPreview());
            vo.setScore(ref.score());
            return vo;
        }).toList();
    }

    private void applyOfficialRuleGate(
            LiveAiResultVO.ViolationCheckResult result,
            List<LiveAiResultVO.OfficialReferenceVO> officialReferences) {
        List<LiveAiResultVO.OfficialReferenceVO> refs = officialReferences != null ? officialReferences : List.of();
        boolean satisfied = refs.stream().anyMatch(ref ->
                "violation_rule".equals(ref.getRefType()) || "douyin_weigui".equals(ref.getKbName()));
        result.setOfficialReferences(refs);
        result.setOfficialReferenceRequired(true);
        result.setOfficialReferenceSatisfied(satisfied);
        result.setOfficialReferenceStatus(satisfied ? "satisfied" : "missing_douyin_weigui_reference");
        if (!satisfied) {
            List<String> violations = new ArrayList<>(result.getViolations() != null ? result.getViolations() : List.of());
            violations.add("[官方规则引用缺失][高危] 未检索到 douyin_weigui 官方违规规则引用，禁止判定为审核通过");
            result.setViolations(violations);
            result.setViolationCount(violations.size());
            result.setPassed(false);
        }
    }

    private int getNextSequenceNo(Long sessionId) {
        Integer max = scriptRepository.findMaxSequenceNoBySessionId(sessionId);
        return (max != null ? max : 0) + 1;
    }

    private List<DyProduct> getSessionProducts(Long sessionId) {
        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(sessionId);
        List<Long> productIds = liveProducts.stream()
                .map(LiveProduct::getProductId)
                .collect(Collectors.toList());
        if (productIds.isEmpty()) return new ArrayList<>();
        return productRepository.findAllById(productIds);
    }

    private List<String> buildCategoryQueries(cn.gaifan.douyinOperations.module.product.entity.DyProduct product, String scriptType, String style) {
        List<String> queries = new ArrayList<>();
        String typeTerm = mapScriptTypeToQuery(scriptType);
        StringBuilder q1 = new StringBuilder();
        if (product != null && product.getProductCategory() != null) q1.append(product.getProductCategory()).append(" ");
        q1.append(typeTerm);
        if (style != null && !style.isBlank()) q1.append(" ").append(style);
        queries.add(q1.toString().trim());
        if (product != null && product.getProductName() != null) {
            StringBuilder q2 = new StringBuilder(product.getProductName());
            if (product.getAiSellingPoints() != null && !product.getAiSellingPoints().isBlank()) {
                String sp = product.getAiSellingPoints().split("[,，]")[0].trim();
                if (!sp.isEmpty()) q2.append(" ").append(sp);
            }
            queries.add(q2.toString().trim());
        }
        if (style != null && !style.isBlank()) queries.add(style + " " + typeTerm + " 案例");
        return queries.stream().filter(q -> !q.isEmpty()).distinct().toList();
    }

    private static String mapScriptTypeToQuery(String scriptType) {
        if (scriptType == null) return "话术";
        return switch (scriptType) {
            case "seed" -> "种草话术";
            case "promotion" -> "促销话术";
            case "formal" -> "产品介绍";
            case "emotional" -> "情绪价值话术";
            case "transition" -> "过渡话术";
            case "opening" -> "开场话术";
            case "closing" -> "收尾话术";
            default -> "话术";
        };
    }

    /** BR-09：引用 douyin 产品话术，不调用 AI */
    private LiveAiResultVO useProductScript(Long sessionId, Long productId, Long productScriptId, String style) {
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        DyProductScript ps = productScriptRepository.findById(productScriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品话术不存在"));
        String content = ps.getScriptContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "产品话术内容为空");
        }
        LiveScript script = new LiveScript();
        script.setSessionId(sessionId);
        script.setScriptContent(content);
        script.setScriptType("product");
        script.setStyle(style);
        script.setAiGenerated(0);
        applyNewScriptDefaults(script, session.getUserId(), "success");
        script.setProductId(productId);
        script.setSequenceNo(getNextSequenceNo(sessionId));
        scriptRepository.save(script);
        LiveAiResultVO.ViolationCheckResult check = doViolationCheck(content, session.getUserId(), "live");
        LiveAiResultVO result = new LiveAiResultVO();
        result.setContent(content);
        result.setScriptType("product");
        result.setViolationCheck(check);
        return result;
    }

    // ─── 产品 AI 话术生成 ──────────────────────────────────────

    @Override
    public cn.gaifan.douyinOperations.module.live.vo.ProductScriptResultVO generateProductScript(
            cn.gaifan.douyinOperations.module.live.vo.ProductScriptGenerateVO vo, Long userId) {

        long startTime = System.currentTimeMillis();

        // 获取产品信息
        DyProduct product = productRepository.findById(vo.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));

        // 获取人设信息
        DyPersona persona = null;
        if (vo.getPersonaId() != null) {
            persona = personaRepository.findByIdAndDeleted(vo.getPersonaId(), 0).orElse(null);
        }

        // 查找可用的 AI 模型
        AiModel model = findAvailableModel();
        if (model == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无可用 AI 模型");
        }

        String prompt = promptBuilder.buildProductScriptPrompt(product, persona, vo);
        boolean useRag = vo.getUseKbRef() == null || Boolean.TRUE.equals(vo.getUseKbRef());
        RagContextResult ragResult = useRag ? buildRagContext(userId, product, vo.getScriptType(), vo.getStyle(), prompt.length(), vo.getKbCategories()) : null;
        if (ragResult != null && ragResult.xml() != null && !ragResult.xml().isBlank()) {
            prompt = prompt + "\n\n" + ragResult.xml() + "\n请参考以上案例的表达方式，生成原创话术。\n";
        }
        String systemPrompt = promptBuilder.buildProductScriptSystemPrompt(vo.getScriptType());

        // 调用 LLM 生成
        String scriptContent;
        int tokenUsage = 0;
        try {
            LlmClient.LlmResponse response = llmClient.chat(model, systemPrompt, prompt);
            if (!response.success() || response.content() == null || response.content().isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 生成失败: " + response.errorMsg());
            }
            scriptContent = response.content().trim();
            tokenUsage = (int) Math.min(response.tokensUsed(), Integer.MAX_VALUE);

            // 更新模型用量
            if (tokenUsage > 0) {
                aiModelRepository.incrementQuotaUsed(model.getId(), (long) tokenUsage);
            }
        } catch (Exception e) {
            log.error("产品话术生成失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 生成失败: " + e.getMessage());
        }

        long generationTime = System.currentTimeMillis() - startTime;

        // 构建结果
        cn.gaifan.douyinOperations.module.live.vo.ProductScriptResultVO result =
                new cn.gaifan.douyinOperations.module.live.vo.ProductScriptResultVO();
        result.setScriptContent(scriptContent);
        result.setScriptType(vo.getScriptType());
        result.setStyle(vo.getStyle());
        result.setProductId(product.getId());
        result.setProductName(product.getProductName());
        if (persona != null) {
            result.setPersonaId(persona.getId());
            result.setPersonaName(persona.getPersonaName());
        }
        result.setGenerationTime(generationTime);
        result.setTokenUsage(tokenUsage);
        if (ragResult != null && !ragResult.refs().isEmpty()) {
            result.setRagRefs(toRagRefVOs(ragResult.refs()));
        }

        log.info("产品话术生成成功: productId={}, type={}, tokens={}, time={}ms",
                product.getId(), vo.getScriptType(), tokenUsage, generationTime);

        return result;
    }

    @Override
    public LiveAiResultVO generateEmotionalScript(EmotionalScriptGenerateVO vo, Long userId) {
        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        AiModel model = findAvailableModel();
        if (model == null) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        String systemPrompt = promptBuilder.buildEmotionalSystemPrompt();
        String userPrompt = promptBuilder.buildEmotionalUserPrompt(vo, session);

        try {
            LlmClient.LlmResponse response = llmClient.chat(model, systemPrompt, userPrompt);
            if (!response.success() || response.content() == null || response.content().isBlank()) {
                throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "AI 生成失败: " + response.errorMsg());
            }
            String content = response.content().trim();
            if (response.tokensUsed() > 0) {
                aiModelRepository.incrementQuotaUsed(model.getId(), response.tokensUsed());
            }

            LiveAiResultVO result = new LiveAiResultVO();
            result.setContent(content);
            result.setScriptType("emotional");
            result.setViolationCheck(doViolationCheck(content, userId, "live"));
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("情绪话术生成失败", e);
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "AI 生成失败: " + e.getMessage());
        }
    }

}
