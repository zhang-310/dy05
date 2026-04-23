package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.event.LiveScriptGeneratedEvent;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.Sha256Hex;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.ModelChatStreamService;
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
import cn.gaifan.douyinOperations.module.live.service.LiveScriptStreamService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.abtest.service.ScriptStyleAbService;
import cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 直播话术 SSE 流式生成服务实现：槽位话术流式输出。
 */
@Service
public class LiveScriptStreamServiceImpl implements LiveScriptStreamService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptStreamServiceImpl.class);

    @Resource private LiveScriptRepository scriptRepository;
    @Resource private LiveSessionRepository sessionRepository;
    @Resource private DyPersonaRepository personaRepository;
    @Resource private LiveProductRepository liveProductRepository;
    @Resource private DyProductRepository productRepository;
    @Resource private ProductService productService;
    @Resource private LivePromptBuilder promptBuilder;
    @Resource private LiveAiModelHelper modelHelper;
    @Resource private ModelChatStreamService modelChatStreamService;
    @Resource private ApplicationEventPublisher eventPublisher;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private LiveScriptLlmUserPromptComposer llmUserPromptComposer;
    @Resource private LiveScriptQualityService liveScriptQualityService;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityScoringService qualityScoringService;

    @Autowired(required = false)
    private ScriptStyleAbService scriptStyleAbService;

    private record SlotContext(LiveScript slot, LiveSession session, DyPersona persona,
                               List<LiveProduct> liveProducts, Map<Long, DyProduct> productMap,
                               List<LiveScript> allSlots, int slotIndex) {}

    @Override
    public void generateForSlotStream(Long scriptId, String requirementOverride, Integer durationSecOverride,
                                      Long modelId, OutputStream out) throws java.io.IOException {
        SlotContext ctx = buildSlotContext(scriptId);
        LiveScript slot = ctx.slot();
        LiveSession session = ctx.session();
        DyPersona persona = ctx.persona();
        List<LiveProduct> liveProducts = ctx.liveProducts();
        Map<Long, DyProduct> productMap = ctx.productMap();
        String scriptType = slot.getScriptType() != null ? slot.getScriptType() : "custom";

        if ("product".equals(scriptType)) {
            if (slot.getProductId() == null) {
                modelChatStreamService.writeErrorToStream(out, "商品未关联，请先为话术槽位选择产品");
                return;
            }
            if (productMap.get(slot.getProductId()) == null) {
                modelChatStreamService.writeErrorToStream(out, "商品已删除或不存在");
                return;
            }
        }

        try {
            LiveAiGenerateVO vo = buildSlotVo(ctx, requirementOverride, durationSecOverride, modelId);
            ScriptStyleAssignVO abAssign = tryAssignAbStyle(session);
            if (abAssign != null && abAssign.getStyleCode() != null && !abAssign.getStyleCode().isBlank()) {
                vo.setStyle(abAssign.getStyleCode());
            }

            if ("product".equals(scriptType)) {
                LiveProduct lp = liveProducts.stream().filter(p -> slot.getProductId().equals(p.getProductId())).findFirst().orElse(null);
                String productType = (lp != null && lp.getProductType() != null && !lp.getProductType().isBlank())
                        ? lp.getProductType() : productService.inferProductType(slot.getProductId());
                vo.setProductType(productType);
            }

            int slotIdx = ctx.slotIndex();
            if (slotIdx >= 0) {
                llmUserPromptComposer.injectRetentionNode(vo, slotIdx);
                vo.setSlotIndex(slotIdx);
            }

            AiModel model = modelHelper.findAvailableModel(modelId);
            if (model == null) {
                modelChatStreamService.writeErrorToStream(out, "无可用 AI 模型");
                return;
            }

            LiveScriptLlmUserPromptComposer.AugmentedUserPrompt aug = llmUserPromptComposer.augmentAfterBasePrompt(scriptType, session, persona, vo);
            String userPrompt = aug.userPrompt();

            String systemPrompt = promptBuilder.getSystemPrompt(null, session.getLiveFormat());
            List<Map<String, String>> messages = List.of(
                    Map.<String, String>of("role", "system", "content", systemPrompt),
                    Map.<String, String>of("role", "user", "content", userPrompt)
            );
            Long resolvedModelId = modelHelper.resolveModelIdForStream(modelId);
            if (resolvedModelId == null) {
                modelChatStreamService.writeErrorToStream(out, "无可用 AI 模型");
                return;
            }
            final Long sid = slot.getId();
            final Long sessionId = session.getId();
            final Long uid = session.getUserId();
            final String stype = scriptType;
            String promptFingerprint = Sha256Hex.fingerprintSystemAndUser(systemPrompt, userPrompt);
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            final ScriptStyleAssignVO abForPersist = abAssign;
            modelChatStreamService.streamChatToOutputStream(resolvedModelId, messages, out, content ->
                    tx.executeWithoutResult(status -> persistStreamSlotSuccess(sid, sessionId, uid, content, promptFingerprint, stype, abForPersist)));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Broken pipe") || msg.contains("Connection reset"))) {
                log.debug("客户端断开连接: scriptId={}", scriptId);
            } else {
                log.warn("generate-slot-sse 异常: {}", msg, e);
            }
            try {
                out.write(("event: error\ndata: " + new ObjectMapper().writeValueAsString(Map.of("error", msg != null ? msg : "未知错误")) + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception ignored) {}
        }
    }

    private SlotContext buildSlotContext(Long scriptId) {
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
        Map<Long, DyProduct> productMap = productIdsForMap.isEmpty() ? Map.of()
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

    /** 与 {@link LiveScriptGenerationServiceImpl} 同步路径一致：会话级话术风格实验 → 变体风格 + 落库归因字段 */
    private ScriptStyleAssignVO tryAssignAbStyle(LiveSession session) {
        if (scriptStyleAbService == null || session == null) {
            return null;
        }
        try {
            return scriptStyleAbService.assignStyle(
                    session.getUserId(),
                    "live_session",
                    session.getId(),
                    "session_" + session.getId());
        } catch (Exception e) {
            log.warn("A/B 风格分配失败（流式槽位），跳过: {}", e.getMessage());
            return null;
        }
    }

    /** 流式成功结束后：正文 + prompt 指纹落库（与同步 generateWithLlm 一致算法） */
    private void persistStreamSlotSuccess(Long scriptId, Long sessionId, Long userId, String content,
            String promptFingerprint, String scriptTypeLabel, ScriptStyleAssignVO abAssign) {
        LiveScript fresh = scriptRepository.findById(scriptId).orElse(null);
        if (fresh == null) {
            return;
        }
        if (!fresh.getUserId().equals(userId)) {
            log.warn("流式落库跳过: scriptId={} 与当前场次用户不一致", scriptId);
            return;
        }
        fresh.setScriptContent(content);
        fresh.setGenerationPromptHash(promptFingerprint);
        fresh.setAiGenerated(1);
        fresh.setGenerationStatus("success");
        if (abAssign != null) {
            fresh.setAbExperimentId(abAssign.getExperimentId());
            fresh.setAbVariantId(abAssign.getVariantId());
            if (abAssign.getStyleCode() != null && !abAssign.getStyleCode().isBlank()) {
                fresh.setStyle(abAssign.getStyleCode());
            }
        }
        liveScriptQualityService.applyGenerationQualityHints(fresh);
        scriptRepository.save(fresh);
        // 流式路径补充四维加权评分（与同步路径对齐）
        if (qualityScoringService != null) {
            try {
                qualityScoringService.scoreScript(scriptId, userId);
            } catch (Exception e) {
                log.warn("流式话术四维评分失败: scriptId={}, {}", scriptId, e.getMessage());
            }
        }
        String type = scriptTypeLabel != null ? scriptTypeLabel : fresh.getScriptType();
        eventPublisher.publishEvent(new LiveScriptGeneratedEvent(this, sessionId, scriptId, type, userId));
    }

    private LiveAiGenerateVO buildSlotVo(SlotContext ctx, String requirementOverride, Integer durationSecOverride, Long modelId) {
        LiveScript slot = ctx.slot();
        LiveSession session = ctx.session();
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

        if (vo.getTimeSlot() == null && ctx.slotIndex() >= 0 && !ctx.allSlots().isEmpty()) {
            double ratio = (double) ctx.slotIndex() / ctx.allSlots().size();
            if (ratio < 0.125) vo.setTimeSlot("0-15min");
            else if (ratio < 0.375) vo.setTimeSlot("15-45min");
            else if (ratio < 0.625) vo.setTimeSlot("45-75min");
            else if (ratio < 0.875) vo.setTimeSlot("75-105min");
            else vo.setTimeSlot("105-120min");
        }

        if (vo.getIpType() == null && ctx.persona() != null) {
            String liveStyle = ctx.persona().getLiveStyle();
            if ("high_energy".equals(liveStyle)) vo.setIpType("phenomenal");
            else if ("deep_value".equals(liveStyle)) vo.setIpType("top");
        }

        if (vo.getScriptModule() == null) {
            String ts = vo.getTimeSlot();
            if ("opening".equals(scriptType) || "0-15min".equals(ts)) vo.setScriptModule("emotion_drive");
            else if ("product".equals(scriptType)) vo.setScriptModule("value_creation");
            else if ("closing".equals(scriptType) || "105-120min".equals(ts)) vo.setScriptModule("trust_reinforcement");
            else if ("45-75min".equals(ts) || "75-105min".equals(ts)) vo.setScriptModule("conversion_engine");
        }

        return vo;
    }

    private static String defaultRequirement(String scriptType) {
        return switch (scriptType) {
            case "opening" -> "开场白";
            case "product" -> "产品介绍";
            case "transition" -> "转场";
            case "closing" -> "收尾";
            case "chat" -> "聊家常（夫妻/婆媳/励志/歇后语/名言等）";
            case "interaction" -> "互动引导";
            case "welfare" -> "福利话术";
            case "closing_deal" -> "逼单促单";
            case "hold_back" -> "憋单蓄水";
            case "emotional" -> "情绪价值";
            case "rapid_intro" -> "快速过品";
            case "deep_sell" -> "深度单品";
            case "pain_point" -> "痛点放大";
            case "testimony" -> "用户证言";
            case "custom" -> "自定义";
            default -> "互动引导";
        };
    }
}
