package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.event.LiveScriptGeneratedEvent;
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
import cn.gaifan.douyinOperations.module.live.service.LivePromptBuilder;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import cn.gaifan.douyinOperations.common.util.Sha256Hex;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 直播话术槽位生成器：封装单槽位上下文构建、VO 组装、逐槽位生成与持久化逻辑。
 * 从 LiveScriptGenerationServiceImpl 提取，减少主类体积。
 */
@Component
public class LiveScriptSlotGenerator {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptSlotGenerator.class);

    @Resource private LiveSessionRepository sessionRepository;
    @Resource private LiveScriptRepository scriptRepository;
    @Resource private DyPersonaRepository personaRepository;
    @Resource private DyProductRepository productRepository;
    @Resource private LiveProductRepository liveProductRepository;
    @Resource private ProductService productService;
    @Resource private DyProductScriptRepository productScriptRepository;
    @Resource private LlmClient llmClient;
    @Resource private LivePromptBuilder promptBuilder;
    @Resource private ApplicationEventPublisher eventPublisher;
    @Resource private LiveAiModelHelper modelHelper;
    @Resource private LiveScriptLlmUserPromptComposer llmUserPromptComposer;
    @Resource private LiveScriptQualityService liveScriptQualityService;
    @Resource private LiveScriptPostProcessor postProcessor;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private cn.gaifan.douyinOperations.module.live.service.ScriptQualityEvaluator scriptQualityEvaluator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private cn.gaifan.douyinOperations.module.live.service.ContentMaterialService contentMaterialService;

    // ─── 记录：槽位上下文 ──────────────────────────────────────

    record SlotContext(
        LiveScript slot,
        LiveSession session,
        DyPersona persona,
        List<LiveProduct> liveProducts,
        Map<Long, DyProduct> productMap,
        List<LiveScript> allSlots,
        int slotIndex
    ) {}

    // ─── 槽位上下文构建 ──────────────────────────────────────

    SlotContext buildSlotContext(Long scriptId) {
        LiveScript slot = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        LiveSession session = sessionRepository.findById(slot.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        Long personaId = session.getPersonaId();
        DyPersona persona = personaId != null ? personaRepository.findByIdAndDeleted(personaId, 0).orElse(null) : null;
        List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(slot.getSessionId()).stream()
                .sorted((a, b) -> Integer.compare(a.getPosition() != null ? a.getPosition() : 0, b.getPosition() != null ? b.getPosition() : 0))
                .toList();
        Set<Long> productIdsForMap = new HashSet<>(liveProducts.stream().map(LiveProduct::getProductId).toList());
        if (slot.getProductId() != null) productIdsForMap.add(slot.getProductId());
        Map<Long, DyProduct> productMap = productIdsForMap.isEmpty() ? Collections.emptyMap()
                : productRepository.findAllById(new ArrayList<>(productIdsForMap)).stream()
                        .collect(Collectors.toMap(DyProduct::getId, p -> p));
        List<LiveScript> allSlots = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(slot.getSessionId(), 0);
        int slotIndex = -1;
        for (int i = 0; i < allSlots.size(); i++) {
            if (allSlots.get(i).getId().equals(scriptId)) {
                slotIndex = i;
                break;
            }
        }
        return new SlotContext(slot, session, persona, liveProducts, productMap, allSlots, slotIndex);
    }

    // ─── 槽位 VO 构建 ──────────────────────────────────────

    LiveAiGenerateVO buildSlotVo(SlotContext ctx, String requirementOverride, Integer durationSecOverride, Long modelId) {
        LiveScript slot = ctx.slot();
        LiveSession session = ctx.session();
        String scriptType = slot.getScriptType() != null ? slot.getScriptType() : "custom";
        String requirement = (requirementOverride != null && !requirementOverride.isBlank())
                ? requirementOverride
                : (slot.getRequirement() != null && !slot.getRequirement().isBlank() ? slot.getRequirement() : LiveScriptPostProcessor.defaultRequirement(scriptType));
        Integer durationSec = (durationSecOverride != null && durationSecOverride > 0)
                ? durationSecOverride
                : (slot.getDurationLimitSec() != null && slot.getDurationLimitSec() > 0 ? slot.getDurationLimitSec() : null);
        String style = slot.getStyle() != null && !slot.getStyle().isBlank() ? slot.getStyle() : null;
        if (style == null && session.getScriptStyle() != null && !session.getScriptStyle().isBlank()) {
            style = session.getScriptStyle();
        }
        LiveAiGenerateVO vo = new LiveAiGenerateVO();
        vo.setSessionId(session.getId());
        vo.setPersonaId(session.getPersonaId());
        vo.setStyle(style);
        vo.setRequirement(requirement);
        vo.setDurationLimitSec(durationSec);
        vo.setProductId(slot.getProductId());
        vo.setModelId(modelId);

        if ("transition".equals(scriptType) && ctx.slotIndex() >= 2) {
            int fromIdx = (ctx.slotIndex() - 2) / 2;
            int toIdx = fromIdx + 1;
            if (fromIdx >= 0 && toIdx < ctx.liveProducts().size()) {
                DyProduct from = ctx.productMap().get(ctx.liveProducts().get(fromIdx).getProductId());
                DyProduct to = ctx.productMap().get(ctx.liveProducts().get(toIdx).getProductId());
                if (from != null && to != null) {
                    vo.setFromProductName(from.getProductName());
                    vo.setToProductName(to.getProductName());
                }
            }
        }

        // 自动推断 timeSlot（按槽位位置与总槽数映射到120分钟时间线）
        if (vo.getTimeSlot() == null && ctx.slotIndex() >= 0 && !ctx.allSlots().isEmpty()) {
            double ratio = (double) ctx.slotIndex() / ctx.allSlots().size();
            if (ratio < 0.125) vo.setTimeSlot("0-15min");
            else if (ratio < 0.375) vo.setTimeSlot("15-45min");
            else if (ratio < 0.625) vo.setTimeSlot("45-75min");
            else if (ratio < 0.875) vo.setTimeSlot("75-105min");
            else vo.setTimeSlot("105-120min");
        }

        // 自动推断 ipType（从场次关联人设的 liveStyle 推断）
        if (vo.getIpType() == null && ctx.persona() != null) {
            String liveStyle = ctx.persona().getLiveStyle();
            if ("high_energy".equals(liveStyle)) {
                vo.setIpType("phenomenal");
            } else if ("deep_value".equals(liveStyle)) {
                vo.setIpType("top");
            }
        }

        // 自动推断 scriptModule（按话术类型和时段推断四大模块）
        if (vo.getScriptModule() == null) {
            String ts = vo.getTimeSlot();
            if ("opening".equals(scriptType) || "0-15min".equals(ts)) {
                vo.setScriptModule("emotion_drive");
            } else if ("product".equals(scriptType)) {
                vo.setScriptModule("value_creation");
            } else if ("closing".equals(scriptType) || "105-120min".equals(ts)) {
                vo.setScriptModule("trust_reinforcement");
            } else if ("45-75min".equals(ts) || "75-105min".equals(ts)) {
                vo.setScriptModule("conversion_engine");
            }
        }

        return vo;
    }

    // ─── 转场产品解析 ──────────────────────────────────────

    /** 根据槽位列表解析转场的前后产品名，支持 chat 穿插结构 */
    LiveScriptPostProcessor.TransitionProductPair resolveTransitionProducts(List<LiveScript> allSlots, int transitionSlotIndex, Map<Long, DyProduct> productMap) {
        List<LiveScript> productSlots = allSlots.stream()
                .filter(s -> "product".equals(s.getScriptType()) && s.getProductId() != null)
                .toList();
        int transitionCount = 0;
        for (int i = 0; i < transitionSlotIndex; i++) {
            if ("transition".equals(allSlots.get(i).getScriptType())) transitionCount++;
        }
        if (transitionCount < 0 || transitionCount >= productSlots.size() - 1) return null;
        DyProduct from = productMap.get(productSlots.get(transitionCount).getProductId());
        DyProduct to = productMap.get(productSlots.get(transitionCount + 1).getProductId());
        if (from == null || to == null) return null;
        return new LiveScriptPostProcessor.TransitionProductPair(from.getProductName(), to.getProductName());
    }

    // ─── 槽位生成（含消费计算） ──────────────────────────────────────

    LiveScriptPostProcessor.SlotResult generateAndUpdateSlotWithConsumption(LiveScript slot, LiveSession session, DyPersona persona,
            LiveAiGenerateVO baseVo, String globalStyle, List<LiveProduct> liveProducts,
            Map<Long, DyProduct> productMap, int slotIndex, List<LiveScript> allSlots) {
        LiveAiResultVO r = generateAndUpdateSlot(slot, session, persona, baseVo, globalStyle, liveProducts, productMap, slotIndex, allSlots);
        double consumption = (slot.getReferencedScriptId() != null) ? 0.0 : 1.0;
        return new LiveScriptPostProcessor.SlotResult(r, consumption);
    }

    // ─── 核心槽位生成与更新 ──────────────────────────────────────

    @SuppressWarnings("unchecked")
    LiveAiResultVO generateAndUpdateSlot(LiveScript slot, LiveSession session, DyPersona persona,
            LiveAiGenerateVO baseVo, String globalStyle, List<LiveProduct> liveProducts,
            Map<Long, DyProduct> productMap, int slotIndex, List<LiveScript> allSlots) {
        String scriptType = slot.getScriptType() != null ? slot.getScriptType() : "custom";
        Long productId = slot.getProductId();
        String requirement = slot.getRequirement() != null && !slot.getRequirement().isBlank()
                ? slot.getRequirement() : LiveScriptPostProcessor.defaultRequirement(scriptType);
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
        vo.setModelId(baseVo.getModelId());
        vo.setHotKeywords(baseVo.getHotKeywords());
        vo.setIpType(baseVo.getIpType());
        vo.setScriptModule(baseVo.getScriptModule());
        vo.setTimeSlot(baseVo.getTimeSlot());
        vo.setMaterialType(baseVo.getMaterialType());
        vo.setRetentionStrategy(baseVo.getRetentionStrategy());
        vo.setInteractionLevel(baseVo.getInteractionLevel());
        vo.setPromptTemplateId(baseVo.getPromptTemplateId());

        // 留人策略调度器：按槽位时间位置注入悬念/高潮/技巧节点
        llmUserPromptComposer.injectRetentionNode(vo, slotIndex);
        if (slotIndex >= 0) {
            vo.setSlotIndex(slotIndex);
        }

        if ("product".equals(scriptType) && (productId == null || productMap.get(productId) == null)) {
            log.warn("话术生成跳过 slotId={} type=product productId={}: 商品未关联或已删除", slot.getId(), productId);
            slot.setScriptContent("");
            slot.setGenerationPromptHash(null);
            slot.setGenerationStatus("failed");
            scriptRepository.save(slot);
            LiveAiResultVO r = new LiveAiResultVO();
            r.setContent("");
            r.setScriptType("product");
            LiveAiResultVO.ViolationCheckResult check = new LiveAiResultVO.ViolationCheckResult();
            check.setPassed(false);
            check.setViolationCount(0);
            r.setViolationCheck(check);
            return r;
        }

        if ("product".equals(scriptType) && productId != null) {
            LiveProduct lp = liveProducts.stream().filter(p -> productId.equals(p.getProductId())).findFirst().orElse(null);
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
                        slot.setGenerationPromptHash(null);
                        slot.setReferencedScriptId(ps.getId());
                        slot.setReferencedScriptSnapshot(postProcessor.buildReferencedSnapshot(ps));
                        Map<String, Object> productHints = liveScriptQualityService.applyGenerationQualityHints(slot);
                        scriptRepository.save(slot);
                        eventPublisher.publishEvent(new LiveScriptGeneratedEvent(this, session.getId(), slot.getId(), "product", session.getUserId()));
                        LiveAiResultVO r = new LiveAiResultVO();
                        r.setContent(content);
                        r.setScriptType("product");
                        Map<String, Object> productDur = (Map<String, Object>) productHints.get("durationFit");
                        Map<String, Object> productDup = (Map<String, Object>) productHints.get("duplicateCheck");
                        r.setDurationFit(productDur);
                        r.setDuplicateCheck(productDup);
                        r.setViolationCheck(liveScriptQualityService.checkViolation(content, session.getUserId(), "live"));
                        return r;
                    }
                }
            }
        }
        if ("transition".equals(scriptType) && allSlots != null) {
            var pair = resolveTransitionProducts(allSlots, slotIndex, productMap);
            if (pair != null) {
                vo.setFromProductName(pair.from());
                vo.setToProductName(pair.to());
            }
        }

        try {
            cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO abAssign = postProcessor.tryAssignAbStyle(session);
            if (abAssign != null) {
                vo.setStyle(abAssign.getStyleCode());
            }

            LiveScriptPostProcessor.GenerateResult gen = generateWithLlm(scriptType, session, persona, vo);
            String content = gen.content();
            slot.setScriptContent(content);
            slot.setScriptType(scriptType);
            slot.setStyle(abAssign != null ? abAssign.getStyleCode() : style);
            slot.setRequirement(requirement);
            slot.setDurationLimitSec(durationSec);
            slot.setAiGenerated(1);
            slot.setGenerationPromptHash(gen.generationPromptHash());
            slot.setGenerationStatus("success");
            if (productId != null) slot.setProductId(productId);
            slot.setReferencedScriptId(null);
            slot.setReferencedScriptSnapshot(null);
            if (abAssign != null) {
                slot.setAbExperimentId(abAssign.getExperimentId());
                slot.setAbVariantId(abAssign.getVariantId());
            }
            if (vo.getAbExperimentId() != null) {
                slot.setAbExperimentId(vo.getAbExperimentId());
            }
            if (vo.getAbVariantId() != null) {
                slot.setAbVariantId(vo.getAbVariantId());
            }
            if (vo.getPromptTemplateId() != null) {
                slot.setPromptTemplateId(vo.getPromptTemplateId());
            }
            Map<String, Object> slotHints = liveScriptQualityService.applyGenerationQualityHints(slot);
            scriptRepository.save(slot);
            eventPublisher.publishEvent(new LiveScriptGeneratedEvent(this, session.getId(), slot.getId(), scriptType, session.getUserId()));
            LiveAiResultVO r = new LiveAiResultVO();
            r.setContent(content);
            r.setScriptType(scriptType);
            Map<String, Object> slotDur = (Map<String, Object>) slotHints.get("durationFit");
            Map<String, Object> slotDup = (Map<String, Object>) slotHints.get("duplicateCheck");
            r.setDurationFit(slotDur);
            r.setDuplicateCheck(slotDup);
            r.setViolationCheck(liveScriptQualityService.checkViolation(content, session.getUserId(), "live"));
            r.setRagRefs(LiveScriptPostProcessor.toRagRefVOs(gen.ragRefs()));
            r.setReferencedChunkIds(LiveScriptPostProcessor.toReferencedChunkIdsJson(gen.ragRefs()));
            r.setScriptIdForAttribution(slot.getId());
            r.setSessionIdForAttribution(session.getId());
            r.setPerformanceGuides(liveScriptQualityService.extractPerformanceGuides(content));

            // 生成后自动质量评估（异步，不阻塞返回）
            if (scriptQualityEvaluator != null && content.length() > 10) {
                try {
                    Map<String, Object> evalResult = scriptQualityEvaluator.evaluate(content, vo.getIpType(), session.getUserId());
                    Object totalScoreObj = evalResult.get("totalScore");
                    double totalScore = totalScoreObj instanceof Number n ? n.doubleValue() : 0;
                    r.setQualityScore(totalScore);
                    r.setQualityGrade(evalResult.get("grade") instanceof String g ? g : null);
                    if (totalScore < 6.0) {
                        log.info("话术质量偏低: slotId={}, score={}, grade={}", slot.getId(), totalScore, evalResult.get("grade"));
                    }
                    if (totalScore >= 8.0) {
                        postProcessor.autoIngestHighQualityScript(content, scriptType, session.getUserId(), totalScore);
                    }
                } catch (Exception qe) {
                    log.debug("自动质量评估跳过: {}", qe.getMessage());
                }
            }

            return r;
        } catch (Exception e) {
            log.warn("话术生成失败 slotId={} type={}, 标记 failed: {}", slot.getId(), scriptType, e.getMessage());
            slot.setScriptContent("");
            slot.setGenerationPromptHash(null);
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

    // ─── LLM 生成（内部调用） ──────────────────────────────────────

    LiveScriptPostProcessor.GenerateResult generateWithLlm(String scriptType, LiveSession session, DyPersona persona, LiveAiGenerateVO vo) {
        if (vo == null) {
            vo = new LiveAiGenerateVO();
        }
        cn.gaifan.douyinOperations.module.ai.entity.AiModel model = modelHelper.findAvailableModel(vo.getModelId());
        if (model == null) {
            log.warn("无可用 AI 模型，使用模板生成话术");
            return new LiveScriptPostProcessor.GenerateResult(promptBuilder.buildScriptTemplate(scriptType, session, persona, vo), List.of(), null);
        }

        LiveScriptLlmUserPromptComposer.AugmentedUserPrompt aug = llmUserPromptComposer.augmentAfterBasePrompt(scriptType, session, persona, vo);
        String prompt = aug.userPrompt();
        List<KnowledgeBaseService.SearchResult> ragRefs = aug.ragRefs();

        String systemPrompt = promptBuilder.getSystemPrompt(null, session.getLiveFormat());
        String promptFingerprint = Sha256Hex.fingerprintSystemAndUser(systemPrompt, prompt);
        try {
            LlmClient.LlmResponse response = llmClient.chat(model, systemPrompt, prompt);
            if (response.success() && response.content() != null && !response.content().isBlank()) {
                if (response.tokensUsed() > 0) {
                    modelHelper.incrementQuotaUsed(model.getId(), response.tokensUsed());
                }
                log.info("LLM 话术生成成功: type={}, model={}, tokens={}", scriptType, model.getModelVersion(), response.tokensUsed());
                return new LiveScriptPostProcessor.GenerateResult(response.content().trim(), ragRefs, promptFingerprint);
            }
            log.warn("LLM 生成失败: {}, fallback 到模板", response.errorMsg());
        } catch (Exception e) {
            log.error("LLM 调用异常, fallback 到模板", e);
        }

        return new LiveScriptPostProcessor.GenerateResult(promptBuilder.buildScriptTemplate(scriptType, session, persona, vo), ragRefs, promptFingerprint);
    }
}
