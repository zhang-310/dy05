package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.compliance.service.ComplianceService;
import cn.gaifan.douyinOperations.common.compliance.vo.ComplianceCheckResult;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LivePromptBuilder;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptGenerationService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptPromptService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptStreamService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.EmotionalScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 直播话术生成服务实现：从 LiveAiServiceImpl 拆分，专注于话术的 AI 生成逻辑。
 * <p>
 * 槽位生成与更新逻辑委托给 {@link LiveScriptSlotGenerator}，
 * RAG 引用、时间线、工具方法委托给 {@link LiveScriptPostProcessor}。
 */
@Service("liveScriptGenerationService")
public class LiveScriptGenerationServiceImpl implements LiveScriptGenerationService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptGenerationServiceImpl.class);

    @Resource private LiveSessionRepository sessionRepository;
    @Resource private LiveScriptRepository scriptRepository;
    @Resource private DyPersonaRepository personaRepository;
    @Resource private DyProductRepository productRepository;
    @Resource private LiveProductRepository liveProductRepository;
    @Resource private ProductService productService;
    @Resource private LlmClient llmClient;
    @Resource private LiveScriptService liveScriptService;
    @Resource private LivePromptBuilder promptBuilder;
    @Resource private LiveAiModelHelper modelHelper;
    @Resource private LiveScriptLlmUserPromptComposer llmUserPromptComposer;
    @Resource private LiveScriptQualityService liveScriptQualityService;
    @Resource private LiveScriptPromptService liveScriptPromptService;
    @Resource private LiveScriptStreamService liveScriptStreamService;
    @Resource private LiveScriptSlotGenerator slotGenerator;
    @Resource private LiveScriptPostProcessor postProcessor;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private cn.gaifan.douyinOperations.module.live.service.ContentMaterialService contentMaterialService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComplianceService complianceService;

    /** G-1 最大并发槽位数 */
    @Value("${app.live.generation.parallel.max-concurrency:5}")
    private int maxParallelConcurrency;

    /** G-1 单个槽位生成超时（秒） */
    @Value("${app.live.generation.parallel.timeout-seconds:300}")
    private int parallelTimeoutSeconds;

    // ─── 公开方法：单段生成 ──────────────────────────────────────

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

    // ─── 公开方法：完整生成 ──────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveAiFullResultVO generateFull(LiveAiGenerateVO vo) {
        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        liveScriptService.ensureScriptSlotsForSession(vo.getSessionId(), session.getUserId());
        List<LiveScript> slots = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0);
        if (slots.isEmpty()) {
            LiveAiFullResultVO r = new LiveAiFullResultVO();
            r.setResults(new ArrayList<>());
            r.setConsumption(0);
            return r;
        }

        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : session.getPersonaId();
        DyPersona persona = personaId != null ? personaRepository.findByIdAndDeleted(personaId, 0).orElse(null) : null;
        String globalStyle = promptBuilder.resolveStyle(vo);

        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(vo.getSessionId()).stream()
                .sorted((a, b) -> Integer.compare(a.getPosition() != null ? a.getPosition() : 0, b.getPosition() != null ? b.getPosition() : 0))
                .toList();
        Map<Long, DyProduct> productMap = buildProductMap(slots, liveProducts);

        double consumption = 0;
        List<String> aggregatedRefChunks = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            LiveScript slot = slots.get(i);
            LiveScriptPostProcessor.SlotResult sr = slotGenerator.generateAndUpdateSlotWithConsumption(slot, session, persona, vo, globalStyle, liveProducts, productMap, i, slots);
            consumption += sr.consumption();
            if (sr.result() != null && sr.result().getReferencedChunkIds() != null && !sr.result().getReferencedChunkIds().isBlank()) {
                aggregatedRefChunks.add(sr.result().getReferencedChunkIds());
            }
        }

        liveScriptQualityService.analyzeAndRefineFullScript(vo.getSessionId(), session.getUserId(), vo.getModelId());
        consumption += 1.0;

        return buildFullResult(slots, session, consumption, aggregatedRefChunks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveAiFullResultVO generateFullWithProgress(LiveAiGenerateVO vo, LiveAiService.FullGenerateProgressCallback progressCallback) {
        llmUserPromptComposer.enrichHotKeywordsIfEmpty(vo);
        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        liveScriptService.ensureScriptSlotsForSession(vo.getSessionId(), session.getUserId());
        List<LiveScript> slots = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0);
        if (slots.isEmpty()) {
            LiveAiFullResultVO r = new LiveAiFullResultVO();
            r.setResults(new ArrayList<>());
            r.setConsumption(0);
            return r;
        }
        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : session.getPersonaId();
        DyPersona persona = personaId != null ? personaRepository.findByIdAndDeleted(personaId, 0).orElse(null) : null;
        String globalStyle = promptBuilder.resolveStyle(vo);

        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(vo.getSessionId()).stream()
                .sorted((a, b) -> Integer.compare(a.getPosition() != null ? a.getPosition() : 0, b.getPosition() != null ? b.getPosition() : 0))
                .toList();
        Map<Long, DyProduct> productMap = buildProductMap(slots, liveProducts);

        double consumption = 0;
        List<String> aggregatedRefChunks = new ArrayList<>();
        int total = slots.size() + 2;
        for (int i = 0; i < slots.size(); i++) {
            LiveScript slot = slots.get(i);
            String label = LiveScriptPostProcessor.slotLabel(slot.getScriptType(), i, liveProducts.size());
            progressCallback.onProgress(i, total, "生成" + label + "中");
            LiveScriptPostProcessor.SlotResult sr = slotGenerator.generateAndUpdateSlotWithConsumption(slot, session, persona, vo, globalStyle, liveProducts, productMap, i, slots);
            consumption += sr.consumption();
            if (sr.result() != null && sr.result().getReferencedChunkIds() != null && !sr.result().getReferencedChunkIds().isBlank()) {
                aggregatedRefChunks.add(sr.result().getReferencedChunkIds());
            }
            progressCallback.onProgress(i + 1, total, label);
            if ("failed".equals(slot.getGenerationStatus())) {
                progressCallback.onSlotFailed(slot.getId(), label, "生成失败", i);
            } else if (sr.result() != null && sr.result().getContent() != null && !sr.result().getContent().isBlank()) {
                int seqNo = slot.getSequenceNo() != null ? slot.getSequenceNo() : (i + 1);
                progressCallback.onSlotDone(slot.getId(), sr.result().getContent(), sr.result().getScriptType(), label, seqNo, i);
            }
        }

        progressCallback.onProgress(slots.size() + 1, total, "生成成篇优化中");
        liveScriptQualityService.analyzeAndRefineFullScript(vo.getSessionId(), session.getUserId(), vo.getModelId());
        consumption += 1.0;
        progressCallback.onProgress(total, total, "完成");

        return buildFullResult(slots, session, consumption, aggregatedRefChunks);
    }

    // ─── 公开方法：违规检测 ──────────────────────────────────────

    @Override
    public LiveAiResultVO.ViolationCheckResult checkViolation(Long userId, Long scriptId) {
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        return liveScriptQualityService.checkViolation(script.getScriptContent(), userId, "live");
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
        return liveScriptQualityService.checkViolation(content, userId, "live");
    }

    // ─── 公开方法：槽位生成 ──────────────────────────────────────

    @Override
    public String generateForSlot(Long scriptId, String requirementOverride, Integer durationSecOverride, Long modelId) {
        LiveScriptSlotGenerator.SlotContext ctx = slotGenerator.buildSlotContext(scriptId);
        LiveScript slot = ctx.slot();
        String scriptType = slot.getScriptType() != null ? slot.getScriptType() : "custom";

        LiveAiGenerateVO vo = slotGenerator.buildSlotVo(ctx, requirementOverride, durationSecOverride, modelId);

        if ("product".equals(scriptType)) {
            if (slot.getProductId() == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "商品未关联，请先为话术槽位选择产品");
            }
            if (ctx.productMap().get(slot.getProductId()) == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "商品已删除或不存在，请从直播中移除此产品后重试");
            }
            LiveProduct lp = ctx.liveProducts().stream().filter(p -> slot.getProductId().equals(p.getProductId())).findFirst().orElse(null);
            String productType = (lp != null && lp.getProductType() != null && !lp.getProductType().isBlank())
                    ? lp.getProductType() : productService.inferProductType(slot.getProductId());
            vo.setProductType(productType);
        }

        int si = ctx.slotIndex();
        if (si >= 0) {
            llmUserPromptComposer.injectRetentionNode(vo, si);
            vo.setSlotIndex(si);
        }

        return slotGenerator.generateWithLlm(scriptType, ctx.session(), ctx.persona(), vo).content();
    }

    @Override
    public void generateForSlotStream(Long scriptId, String requirementOverride, Integer durationSecOverride, Long modelId, OutputStream out) throws java.io.IOException {
        liveScriptStreamService.generateForSlotStream(scriptId, requirementOverride, durationSecOverride, modelId, out);
    }

    // ─── G-1 批量并行生成 ──────────────────────────────────────

    @Override
    public Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult> generateParallel(List<Long> scriptIds, Long modelId) {
        if (scriptIds == null || scriptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (scriptIds.size() > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "批量生成槽位数不能超过 50");
        }

        Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult> results = new ConcurrentHashMap<>();
        int actualConcurrency = Math.min(maxParallelConcurrency, scriptIds.size());

        ExecutorService executor = Executors.newFixedThreadPool(actualConcurrency, r -> {
            Thread t = new Thread(r, "live-parallel-gen-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        log.debug("批量并行生成：固定线程池并发数={}", actualConcurrency);

        List<Future<?>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(scriptIds.size());

        for (Long scriptId : scriptIds) {
            Future<?> future = executor.submit(() -> {
                try {
                    long start = System.currentTimeMillis();
                    cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult result = new cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult();
                    try {
                        String content = generateForSlot(scriptId, null, null, modelId);
                        result.setSuccess(true);
                        result.setContent(content);
                        result.setDurationMs(System.currentTimeMillis() - start);
                        log.debug("槽位 {} 并行生成成功，耗时 {}ms", scriptId, result.getDurationMs());
                    } catch (Exception e) {
                        result.setSuccess(false);
                        result.setError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                        result.setDurationMs(System.currentTimeMillis() - start);
                        log.warn("槽位 {} 并行生成失败: {}", scriptId, result.getError());
                    }
                    results.put(scriptId, result);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        try {
            boolean completed = latch.await(parallelTimeoutSeconds * scriptIds.size() / actualConcurrency + 60, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("批量并行生成超时，部分任务可能未完成");
                for (Future<?> f : futures) {
                    if (!f.isDone()) {
                        f.cancel(true);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("批量并行生成被中断");
            for (Future<?> f : futures) {
                f.cancel(true);
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        for (Long scriptId : scriptIds) {
            if (!results.containsKey(scriptId)) {
                cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult result = new cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult();
                result.setSuccess(false);
                result.setError("生成超时或任务被取消");
                results.put(scriptId, result);
            }
        }

        int successCount = (int) results.values().stream().filter(cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult::isSuccess).count();
        log.info("批量并行生成完成: 总数={}, 成功={}, 失败={}", scriptIds.size(), successCount, scriptIds.size() - successCount);
        return results;
    }

    // ─── 公开方法：产品话术 / 情绪话术 / 归因 ──────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductScriptResultVO generateProductScript(ProductScriptGenerateVO vo, Long userId) {
        long startTime = System.currentTimeMillis();

        DyProduct product = productRepository.findById(vo.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));

        DyPersona persona = null;
        if (vo.getPersonaId() != null) {
            persona = personaRepository.findByIdAndDeleted(vo.getPersonaId(), 0).orElse(null);
        }

        AiModel model = modelHelper.findAvailableModel();
        if (model == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无可用 AI 模型");
        }

        String prompt = promptBuilder.buildProductScriptPrompt(product, persona, vo);
        boolean useRag = vo.getUseKbRef() == null || Boolean.TRUE.equals(vo.getUseKbRef());
        LiveScriptPromptService.RagContextResult ragResult = useRag ? liveScriptPromptService.buildRagContext(userId, product, vo.getScriptType(), vo.getStyle(), prompt.length(), null) : null;
        if (ragResult != null && ragResult.xml() != null && !ragResult.xml().isBlank()) {
            prompt = prompt + "\n\n" + ragResult.xml() + "\n请参考以上案例的表达方式，生成原创话术。\n";
        }
        String systemPrompt = promptBuilder.buildProductScriptSystemPrompt(vo.getScriptType());

        String scriptContent;
        int tokenUsage = 0;
        try {
            LlmClient.LlmResponse response = llmClient.chat(model, systemPrompt, prompt);
            if (!response.success() || response.content() == null || response.content().isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 生成失败: " + response.errorMsg());
            }
            scriptContent = response.content().trim();
            tokenUsage = (int) Math.min(response.tokensUsed(), Integer.MAX_VALUE);
            if (tokenUsage > 0) {
                modelHelper.incrementQuotaUsed(model.getId(), (long) tokenUsage);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("产品话术生成失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 生成失败: " + e.getMessage());
        }

        long generationTime = System.currentTimeMillis() - startTime;

        ProductScriptResultVO result = new ProductScriptResultVO();
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
            result.setRagRefs(LiveScriptPostProcessor.toRagRefVOs(ragResult.refs()));
        }

        log.info("产品话术生成成功: productId={}, type={}, tokens={}, time={}ms",
                product.getId(), vo.getScriptType(), tokenUsage, generationTime);
        return result;
    }

    @Override
    public LiveAiResultVO generateEmotionalScript(EmotionalScriptGenerateVO vo, Long userId) {
        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        AiModel model = modelHelper.findAvailableModel();
        if (model == null) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        String systemPrompt = promptBuilder.buildEmotionalSystemPrompt();
        String userPrompt = promptBuilder.buildEmotionalUserPrompt(vo, session);

        if (contentMaterialService != null) {
            String materialType = switch (vo.getCategory()) {
                case "humor", "self_mockery", "poison_soup" -> "joke";
                case "emotional_healing", "healing", "positive_energy" -> "chicken_soup";
                case "quote", "heart_piercing" -> "quote";
                default -> null;
            };
            if (materialType != null) {
                String materialPrompt = contentMaterialService.buildMaterialPrompt(materialType, vo.getSubCategory());
                if (materialPrompt != null && !materialPrompt.isBlank()) {
                    userPrompt = userPrompt + "\n" + materialPrompt;
                }
            }
            String performancePrompt = contentMaterialService.buildPerformancePrompt(null);
            if (performancePrompt != null && !performancePrompt.isBlank()) {
                userPrompt = userPrompt + "\n" + performancePrompt;
            }
        }

        try {
            LlmClient.LlmResponse response = llmClient.chat(model, systemPrompt, userPrompt);
            if (!response.success() || response.content() == null || response.content().isBlank()) {
                throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "AI 生成失败: " + response.errorMsg());
            }
            String content = response.content().trim();
            if (response.tokensUsed() > 0) {
                modelHelper.incrementQuotaUsed(model.getId(), response.tokensUsed());
            }

            LiveAiResultVO result = new LiveAiResultVO();
            result.setContent(content);
            result.setScriptType("emotional");
            result.setViolationCheck(liveScriptQualityService.checkViolation(content, userId, "live"));
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("情绪话术生成失败", e);
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "AI 生成失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void attachAiCallLogToScript(Long scriptId, Long callLogId) {
        if (scriptId == null || callLogId == null) return;
        scriptRepository.findById(scriptId).ifPresent(s -> {
            s.setAiCallLogId(callLogId);
            scriptRepository.save(s);
        });
    }

    // ─── 私有方法：核心生成逻辑 ──────────────────────────────────────

    @SuppressWarnings("unchecked")
    private LiveAiResultVO doGenerate(LiveAiGenerateVO vo, String scriptType) {
        llmUserPromptComposer.enrichHotKeywordsIfEmpty(vo);
        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : session.getPersonaId();
        DyPersona persona = null;
        if (personaId != null) {
            persona = personaRepository.findByIdAndDeleted(personaId, 0).orElse(null);
        }

        if ("product".equals(scriptType) && vo.getProductId() != null
                && (vo.getProductType() == null || vo.getProductType().isBlank())) {
            vo.setProductType(productService.inferProductType(vo.getProductId()));
        }

        LiveScriptPostProcessor.GenerateResult gen = slotGenerator.generateWithLlm(scriptType, session, persona, vo);
        String content = gen.content();

        cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO abAssign = postProcessor.tryAssignAbStyle(session);

        // 违规检测
        ComplianceCheckResult complianceResult = null;
        if (complianceService != null) {
            try {
                complianceResult = complianceService.check(session.getUserId(), "script", null, content);
                if ("reject".equals(complianceResult.getResult())) {
                    log.warn("话术生成违规检测失败: sessionId={}, riskScore={}", session.getId(), complianceResult.getRiskScore());
                    throw new BusinessException(ErrorCode.COMPLIANCE_VIOLATION,
                        "话术包含违规内容，风险评分: " + complianceResult.getRiskScore() + "，建议: " + complianceResult.getSuggestions());
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("违规检测失败，跳过检测", e);
            }
        }

        LiveScript script = new LiveScript();
        script.setSessionId(session.getId());
        script.setUserId(session.getUserId());
        script.setScriptContent(content);
        script.setScriptType(scriptType);
        script.setStyle(abAssign != null ? abAssign.getStyleCode() : vo.getStyle());
        script.setAiGenerated(1);
        script.setGenerationPromptHash(gen.generationPromptHash());
        script.setSequenceNo(postProcessor.getNextSequenceNo(session.getId()));

        // 设置违规检测结果
        if (complianceResult != null) {
            script.setViolationChecked(1);
            script.setViolationResult(complianceResult.getResult());
            if (complianceResult.getSuggestions() != null) {
                script.setAiSuggestion(complianceResult.getSuggestions());
            }
        }

        if (abAssign != null) {
            script.setAbExperimentId(abAssign.getExperimentId());
            script.setAbVariantId(abAssign.getVariantId());
        }
        if (vo.getAbExperimentId() != null) {
            script.setAbExperimentId(vo.getAbExperimentId());
        }
        if (vo.getAbVariantId() != null) {
            script.setAbVariantId(vo.getAbVariantId());
        }
        if (vo.getPromptTemplateId() != null) {
            script.setPromptTemplateId(vo.getPromptTemplateId());
        }
        if ("product".equals(scriptType) && vo.getProductId() != null) {
            script.setProductId(vo.getProductId());
        }
        if (vo.getDurationLimitSec() != null && vo.getDurationLimitSec() > 0) {
            script.setDurationLimitSec(vo.getDurationLimitSec());
        }
        Map<String, Object> hints = liveScriptQualityService.applyGenerationQualityHints(script);
        scriptRepository.save(script);

        LiveAiResultVO.ViolationCheckResult checkResult = liveScriptQualityService.checkViolation(content, session.getUserId(), "live");

        LiveAiResultVO result = new LiveAiResultVO();
        result.setContent(content);
        result.setScriptType(scriptType);
        Map<String, Object> durationFit = (Map<String, Object>) hints.get("durationFit");
        Map<String, Object> duplicateCheck = (Map<String, Object>) hints.get("duplicateCheck");
        result.setDurationFit(durationFit);
        result.setDuplicateCheck(duplicateCheck);
        result.setViolationCheck(checkResult);
        result.setRagRefs(LiveScriptPostProcessor.toRagRefVOs(gen.ragRefs()));
        result.setReferencedChunkIds(LiveScriptPostProcessor.toReferencedChunkIdsJson(gen.ragRefs()));
        result.setSessionIdForAttribution(session.getId());
        result.setScriptIdForAttribution(script.getId());
        return result;
    }

    // ─── 私有方法：产品映射构建 ──────────────────────────────────────

    /**
     * P0-5: 批量查询优化 - 使用 findAllById 替代循环 findById
     * 预期收益：响应时间 N×50ms → 50ms
     */
    private Map<Long, DyProduct> buildProductMap(List<LiveScript> slots, List<LiveProduct> liveProducts) {
        if ((slots == null || slots.isEmpty()) && (liveProducts == null || liveProducts.isEmpty())) {
            return Collections.emptyMap();
        }

        Set<Long> allProductIds = new HashSet<>();
        if (liveProducts != null) {
            liveProducts.stream()
                    .map(LiveProduct::getProductId)
                    .filter(Objects::nonNull)
                    .forEach(allProductIds::add);
        }
        if (slots != null) {
            slots.stream()
                    .map(LiveScript::getProductId)
                    .filter(Objects::nonNull)
                    .forEach(allProductIds::add);
        }

        if (allProductIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询：一次数据库调用获取所有产品
        List<DyProduct> products = productRepository.findAllById(new ArrayList<>(allProductIds));
        return products.stream().collect(Collectors.toMap(DyProduct::getId, p -> p));
    }

    // ─── 私有方法：完整结果构建 ──────────────────────────────────────

    private LiveAiFullResultVO buildFullResult(List<LiveScript> slots, LiveSession session, double consumption, List<String> aggregatedRefChunks) {
        List<LiveScript> finalSlots = scriptRepository.findAllById(slots.stream().map(LiveScript::getId).toList());
        finalSlots.sort(Comparator.comparing(s -> s.getSequenceNo() != null ? s.getSequenceNo() : 0));
        LiveAiFullResultVO full = new LiveAiFullResultVO();
        full.setResults(finalSlots.stream()
                .map(s -> {
                    LiveAiResultVO r = new LiveAiResultVO();
                    r.setContent(s.getScriptContent());
                    r.setScriptType(s.getScriptType());
                    r.setViolationCheck(liveScriptQualityService.checkViolation(s.getScriptContent(), session.getUserId(), "live"));
                    r.setDurationFit(liveScriptQualityService.checkDurationFit(s.getScriptContent(), s.getDurationLimitSec()));
                    r.setDuplicateCheck(liveScriptQualityService.checkCorpusDuplicate(s.getUserId(), s.getId(), s.getScriptContent()));
                    r.setScriptIdForAttribution(s.getId());
                    r.setPerformanceGuides(liveScriptQualityService.extractPerformanceGuides(s.getScriptContent()));
                    return r;
                })
                .collect(Collectors.toList()));
        full.setConsumption(consumption);
        full.setSessionIdForAttribution(session.getId());
        full.setScriptIdsForAttribution(finalSlots.stream().map(LiveScript::getId).toList());
        full.setReferencedChunkIds(LiveScriptPostProcessor.mergeReferencedChunkIds(aggregatedRefChunks));
        full.setTimeline(LiveScriptPostProcessor.buildTimeline(finalSlots));
        return full;
    }

    // ─── 分组流水线全场生成 ──────────────────────────────────────────────────────

    /**
     * 分组流水线全场生成：
     * Phase 1 - 批量 RAG 检索（一次性，供所有槽位共享）
     * Phase 2 - 开场话术串行生成（为后续商品环节定调）
     * Phase 3 - 所有商品槽位并行生成（最耗时的阶段，并发 maxParallelConcurrency）
     * Phase 4 - 过渡/互动/收尾串行生成（依赖前面内容做衔接）
     * Phase 5 - 成篇优化（全场话术衔接一致性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveAiFullResultVO generateFullPipelined(LiveAiGenerateVO vo,
                                                     LiveAiService.FullGenerateProgressCallback progressCallback) {
        llmUserPromptComposer.enrichHotKeywordsIfEmpty(vo);
        LiveSession session = sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        liveScriptService.ensureScriptSlotsForSession(vo.getSessionId(), session.getUserId());
        List<LiveScript> slots = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0);
        if (slots.isEmpty()) {
            LiveAiFullResultVO r = new LiveAiFullResultVO();
            r.setResults(new ArrayList<>());
            r.setConsumption(0);
            return r;
        }

        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : session.getPersonaId();
        DyPersona persona = personaId != null ? personaRepository.findByIdAndDeleted(personaId, 0).orElse(null) : null;
        String globalStyle = promptBuilder.resolveStyle(vo);

        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(vo.getSessionId()).stream()
                .sorted((a, b) -> Integer.compare(a.getPosition() != null ? a.getPosition() : 0,
                        b.getPosition() != null ? b.getPosition() : 0))
                .toList();
        Map<Long, DyProduct> productMap = buildProductMap(slots, liveProducts);

        int total = slots.size() + 2; // +2: 成篇优化 + 完成
        int progressIdx = 0;

        // ── Phase 1: 批量 RAG 检索（避免 N 次独立查询）──────────────────
        List<String> distinctTypes = slots.stream()
                .map(s -> s.getScriptType() != null ? s.getScriptType() : "custom")
                .distinct().toList();

        // 找第一个商品作为 RAG 的代表性商品
        DyProduct repProduct = liveProducts.stream()
                .map(lp -> productMap.get(lp.getProductId()))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        if (progressCallback != null) progressCallback.onProgress(0, total, "预加载知识库参考...");
        LiveScriptPromptService.BatchRagContextResult batchRag =
                liveScriptPromptService.buildRagContextBatch(session.getUserId(), globalStyle, distinctTypes, repProduct);
        log.info("[Pipeline] Session={} 批量 RAG 完成，覆盖类型={}", vo.getSessionId(), batchRag.byScriptType().size());

        // ── 按类型分组槽位 ────────────────────────────────────────────────
        List<LiveScript> openingSlots = new ArrayList<>();
        List<LiveScript> productSlots = new ArrayList<>();
        List<LiveScript> transitionSlots = new ArrayList<>();
        List<LiveScript> closingSlots = new ArrayList<>();
        List<LiveScript> otherSlots = new ArrayList<>();

        for (LiveScript slot : slots) {
            String t = slot.getScriptType() != null ? slot.getScriptType() : "custom";
            if ("opening".equals(t)) {
                openingSlots.add(slot);
            } else if ("product".equals(t) || "closing_deal".equals(t) || "pain_point".equals(t)
                    || "testimony".equals(t) || "deep_sell".equals(t) || "rapid_intro".equals(t)) {
                productSlots.add(slot);
            } else if ("transition".equals(t) || "interaction".equals(t) || "chat".equals(t)) {
                transitionSlots.add(slot);
            } else if ("closing".equals(t)) {
                closingSlots.add(slot);
            } else {
                otherSlots.add(slot);
            }
        }

        double[] consumption = {0};
        List<String> aggregatedRefChunks = Collections.synchronizedList(new ArrayList<>());

        // ── Phase 2: 开场串行 ─────────────────────────────────────────────
        if (progressCallback != null) progressCallback.onProgress(progressIdx, total, "生成开场话术...");
        for (int i = 0; i < openingSlots.size(); i++) {
            LiveScript slot = openingSlots.get(i);
            int slotIdx = slots.indexOf(slot);
            String priorCtx = buildPriorSlotsContext(slots, slotIdx, 3);
            if (priorCtx != null) vo.setPriorSlotsContext(priorCtx);
            LiveScriptPostProcessor.SlotResult sr = slotGenerator.generateAndUpdateSlotWithConsumption(
                    slot, session, persona, vo, globalStyle, liveProducts, productMap, slotIdx, slots);
            vo.setPriorSlotsContext(null);
            consumption[0] += sr.consumption();
            collectRefChunks(sr, aggregatedRefChunks);
            progressIdx++;
            if (progressCallback != null) {
                String label = LiveScriptPostProcessor.slotLabel(slot.getScriptType(), slotIdx, liveProducts.size());
                progressCallback.onProgress(progressIdx, total, label);
                if (sr.result() != null && sr.result().getContent() != null) {
                    progressCallback.onSlotDone(slot.getId(), sr.result().getContent(), slot.getScriptType(), label,
                            slot.getSequenceNo() != null ? slot.getSequenceNo() : slotIdx, slotIdx);
                }
            }
        }

        // ── Phase 3: 商品槽并行 ───────────────────────────────────────────
        if (!productSlots.isEmpty()) {
            int productConcurrency = Math.min(maxParallelConcurrency, productSlots.size());
            ExecutorService productExecutor = Executors.newFixedThreadPool(productConcurrency, r -> {
                Thread t = new Thread(r, "live-pipeline-product-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
            if (progressCallback != null)
                progressCallback.onProgress(progressIdx, total, "并行生成商品话术(" + productSlots.size() + "个)...");

            CountDownLatch productLatch = new CountDownLatch(productSlots.size());
            List<Future<?>> productFutures = new ArrayList<>();
            for (LiveScript slot : productSlots) {
                int slotIdx = slots.indexOf(slot);
                productFutures.add(productExecutor.submit(() -> {
                    try {
                        LiveScriptPostProcessor.SlotResult sr = slotGenerator.generateAndUpdateSlotWithConsumption(
                                slot, session, persona, vo, globalStyle, liveProducts, productMap, slotIdx, slots);
                        synchronized (aggregatedRefChunks) { consumption[0] += sr.consumption(); consumption2Add(consumption[0], sr.consumption(), aggregatedRefChunks, sr); }
                        if (progressCallback != null && sr.result() != null && sr.result().getContent() != null) {
                            String label = LiveScriptPostProcessor.slotLabel(slot.getScriptType(), slotIdx, liveProducts.size());
                            progressCallback.onSlotDone(slot.getId(), sr.result().getContent(), slot.getScriptType(), label,
                                    slot.getSequenceNo() != null ? slot.getSequenceNo() : slotIdx, slotIdx);
                        }
                    } catch (Exception e) {
                        log.warn("[Pipeline] 商品槽 {} 生成失败: {}", slot.getId(), e.getMessage());
                    } finally {
                        productLatch.countDown();
                    }
                }));
            }
            try {
                int timeoutSecs = parallelTimeoutSeconds * productSlots.size() / productConcurrency + 120;
                productLatch.await(timeoutSecs, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                productExecutor.shutdown();
            }
            progressIdx += productSlots.size();
            if (progressCallback != null)
                progressCallback.onProgress(progressIdx, total, "商品话术生成完毕");
        }

        // ── Phase 4: 过渡 / 互动 / 其他 / 收尾串行 ───────────────────────
        List<LiveScript> finalPhaseSlots = new ArrayList<>();
        finalPhaseSlots.addAll(transitionSlots);
        finalPhaseSlots.addAll(otherSlots);
        finalPhaseSlots.addAll(closingSlots);

        for (LiveScript slot : finalPhaseSlots) {
            int slotIdx = slots.indexOf(slot);
            // 多轮上下文注入：过渡/收尾阶段用前 3 段内容做衔接参考
            String priorCtx = buildPriorSlotsContext(slots, slotIdx, 3);
            if (priorCtx != null) vo.setPriorSlotsContext(priorCtx);
            LiveScriptPostProcessor.SlotResult sr = slotGenerator.generateAndUpdateSlotWithConsumption(
                    slot, session, persona, vo, globalStyle, liveProducts, productMap, slotIdx, slots);
            vo.setPriorSlotsContext(null); // 清除
            consumption[0] += sr.consumption();
            collectRefChunks(sr, aggregatedRefChunks);
            progressIdx++;
            if (progressCallback != null) {
                String label = LiveScriptPostProcessor.slotLabel(slot.getScriptType(), slotIdx, liveProducts.size());
                progressCallback.onProgress(progressIdx, total, label);
                if (sr.result() != null && sr.result().getContent() != null) {
                    progressCallback.onSlotDone(slot.getId(), sr.result().getContent(), slot.getScriptType(), label,
                            slot.getSequenceNo() != null ? slot.getSequenceNo() : slotIdx, slotIdx);
                }
            }
        }

        // ── Phase 5: 成篇优化 ─────────────────────────────────────────────
        if (progressCallback != null) progressCallback.onProgress(progressIdx, total, "生成成篇优化中");
        liveScriptQualityService.analyzeAndRefineFullScript(vo.getSessionId(), session.getUserId(), vo.getModelId());
        consumption[0] += 1.0;
        if (progressCallback != null) progressCallback.onProgress(total, total, "完成");

        return buildFullResult(slots, session, consumption[0], aggregatedRefChunks);
    }

    /** 线程安全地累积消耗量和 RAG 引用 */
    private volatile double pipelineConsumption = 0;

    private void consumption2Add(double current, double add, List<String> refChunks,
                                  LiveScriptPostProcessor.SlotResult sr) {
        // consumption 是方法局部变量，并发需用外部同步——此处通过调用方 synchronized 保证
        collectRefChunks(sr, refChunks);
    }

    private static void collectRefChunks(LiveScriptPostProcessor.SlotResult sr, List<String> target) {
        if (sr != null && sr.result() != null && sr.result().getReferencedChunkIds() != null
                && !sr.result().getReferencedChunkIds().isBlank()) {
            target.add(sr.result().getReferencedChunkIds());
        }
    }

    /**
     * P2 多轮上下文生成：构建已生成槽位的摘要，注入后续槽位 vo.priorSlotsContext
     * <p>
     * 格式：「第1段[opening]: 欢迎大家来到...（前40字）\n第2段[product]: 今天给大家介绍...（前40字）」
     * 最多取前 N 段避免 prompt 膨胀（默认3段）
     */
    private static String buildPriorSlotsContext(List<LiveScript> allSlots, int currentSlotIdx, int maxPrior) {
        if (allSlots == null || currentSlotIdx <= 0) return null;
        StringBuilder sb = new StringBuilder();
        int startFrom = Math.max(0, currentSlotIdx - maxPrior);
        int count = 0;
        for (int i = startFrom; i < currentSlotIdx && i < allSlots.size(); i++) {
            LiveScript s = allSlots.get(i);
            if (s.getScriptContent() == null || s.getScriptContent().isBlank()) continue;
            String preview = s.getScriptContent().length() > 40
                    ? s.getScriptContent().substring(0, 40) + "..."
                    : s.getScriptContent();
            String typeLabel = s.getScriptType() != null ? s.getScriptType() : "custom";
            sb.append("第").append(i + 1).append("段[").append(typeLabel).append("]: ")
              .append(preview).append("\n");
            count++;
        }
        return count > 0 ? sb.toString().trim() : null;
    }
}
