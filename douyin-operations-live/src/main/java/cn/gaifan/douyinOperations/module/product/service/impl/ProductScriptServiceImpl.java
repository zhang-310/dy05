package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.service.ProductAiService;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.service.BatchProgressCallback;
import cn.gaifan.douyinOperations.module.product.service.ComplianceService;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptRateLimitService;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptService;
import cn.gaifan.douyinOperations.module.product.service.ScriptVersionHistoryService;
import cn.gaifan.douyinOperations.common.event.ProductScriptUpdatedEvent;
import cn.gaifan.douyinOperations.module.product.vo.BatchGenerateRequestVO;
import cn.gaifan.douyinOperations.module.product.vo.MultiStyleGenerateRequestVO;
import cn.gaifan.douyinOperations.module.product.vo.MultiStyleGenerateResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptSaveVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;

/** 允许的风格与话术类型（v2.0） */
class ProductScriptConstants {
    static final Set<String> ALLOWED_STYLES = Set.of("professional", "friendly", "passionate", "seeding", "promotion",
            "enthusiastic", "casual", "warm");
    static final Set<String> ALLOWED_SCRIPT_TYPES = Set.of("seed", "promotion", "formal");
}

@Service
public class ProductScriptServiceImpl implements ProductScriptService {

    private static final Logger log = LoggerFactory.getLogger(ProductScriptServiceImpl.class);
    private static final int MAX_STYLES_PER_REQUEST = 10;

    @Resource
    private DyProductScriptRepository scriptRepository;

    @Resource
    private DyProductRepository productRepository;

    @Resource
    private ProductAiService productAiService;

    @Resource
    private ComplianceService complianceService;

    @Resource
    private ProductScriptRateLimitService rateLimitService;

    @Resource
    private ScriptVersionHistoryService scriptVersionHistoryService;

    /**
     * 自调用事务方法（如 saveProductScriptWithLock）需走代理；用 ObjectProvider 替代 @Lazy 自注入，
     * 避免 Spring Boot DevTools 重启场景下 CGLIB Lazy 代理出现 ClassCastException。
     */
    @Resource
    private ObjectProvider<ProductScriptServiceImpl> selfProvider;

    @Resource
    @Qualifier("aiTaskExecutor")
    private Executor aiTaskExecutor;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.abtest.service.ScriptStyleAbService scriptStyleAbService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private AiModelRepository aiModelRepository;

    @Resource
    private LlmClient llmClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DyProductScript saveScript(ProductScriptSaveVO vo, Long userId) {
        // 锁定产品行，避免并发生成时版本号冲突
        DyProduct product = productRepository.findByIdForUpdate(vo.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));

        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此产品");
        }

        // 获取版本号（v2.0 有 style 时按 productId+scriptType+style 维度）
        String style = normalizeStyle(vo.getStyle());
        Integer maxVersion = style != null
                ? scriptRepository.findMaxVersionByProductIdAndScriptTypeAndStyle(vo.getProductId(), vo.getScriptType(), style)
                : scriptRepository.findMaxVersionByProductIdAndScriptType(vo.getProductId(), vo.getScriptType());
        int version = (maxVersion != null ? maxVersion : 0) + 1;

        // 创建话术
        DyProductScript script = new DyProductScript();
        script.setProductId(vo.getProductId());
        script.setScriptType(vo.getScriptType());
        script.setScriptContent(vo.getScriptContent());
        script.setPersonaId(vo.getPersonaId());
        script.setStyle(style);
        script.setVersion(version);
        script.setDuration(vo.getDuration());
        script.setTokenUsage(vo.getTokenUsage());
        script.setCreatedBy(userId);
        script.setSource(vo.getSource() != null ? vo.getSource() : "manual");
        script.setScene(vo.getScene());

        // 如果设置为激活，先取消同类型（同风格）的其他激活状态
        if (vo.getIsActive() != null && vo.getIsActive()) {
            if (style != null) {
                scriptRepository.deactivateByProductIdAndScriptTypeAndStyle(vo.getProductId(), vo.getScriptType(), style);
            } else {
                scriptRepository.deactivateAllByProductIdAndScriptType(vo.getProductId(), vo.getScriptType());
            }
            script.setIsActive(true);
        } else {
            script.setIsActive(false);
        }

        script = scriptRepository.save(script);
        scriptVersionHistoryService.saveHistory(script, userId);
        eventPublisher.publishEvent(new ProductScriptUpdatedEvent(this, script.getId(), script.getProductId(), script.getScriptType(), style, userId));

        log.info("产品话术保存成功: productId={}, type={}, version={}",
                vo.getProductId(), vo.getScriptType(), version);

        return script;
    }

    @Override
    public List<DyProductScript> listScripts(Long productId, Long userId) {
        // 验证产品权限
        verifyProductAccess(productId, userId);
        return scriptRepository.findByProductIdAndDeletedOrderByCreateTimeDesc(productId, 0);
    }

    @Override
    public List<DyProductScript> listScriptsByType(Long productId, String scriptType, Long userId) {
        // 验证产品权限
        verifyProductAccess(productId, userId);
        return scriptRepository.findByProductIdAndScriptTypeAndDeletedOrderByVersionDesc(
                productId, scriptType, 0);
    }

    @Override
    public List<DyProductScript> listActiveScripts(Long productId, Long userId) {
        // 验证产品权限
        verifyProductAccess(productId, userId);
        return scriptRepository.findByProductIdAndIsActiveAndDeleted(productId, true, 0);
    }

    @Override
    public Map<String, List<DyProductScript>> listScriptsByStyle(Long productId, String scriptType, Long userId) {
        verifyProductAccess(productId, userId);
        validateScriptType(scriptType);
        List<DyProductScript> all = scriptRepository.findByProductIdAndScriptTypeAndDeletedOrderByVersionDesc(
                productId, scriptType, 0);
        return all.stream().collect(Collectors.groupingBy(s -> normalizeStyle(s.getStyle()) != null ? s.getStyle() : "default"));
    }

    @Override
    public Map<String, DyProductScript> getActiveScriptsByStyle(Long productId, String scriptType, Long userId) {
        verifyProductAccess(productId, userId);
        validateScriptType(scriptType);
        List<DyProductScript> active = scriptRepository.findByProductIdAndScriptTypeAndIsActiveAndDeleted(
                productId, scriptType, true, 0);
        Map<String, DyProductScript> map = new LinkedHashMap<>();
        for (DyProductScript s : active) {
            String style = normalizeStyle(s.getStyle()) != null ? s.getStyle() : "default";
            map.put(style, s);
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MultiStyleGenerateResultVO generateMultiStyleScripts(MultiStyleGenerateRequestVO vo, Long userId) {
        rateLimitService.tryAcquire(userId);
        validateScriptType(vo.getScriptType());
        if (vo.getStyles() == null || vo.getStyles().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_STYLE_INVALID, "至少选择一个风格");
        }
        if (vo.getStyles().size() > MAX_STYLES_PER_REQUEST) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_BATCH_TOO_LARGE,
                    "单次最多生成 " + MAX_STYLES_PER_REQUEST + " 个风格");
        }
        for (String s : vo.getStyles()) {
            if (!ProductScriptConstants.ALLOWED_STYLES.contains(s)) {
                throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_STYLE_INVALID, "不支持的风格: " + s);
            }
        }

        // 锁定产品行，避免并发生成时版本号冲突
        DyProduct product = productRepository.findByIdForUpdate(vo.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权限操作此产品");
        }

        MultiStyleGenerateResultVO result = new MultiStyleGenerateResultVO();
        result.setProductId(product.getId());
        result.setProductName(product.getProductName());
        result.setScriptType(vo.getScriptType());

        // A/B 实验集成：如果有运行中的 script_style 实验，使用实验分配的风格
        List<String> finalStyles = vo.getStyles();
        if (scriptStyleAbService != null && !vo.getStyles().isEmpty()) {
            try {
                String userFingerprint = "user_" + userId + "_product_" + vo.getProductId();
                cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO assignment =
                    scriptStyleAbService.assignStyle(userId, "product", vo.getProductId(), userFingerprint);
                if (assignment != null && assignment.getStyleCode() != null) {
                    log.info("[AB实验] productId={}, 分配风格={}, experimentId={}, variantId={}",
                        vo.getProductId(), assignment.getStyleCode(), assignment.getExperimentId(), assignment.getVariantId());
                    finalStyles = List.of(assignment.getStyleCode());
                    result.setAbExperimentId(assignment.getExperimentId());
                    result.setAbVariantId(assignment.getVariantId());
                }
            } catch (Exception e) {
                log.warn("[AB实验] 分配失败，使用默认风格: {}", e.getMessage());
            }
        }

        int duration = vo.getDuration() != null && vo.getDuration() > 0 ? vo.getDuration() : 60;

        // 风格融合模式：多风格融合为一条话术
        if (vo.getFusionMode() != null && vo.getFusionMode() && finalStyles.size() >= 2) {
            return generateFusionScript(vo, finalStyles, product, duration, userId, result);
        }

        // 普通模式：每个风格独立生成
        for (String style : finalStyles) {
            MultiStyleGenerateResultVO.StyleResult sr = new MultiStyleGenerateResultVO.StyleResult();
            sr.setStyle(style);
            try {
                ProductAiService.ProductScriptAiResult aiResult = productAiService.generateScript(
                        vo.getProductId(), vo.getScriptType(), style, vo.getPersonaId(), duration, userId, vo.getUseKbRef(), vo.getScene(), vo.getKbCategories());
                String content = aiResult.scriptContent();
                if (content == null || content.isBlank()) {
                    sr.setSuccess(false);
                    sr.setErrorMessage("AI 返回内容为空");
                    result.getResults().add(sr);
                    continue;
                }

                // 合规检测
                ComplianceService.ComplianceResult comp = complianceService.checkAndFix(content);
                if (!comp.passed()) {
                    sr.setSuccess(false);
                    sr.setErrorMessage("合规检测未通过: " + String.join(", ", comp.violations()));
                    result.getResults().add(sr);
                    continue;
                }
                content = comp.fixedText();

                DyProductScript script = selfProvider.getObject().saveProductScriptWithLock(vo.getProductId(), vo.getScriptType(), style,
                        content, vo.getPersonaId(), duration, aiResult.tokenUsage(), userId, vo.getScene());

                sr.setSuccess(true);
                sr.setScript(script);
                result.getResults().add(sr);
                log.info("[ScriptGenerate] productId={}, style={}, version={}", vo.getProductId(), style, script != null ? script.getVersion() : null);
            } catch (BusinessException e) {
                sr.setSuccess(false);
                sr.setErrorMessage(e.getMessage());
                result.getResults().add(sr);
            } catch (Exception e) {
                log.error("产品话术生成失败: productId={}, style={}", vo.getProductId(), style, e);
                sr.setSuccess(false);
                sr.setErrorMessage("生成异常: " + e.getMessage());
                result.getResults().add(sr);
            }
        }
        return result;
    }

    /**
     * 风格融合生成：多风格融合为一条话术
     */
    private MultiStyleGenerateResultVO generateFusionScript(MultiStyleGenerateRequestVO vo, List<String> styles,
            DyProduct product, int duration, Long userId, MultiStyleGenerateResultVO result) {
        MultiStyleGenerateResultVO.StyleResult sr = new MultiStyleGenerateResultVO.StyleResult();
        sr.setStyle(String.join("+", styles)); // 融合风格标识

        try {
            // 1. 计算各风格的时长分配（基于权重）
            Map<String, Integer> styleDurations = calculateStyleDurations(styles, vo.getStyleWeights(), duration);

            // 2. 为每个风格生成独立话术片段
            List<String> styleContents = new ArrayList<>();
            int totalTokens = 0;

            for (String style : styles) {
                int styleDuration = styleDurations.getOrDefault(style, duration / styles.size());
                ProductAiService.ProductScriptAiResult aiResult = productAiService.generateScript(
                        vo.getProductId(), vo.getScriptType(), style, vo.getPersonaId(), styleDuration,
                        userId, vo.getUseKbRef(), vo.getScene(), vo.getKbCategories());
                if (aiResult.scriptContent() != null && !aiResult.scriptContent().isBlank()) {
                    styleContents.add(aiResult.scriptContent());
                    totalTokens += aiResult.tokenUsage();
                }
            }

            if (styleContents.isEmpty()) {
                sr.setSuccess(false);
                sr.setErrorMessage("所有风格生成均失败");
                result.getResults().add(sr);
                return result;
            }

            // 3. 使用 AI 融合多个风格片段（传入权重信息和融合策略）
            String fusedContent = fuseStyleContents(product, vo.getScriptType(), styles, styleContents,
                    duration, userId, vo.getStyleWeights(), vo.getFusionStrategy());
            if (fusedContent == null || fusedContent.isBlank()) {
                sr.setSuccess(false);
                sr.setErrorMessage("风格融合失败");
                result.getResults().add(sr);
                return result;
            }

            // 4. 合规检测
            ComplianceService.ComplianceResult comp = complianceService.checkAndFix(fusedContent);
            if (!comp.passed()) {
                sr.setSuccess(false);
                sr.setErrorMessage("合规检测未通过: " + String.join(", ", comp.violations()));
                result.getResults().add(sr);
                return result;
            }
            fusedContent = comp.fixedText();

            // 4. 保存融合话术（style 字段记录所有风格）
            DyProductScript script = selfProvider.getObject().saveProductScriptWithLock(vo.getProductId(), vo.getScriptType(),
                    String.join("+", styles), fusedContent, vo.getPersonaId(), duration, totalTokens, userId, vo.getScene());

            sr.setSuccess(true);
            sr.setScript(script);
            result.getResults().add(sr);
            log.info("[FusionGenerate] productId={}, styles=, version={}", vo.getProductId(), styles, script.getVersion());

        } catch (Exception e) {
            log.error("风格融合生成失败: productId={}, styles={}", vo.getProductId(), styles, e);
            sr.setSuccess(false);
            sr.setErrorMessage("融合失败: " + e.getMessage());
            result.getResults().add(sr);
        }

        return result;
    }

    /**
     * 计算各风格的时长分配（基于权重）
     */
    private Map<String, Integer> calculateStyleDurations(List<String> styles, Map<String, Double> styleWeights, int totalDuration) {
        Map<String, Integer> durations = new HashMap<>();

        // 如果没有提供权重，平均分配
        if (styleWeights == null || styleWeights.isEmpty()) {
            int avgDuration = totalDuration / styles.size();
            for (String style : styles) {
                durations.put(style, avgDuration);
            }
            return durations;
        }

        // 归一化权重（确保总和为1.0）
        double totalWeight = 0.0;
        for (String style : styles) {
            Double weight = styleWeights.get(style);
            if (weight != null && weight > 0) {
                totalWeight += weight;
            }
        }

        // 按权重分配时长
        int allocatedDuration = 0;
        for (int i = 0; i < styles.size(); i++) {
            String style = styles.get(i);
            Double weight = styleWeights.get(style);

            if (weight == null || weight <= 0) {
                // 未指定权重的风格，分配剩余平均时长
                weight = (1.0 - totalWeight) / styles.stream().filter(s -> styleWeights.get(s) == null || styleWeights.get(s) <= 0).count();
            }

            int styleDuration;
            if (i == styles.size() - 1) {
                // 最后一个风格，分配剩余时长
                styleDuration = totalDuration - allocatedDuration;
            } else {
                styleDuration = (int) Math.round(totalDuration * (weight / totalWeight));
                allocatedDuration += styleDuration;
            }

            durations.put(style, Math.max(10, styleDuration)); // 最少10秒
        }

        return durations;
    }

    /**
     * 使用 AI 融合多个风格的话术片段
     */
    private String fuseStyleContents(DyProduct product, String scriptType, List<String> styles,
            List<String> contents, int duration, Long userId, Map<String, Double> styleWeights, String fusionStrategy) {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        String systemPrompt = """
            你是直播话术融合专家。任务：将多个不同风格的话术片段融合为一条连贯、自然的话术。

            要求：
            1. 保留各风格的核心特点，自然过渡
            2. 根据权重控制各风格的占比和影响力
            3. 根据融合策略调整融合方式
            4. 避免风格冲突，确保整体协调
            5. 保持话术流畅性和感染力
            6. 控制在指定时长内
            7. 只输出融合后的话术正文，无标题无解释
            """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("商品：").append(product.getProductName()).append("\n");
        userPrompt.append("话术类型：").append(scriptType).append("\n");
        userPrompt.append("目标时长：").append(duration).append("秒\n");
        userPrompt.append("融合风格：").append(String.join("、", styles)).append("\n");

        // 添加权重信息
        if (styleWeights != null && !styleWeights.isEmpty()) {
            userPrompt.append("风格权重：");
            for (String style : styles) {
                Double weight = styleWeights.get(style);
                if (weight != null) {
                    userPrompt.append(style).append("(").append(String.format("%.0f%%", weight * 100)).append(") ");
                }
            }
            userPrompt.append("\n");
        }

        // 添加融合策略说明
        if (fusionStrategy != null && !fusionStrategy.isBlank()) {
            userPrompt.append("融合策略：").append(getFusionStrategyDescription(fusionStrategy, styles)).append("\n");
        }
        userPrompt.append("\n");

        for (int i = 0; i < styles.size() && i < contents.size(); i++) {
            userPrompt.append("【").append(styles.get(i)).append("风格片段");
            if (styleWeights != null && styleWeights.containsKey(styles.get(i))) {
                userPrompt.append("，权重 ").append(String.format("%.0f%%", styleWeights.get(styles.get(i)) * 100));
            }
            userPrompt.append("】\n");
            userPrompt.append(contents.get(i)).append("\n\n");
        }

        userPrompt.append(getFusionInstructionByStrategy(fusionStrategy, styles, styleWeights));

        LlmClient.LlmResponse response = llmClient.chatWithFallback(models, systemPrompt, userPrompt.toString());
        if (!response.success() || response.content() == null || response.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "AI 融合失败: " + response.errorMsg());
        }

        return response.content().trim();
    }

    /**
     * 获取融合策略描述
     */
    private String getFusionStrategyDescription(String strategy, List<String> styles) {
        if (strategy == null || strategy.isBlank()) {
            return "混合融合";
        }
        return switch (strategy) {
            case "blended" -> "混合融合 - 将多个风格自然混合，整体协调统一";
            case "sequential" -> "顺序融合 - 按风格顺序依次呈现，适合分段讲解";
            case "layered" -> "分层融合 - 主风格为基础，其他风格作为点缀";
            case "alternating" -> "交替融合 - 多个风格交替出现，节奏感强";
            case "progressive" -> "渐进融合 - 从第一风格逐渐过渡到最后风格";
            default -> "混合融合";
        };
    }

    /**
     * 根据融合策略生成融合指令
     */
    private String getFusionInstructionByStrategy(String strategy, List<String> styles, Map<String, Double> styleWeights) {
        if (strategy == null || strategy.isBlank()) {
            strategy = "blended";
        }

        int styleCount = styles.size();
        return switch (strategy) {
            case "blended" -> "请将以上片段自然混合为一条连贯的话术，根据权重控制各风格占比，保留各风格特点，整体协调统一。只输出话术正文：";

            case "sequential" -> {
                StringBuilder sb = new StringBuilder("请按以下顺序分段呈现话术：\n");
                for (int i = 0; i < styles.size(); i++) {
                    String style = styles.get(i);
                    double weight = styleWeights != null && styleWeights.containsKey(style)
                        ? styleWeights.get(style)
                        : 1.0 / styleCount;
                    sb.append("第").append(i + 1).append("段：").append(style)
                      .append("风格（占比").append(String.format("%.0f%%", weight * 100)).append("）\n");
                }
                sb.append("每段风格清晰，过渡自然流畅。只输出话术正文：");
                yield sb.toString();
            }

            case "layered" -> {
                String primaryStyle = styles.get(0);
                String accentStyles = String.join("、", styles.subList(1, styles.size()));
                yield String.format("请以 %s 风格为主基调，在关键位置点缀 %s 等风格元素。主风格贯穿全文，其他风格作为亮点出现。只输出话术正文：",
                    primaryStyle, accentStyles);
            }

            case "alternating" ->
                String.format("请让 %s 等风格交替出现，形成节奏感。每个风格片段简短有力，切换自然不突兀。只输出话术正文：",
                    String.join("、", styles));

            case "progressive" ->
                String.format("请从 %s 风格开始，逐渐过渡到 %s 风格。整体呈现渐进变化，过渡自然流畅。只输出话术正文：",
                    styles.get(0), styles.get(styles.size() - 1));

            default -> "请将以上片段融合为一条连贯的话术，根据权重控制各风格占比，保留各风格特点，自然过渡。只输出话术正文：";
        };
    }

    @Override
    public void generateMultiStyleScriptsWithProgress(MultiStyleGenerateRequestVO vo, Long userId, SseEmitter emitter) {
        aiTaskExecutor.execute(() -> {
            try {
                rateLimitService.tryAcquire(userId);
                validateScriptType(vo.getScriptType());
                if (vo.getStyles() == null || vo.getStyles().isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", "至少选择一个风格"), MediaType.APPLICATION_JSON));
                    emitter.complete();
                    return;
                }
                if (vo.getStyles().size() > MAX_STYLES_PER_REQUEST) {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", "单次最多生成 " + MAX_STYLES_PER_REQUEST + " 个风格"), MediaType.APPLICATION_JSON));
                    emitter.complete();
                    return;
                }

                DyProduct product = productRepository.findById(vo.getProductId()).orElse(null);
                if (product == null || !product.getUserId().equals(userId)) {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", "产品不存在或无权限"), MediaType.APPLICATION_JSON));
                    emitter.complete();
                    return;
                }

                // A/B 实验集成
                Long abExperimentId = null;
                Long abVariantId = null;
                List<String> finalStyles = vo.getStyles();
                if (scriptStyleAbService != null && !vo.getStyles().isEmpty()) {
                    try {
                        String userFingerprint = "user_" + userId + "_product_" + vo.getProductId();
                        cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO assignment =
                            scriptStyleAbService.assignStyle(userId, "product", vo.getProductId(), userFingerprint);
                        if (assignment != null && assignment.getStyleCode() != null) {
                            finalStyles = List.of(assignment.getStyleCode());
                            abExperimentId = assignment.getExperimentId();
                            abVariantId = assignment.getVariantId();
                        }
                    } catch (Exception e) {
                        log.warn("[AB实验] 分配失败: {}", e.getMessage());
                    }
                }

                int duration = vo.getDuration() != null && vo.getDuration() > 0 ? vo.getDuration() : 60;

                // 风格融合模式
                if (vo.getFusionMode() != null && vo.getFusionMode() && finalStyles.size() >= 2) {
                    Map<String, Object> progressData = new HashMap<>();
                    progressData.put("style", String.join("+", finalStyles));
                    progressData.put("status", "loading");
                    progressData.put("fusionMode", true);
                    if (abExperimentId != null) {
                        progressData.put("abExperimentId", abExperimentId);
                        progressData.put("abVariantId", abVariantId);
                    }
                    emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));

                    try {
                        // 计算各风格的时长分配（基于权重）
                        Map<String, Integer> styleDurations = calculateStyleDurations(finalStyles, vo.getStyleWeights(), duration);

                        // 生成各风格片段
                        List<String> styleContents = new ArrayList<>();
                        int totalTokens = 0;
                        for (String style : finalStyles) {
                            int styleDuration = styleDurations.getOrDefault(style, duration / finalStyles.size());
                            ProductAiService.ProductScriptAiResult aiResult = productAiService.generateScript(
                                    vo.getProductId(), vo.getScriptType(), style, vo.getPersonaId(), styleDuration,
                                    userId, vo.getUseKbRef(), vo.getScene(), vo.getKbCategories());
                            if (aiResult.scriptContent() != null && !aiResult.scriptContent().isBlank()) {
                                styleContents.add(aiResult.scriptContent());
                                totalTokens += aiResult.tokenUsage();
                            }
                        }

                        if (styleContents.isEmpty()) {
                            progressData.put("status", "failed");
                            progressData.put("message", "所有风格生成均失败");
                            emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                        } else {
                            // AI 融合（传入权重信息和融合策略）
                            String fusedContent = fuseStyleContents(product, vo.getScriptType(), finalStyles, styleContents,
                                    duration, userId, vo.getStyleWeights(), vo.getFusionStrategy());
                            ComplianceService.ComplianceResult comp = complianceService.checkAndFix(fusedContent);
                            if (!comp.passed()) {
                                progressData.put("status", "failed");
                                progressData.put("message", "合规检测未通过: " + String.join(", ", comp.violations()));
                                emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                            } else {
                                fusedContent = comp.fixedText();
                                DyProductScript script = selfProvider.getObject().saveProductScriptWithLock(vo.getProductId(), vo.getScriptType(),
                                        String.join("+", finalStyles), fusedContent, vo.getPersonaId(), duration, totalTokens, userId, vo.getScene());
                                progressData.put("status", "done");
                                progressData.put("success", true);
                                emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                            }
                        }
                    } catch (Exception e) {
                        log.error("风格融合失败: productId={}, styles={}", vo.getProductId(), finalStyles, e);
                        progressData.put("status", "failed");
                        progressData.put("message", "融合失败: " + e.getMessage());
                        emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                    }
                } else {
                    // 普通模式：每个风格独立生成
                    for (String style : finalStyles) {
                        Map<String, Object> progressData = new HashMap<>();
                        progressData.put("style", style);
                        progressData.put("status", "loading");
                        if (abExperimentId != null) {
                            progressData.put("abExperimentId", abExperimentId);
                            progressData.put("abVariantId", abVariantId);
                        }
                        emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));

                        try {
                            ProductAiService.ProductScriptAiResult aiResult = productAiService.generateScript(
                                    vo.getProductId(), vo.getScriptType(), style, vo.getPersonaId(), duration, userId, vo.getUseKbRef(), vo.getScene(), vo.getKbCategories());
                            String content = aiResult.scriptContent();
                            if (content == null || content.isBlank()) {
                                progressData.put("status", "failed");
                                progressData.put("message", "AI 返回内容为空");
                                emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                                continue;
                            }

                            ComplianceService.ComplianceResult comp = complianceService.checkAndFix(content);
                            if (!comp.passed()) {
                                progressData.put("status", "failed");
                                progressData.put("message", "合规检测未通过: " + String.join(", ", comp.violations()));
                                emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                                continue;
                            }
                            content = comp.fixedText();

                            DyProductScript script = selfProvider.getObject().saveProductScriptWithLock(vo.getProductId(), vo.getScriptType(), style,
                                    content, vo.getPersonaId(), duration, aiResult.tokenUsage(), userId, vo.getScene());

                            progressData.put("status", "done");
                            progressData.put("success", true);
                            emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                        } catch (Exception e) {
                            log.error("生成失败: productId={}, style={}", vo.getProductId(), style, e);
                            progressData.put("status", "failed");
                            progressData.put("message", e.getMessage());
                            emitter.send(SseEmitter.event().name("progress").data(progressData, MediaType.APPLICATION_JSON));
                        }
                    }
                }

                emitter.send(SseEmitter.event().name("done").data(Map.of("status", "ok"), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 生成异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", e.getMessage()), MediaType.APPLICATION_JSON));
                } catch (Exception ex) {
                    log.debug("SSE error事件发送失败: {}", ex.getMessage());
                }
                emitter.complete();
            }
        });
    }

    @Override
    public void generateBatchWithProgress(BatchGenerateRequestVO vo, Long userId, BatchProgressCallback callback) {
        rateLimitService.tryAcquire(userId);
        validateScriptType(vo.getScriptType());
        if (vo.getProductIds() == null || vo.getProductIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "产品列表不能为空");
        }
        if (vo.getProductIds().size() > 100) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_BATCH_TOO_LARGE, "单次最多 100 个产品");
        }
        if (vo.getStyles() == null || vo.getStyles().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_STYLE_INVALID, "至少选择一个风格");
        }
        for (String s : vo.getStyles()) {
            if (!ProductScriptConstants.ALLOWED_STYLES.contains(s)) {
                throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_STYLE_INVALID, "不支持的风格: " + s);
            }
        }

        int duration = vo.getDuration() != null && vo.getDuration() > 0 ? vo.getDuration() : 60;
        int total = vo.getProductIds().size() * vo.getStyles().size();
        AtomicInteger completed = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Long productId : vo.getProductIds()) {
            DyProduct product = productRepository.findById(productId).orElse(null);
            final Long pid = productId;
            final String productName = product != null ? product.getProductName() : "-";
            if (product == null) {
                for (String style : vo.getStyles()) {
                    int c = completed.incrementAndGet();
                    callback.onProgress(c, total, pid, "-", style, false, "产品不存在");
                }
                continue;
            }
            if (!product.getUserId().equals(userId)) {
                for (String style : vo.getStyles()) {
                    int c = completed.incrementAndGet();
                    callback.onProgress(c, total, pid, productName, style, false, "无权限");
                }
                continue;
            }

            for (String style : vo.getStyles()) {
                final String s = style;
                CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                    try {
                        ProductAiService.ProductScriptAiResult aiResult = productAiService.generateScript(
                                pid, vo.getScriptType(), s, vo.getPersonaId(), duration, userId, vo.getUseKbRef(), vo.getScene(), null);
                        String content = aiResult.scriptContent();
                        if (content == null || content.isBlank()) {
                            int c = completed.incrementAndGet();
                            callback.onProgress(c, total, pid, productName, s, false, "AI 返回为空");
                            return;
                        }

                        ComplianceService.ComplianceResult comp = complianceService.checkAndFix(content);
                        if (!comp.passed()) {
                            int c = completed.incrementAndGet();
                            callback.onProgress(c, total, pid, productName, s, false,
                                    "合规未通过: " + String.join(", ", comp.violations()));
                            return;
                        }
                        content = comp.fixedText();

                        selfProvider.getObject().saveProductScriptWithLock(pid, vo.getScriptType(), s, content, vo.getPersonaId(),
                                duration, aiResult.tokenUsage(), userId, vo.getScene());

                        int c = completed.incrementAndGet();
                        callback.onProgress(c, total, pid, productName, s, true, null);
                    } catch (BusinessException e) {
                        int c = completed.incrementAndGet();
                        callback.onProgress(c, total, pid, productName, s, false, e.getMessage());
                    } catch (Exception e) {
                        log.warn("批量生成失败: productId={}, style={}", pid, s, e);
                        int c = completed.incrementAndGet();
                        callback.onProgress(c, total, pid, productName, s, false,
                                e.getMessage() != null ? e.getMessage() : "生成异常");
                    }
                }, aiTaskExecutor);
                futures.add(f);
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateScript(Long scriptId, Long userId) {
        DyProductScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));

        // 验证产品权限
        verifyProductAccess(script.getProductId(), userId);

        // 取消同类型（同风格）的其他激活状态
        String style = normalizeStyle(script.getStyle());
        if (style != null) {
            scriptRepository.deactivateByProductIdAndScriptTypeAndStyle(
                    script.getProductId(), script.getScriptType(), style);
        } else {
            scriptRepository.deactivateAllByProductIdAndScriptType(
                    script.getProductId(), script.getScriptType());
        }

        // 激活当前话术
        script.setIsActive(true);
        scriptRepository.save(script);

        log.info("话术激活成功: scriptId={}, productId={}, type={}",
                scriptId, script.getProductId(), script.getScriptType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DyProductScript updateScript(Long scriptId, ProductScriptSaveVO vo, Long userId) {
        DyProductScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));

        // 验证产品权限
        verifyProductAccess(script.getProductId(), userId);

        // 保存当前版本到历史（更新前）
        scriptVersionHistoryService.saveHistory(script, userId);

        // 更新话术内容
        script.setScriptContent(vo.getScriptContent());
        if (vo.getPersonaId() != null) {
            script.setPersonaId(vo.getPersonaId());
        }
        if (vo.getStyle() != null) {
            script.setStyle(vo.getStyle());
        }
        if (vo.getDuration() != null) {
            script.setDuration(vo.getDuration());
        }

        // 如果设置为激活，先取消同类型（同风格）的其他激活状态
        if (vo.getIsActive() != null && vo.getIsActive() && !script.getIsActive()) {
            String style = normalizeStyle(script.getStyle());
            if (style != null) {
                scriptRepository.deactivateByProductIdAndScriptTypeAndStyle(
                        script.getProductId(), script.getScriptType(), style);
            } else {
                scriptRepository.deactivateAllByProductIdAndScriptType(
                        script.getProductId(), script.getScriptType());
            }
            script.setIsActive(true);
        }

        script = scriptRepository.save(script);
        eventPublisher.publishEvent(new ProductScriptUpdatedEvent(this, script.getId(), script.getProductId(), script.getScriptType(), script.getStyle(), userId));

        log.info("话术更新成功: scriptId={}", scriptId);

        return script;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DyProductScript rollbackToVersion(Long historyId, Long userId) {
        DyProductScript script = scriptVersionHistoryService.rollbackToVersion(historyId, userId);
        eventPublisher.publishEvent(new ProductScriptUpdatedEvent(this, script.getId(), script.getProductId(), script.getScriptType(), script.getStyle(), userId));
        return script;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScript(Long scriptId, Long userId) {
        DyProductScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));

        // 验证产品权限
        verifyProductAccess(script.getProductId(), userId);

        // 软删除
        script.setDeleted(1);
        scriptRepository.save(script);

        log.info("话术删除成功: scriptId={}", scriptId);
    }

    @Override
    public DyProductScript getScript(Long scriptId, Long userId) {
        DyProductScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));

        // 验证产品权限
        verifyProductAccess(script.getProductId(), userId);

        return script;
    }

    private void verifyProductAccess(Long productId, Long userId) {
        DyProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));

        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此产品");
        }
    }

    private static String normalizeStyle(String style) {
        if (style == null || style.isBlank()) return null;
        return style.trim();
    }

    private static void validateScriptType(String scriptType) {
        if (scriptType == null || scriptType.isBlank()) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_TYPE_INVALID, "话术类型不能为空");
        }
        if (!ProductScriptConstants.ALLOWED_SCRIPT_TYPES.contains(scriptType)) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_TYPE_INVALID, "不支持的话术类型: " + scriptType);
        }
    }

    /**
     * 在事务内锁定产品行并保存话术，避免并发生成时版本号冲突
     * @return 保存后的话术（供调用方使用）
     */
    @Transactional(rollbackFor = Exception.class)
    public DyProductScript saveProductScriptWithLock(Long productId, String scriptType, String style, String content,
            Long personaId, int duration, Integer tokenUsage, Long userId, String scene) {
        productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));

        Integer maxVer = scriptRepository.findMaxVersionByProductIdAndScriptTypeAndStyle(
                productId, scriptType, style);
        int version = (maxVer != null ? maxVer : 0) + 1;
        scriptRepository.deactivateByProductIdAndScriptTypeAndStyle(productId, scriptType, style);

        DyProductScript script = new DyProductScript();
        script.setProductId(productId);
        script.setScriptType(scriptType);
        script.setScriptContent(content);
        script.setPersonaId(personaId);
        script.setStyle(style);
        script.setVersion(version);
        script.setIsActive(true);
        script.setDuration(duration);
        script.setTokenUsage(tokenUsage);
        script.setCreatedBy(userId);
        script.setSource("ai");
        script.setScene(scene);
        script = scriptRepository.save(script);

        log.info("[ScriptGenerate] productId={}, style={}, scene={}, version={}", productId, style, scene, version);
        return script;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DyProductScript generateSingleScript(Long productId, String style, Long userId) {
        rateLimitService.tryAcquire(userId);

        // 验证产品权限
        DyProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权限操作此产品");
        }

        // 验证风格
        String normalizedStyle = normalizeStyle(style);
        if (normalizedStyle == null || !ProductScriptConstants.ALLOWED_STYLES.contains(normalizedStyle)) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_STYLE_INVALID, "不支持的风格: " + style);
        }

        // 默认生成种草话术，60秒时长
        String scriptType = "seed";
        int duration = 60;

        try {
            ProductAiService.ProductScriptAiResult aiResult = productAiService.generateScript(
                    productId, scriptType, normalizedStyle, null, duration, userId, true, null, null);
            String content = aiResult.scriptContent();
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 返回内容为空");
            }

            // 合规检测
            ComplianceService.ComplianceResult comp = complianceService.checkAndFix(content);
            if (!comp.passed()) {
                throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_COMPLIANCE_FAIL,
                        "合规检测未通过: " + String.join(", ", comp.violations()));
            }
            content = comp.fixedText();

            DyProductScript script = selfProvider.getObject().saveProductScriptWithLock(productId, scriptType, normalizedStyle,
                    content, null, duration, aiResult.tokenUsage(), userId, null);

            log.info("单个话术生成成功: productId={}, style={}, version={}", productId, normalizedStyle, script.getVersion());
            return script;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("单个话术生成失败: productId={}, style={}", productId, normalizedStyle, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getScriptUsageStatistics(Long productId, Long userId) {
        verifyProductAccess(productId, userId);

        List<DyProductScript> scripts = scriptRepository.findByProductIdAndDeletedOrderByCreateTimeDesc(productId, 0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalScripts", scripts.size());
        stats.put("activeScripts", scripts.stream().filter(DyProductScript::getIsActive).count());

        // 按类型统计
        Map<String, Long> byType = scripts.stream()
                .collect(Collectors.groupingBy(DyProductScript::getScriptType, Collectors.counting()));
        stats.put("byType", byType);

        // 按风格统计
        Map<String, Long> byStyle = scripts.stream()
                .collect(Collectors.groupingBy(s -> normalizeStyle(s.getStyle()) != null ? s.getStyle() : "default",
                        Collectors.counting()));
        stats.put("byStyle", byStyle);

        // 按来源统计
        Map<String, Long> bySource = scripts.stream()
                .collect(Collectors.groupingBy(s -> s.getSource() != null ? s.getSource() : "manual",
                        Collectors.counting()));
        stats.put("bySource", bySource);

        // 平均时长
        double avgDuration = scripts.stream()
                .filter(s -> s.getDuration() != null && s.getDuration() > 0)
                .mapToInt(DyProductScript::getDuration)
                .average()
                .orElse(0.0);
        stats.put("avgDuration", Math.round(avgDuration * 10) / 10.0);

        // 总Token消耗
        int totalTokens = scripts.stream()
                .filter(s -> s.getTokenUsage() != null)
                .mapToInt(DyProductScript::getTokenUsage)
                .sum();
        stats.put("totalTokens", totalTokens);

        return stats;
    }

    @Override
    public Map<String, Object> getScriptStatistics(Long productId, Long userId) {
        verifyProductAccess(productId, userId);

        List<DyProductScript> scripts = scriptRepository.findByProductIdAndDeletedOrderByCreateTimeDesc(productId, 0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", scripts.size());
        stats.put("active", scripts.stream().filter(DyProductScript::getIsActive).count());
        stats.put("inactive", scripts.stream().filter(s -> !s.getIsActive()).count());

        // 最新版本号（按类型）
        Map<String, Integer> latestVersions = new LinkedHashMap<>();
        for (String type : ProductScriptConstants.ALLOWED_SCRIPT_TYPES) {
            Integer maxVer = scriptRepository.findMaxVersionByProductIdAndScriptType(productId, type);
            if (maxVer != null) {
                latestVersions.put(type, maxVer);
            }
        }
        stats.put("latestVersions", latestVersions);

        // 最近创建时间
        scripts.stream()
                .filter(s -> s.getCreateTime() != null)
                .max(Comparator.comparing(DyProductScript::getCreateTime))
                .ifPresent(s -> stats.put("lastCreated", s.getCreateTime()));

        return stats;
    }

    @Override
    public List<cn.gaifan.douyinOperations.module.product.vo.StylePreviewVO> previewStyles(
            MultiStyleGenerateRequestVO vo, Long userId) {
        // 1. 参数校验
        rateLimitService.tryAcquire(userId);
        validateScriptType(vo.getScriptType());
        if (vo.getStyles() == null || vo.getStyles().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_STYLE_INVALID, "至少选择一个风格");
        }
        if (vo.getStyles().size() > MAX_STYLES_PER_REQUEST) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_BATCH_TOO_LARGE,
                    "单次最多预览 " + MAX_STYLES_PER_REQUEST + " 个风格");
        }
        for (String s : vo.getStyles()) {
            if (!ProductScriptConstants.ALLOWED_STYLES.contains(s)) {
                throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_STYLE_INVALID, "不支持的风格: " + s);
            }
        }

        // 2. 获取商品信息
        DyProduct product = productRepository.findById(vo.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权限操作此产品");
        }

        // 3. 为每个风格生成预览片段（固定15秒）
        List<cn.gaifan.douyinOperations.module.product.vo.StylePreviewVO> previews = new ArrayList<>();
        int previewDuration = 15; // 预览固定15秒

        for (String style : vo.getStyles()) {
            try {
                // 生成预览片段
                ProductAiService.ProductScriptAiResult aiResult = productAiService.generateScript(
                        vo.getProductId(),
                        vo.getScriptType(),
                        style,
                        vo.getPersonaId(),
                        previewDuration,
                        userId,
                        vo.getUseKbRef(),
                        vo.getScene(),
                        vo.getKbCategories()
                );

                String content = aiResult.scriptContent();
                if (content == null || content.isBlank()) {
                    content = "预览生成失败：AI 返回内容为空";
                }

                // 获取风格名称
                String styleName = getStyleName(style);

                previews.add(new cn.gaifan.douyinOperations.module.product.vo.StylePreviewVO(
                        style, styleName, content));

                log.info("[StylePreview] productId={}, style={}, contentLength={}",
                        vo.getProductId(), style, content.length());
            } catch (Exception e) {
                log.error("预览风格 {} 失败: {}", style, e.getMessage());
                previews.add(new cn.gaifan.douyinOperations.module.product.vo.StylePreviewVO(
                        style, getStyleName(style), "预览生成失败：" + e.getMessage()));
            }
        }

        return previews;
    }

    /**
     * 获取风格名称
     */
    private String getStyleName(String styleCode) {
        Map<String, String> styleNames = Map.of(
                "professional", "专业",
                "warm", "温暖",
                "enthusiastic", "热情",
                "casual", "随意",
                "friendly", "亲和",
                "passionate", "激情",
                "elegant", "优雅",
                "trendy", "时尚"
        );
        return styleNames.getOrDefault(styleCode, styleCode);
    }
}
