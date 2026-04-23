package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.ModelChatStreamService;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LivePromptBuilder;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptAnalysisService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptSkeletonService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.SimilarityItemVO;
import cn.gaifan.douyinOperations.module.live.vo.SkeletonSlotVO;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 直播话术分析与交互服务实现：refine / chat / similarity / skeleton / suggestImprovement
 * 从 LiveAiServiceImpl 拆分，专注于对已有话术的分析、修改、交互。
 */
@Service
public class LiveScriptAnalysisServiceImpl implements LiveScriptAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptAnalysisServiceImpl.class);

    @Resource private LiveScriptRepository scriptRepository;
    @Resource private LiveSessionRepository sessionRepository;
    @Resource private LiveProductRepository liveProductRepository;
    @Resource private LiveScriptService liveScriptService;
    @Resource private LivePromptBuilder promptBuilder;
    @Resource private LlmClient llmClient;
    @Resource private LiveAiModelHelper modelHelper;
    @Resource private ModelChatStreamService modelChatStreamService;
    @Resource private LiveScriptSkeletonService skeletonService;
    @Resource private ProductService productService;
    @Resource private AiModelRepository aiModelRepository;

    private static ObjectMapper objectMapper() {
        return new ObjectMapper();
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
        List<AiModel> models = modelHelper.resolveModels(modelId);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }
        String systemPrompt = """
            你是直播话术修改助手。用户提出修改要求时，在保持原有优点（卖点、感染力）基础上精准调整。
            常见要求：时长压缩、风格切换（促销/亲切/专业）、意图转换（介绍→促单）、语气调整。
            原则：① 保留有效信息 ② 按用户要求调整 ③ 保持口语化、无违禁词 ④ 只输出修改后正文，无解释
            """;
        String userPrompt = String.format("用户要求：%s\n\n当前话术：\n%s\n\n请按要求修改，只输出修改后的话术正文：", userQuestion.trim(), content);
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, resp.errorMsg() != null ? resp.errorMsg() : "AI 修改失败");
        }
        return resp.content().trim();
    }

    @Override
    public String refineSegment(Long scriptId, String segmentText, String instruction, Long userId, Long modelId) {
        if (scriptId == null || scriptId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        }
        if (segmentText == null || segmentText.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "待修改片段不能为空");
        }
        if (instruction == null || instruction.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "修改要求不能为空");
        }
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        LiveSession session = sessionRepository.findById(script.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此话术");
        }
        String content = script.getScriptContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术内容为空，无法修改");
        }
        int idx = content.indexOf(segmentText.trim());
        if (idx < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "未找到匹配片段，请确认选中内容与原文一致");
        }
        List<AiModel> models = modelHelper.resolveModels(modelId);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }
        String systemPrompt = """
            你是直播话术修改助手。用户仅要求修改话术中的某一小段（句子或段落），其余内容保持不变。
            原则：① 仅输出修改后的该片段内容 ② 不输出整段话术 ③ 保持口语化、无违禁词 ④ 按用户要求精准调整
            """;
        String userPrompt = String.format("修改要求：%s\n\n待修改片段：\n%s\n\n请只输出修改后的该片段内容（不要输出整段话术）：",
                instruction.trim(), segmentText.trim());
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, resp.errorMsg() != null ? resp.errorMsg() : "AI 修改失败");
        }
        String modifiedSegment = resp.content().trim();
        String before = content.substring(0, idx);
        String after = content.substring(idx + segmentText.trim().length());
        return before + modifiedSegment + after;
    }

    @Override
    public void refineScriptStream(Long scriptId, String userQuestion, Long userId, java.io.OutputStream out, Long modelId) throws java.io.IOException {
        if (scriptId == null || scriptId <= 0) {
            modelChatStreamService.writeErrorToStream(out, "话术 ID 无效");
            return;
        }
        if (userQuestion == null || userQuestion.isBlank()) {
            modelChatStreamService.writeErrorToStream(out, "修改要求不能为空");
            return;
        }
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        String content = script.getScriptContent();
        if (content == null || content.isBlank()) {
            modelChatStreamService.writeErrorToStream(out, "话术内容为空，无法修改");
            return;
        }
        try {
            String systemPrompt = """
                你是直播话术修改助手。用户提出修改要求时，在保持原有优点（卖点、感染力）基础上精准调整。
                常见要求：时长压缩、风格切换（促销/亲切/专业）、意图转换（介绍→促单）、语气调整。
                原则：① 保留有效信息 ② 按用户要求调整 ③ 保持口语化、无违禁词 ④ 只输出修改后正文，无解释
                """;
            String userPrompt = String.format("用户要求：%s\n\n当前话术：\n%s\n\n请按要求修改，只输出修改后的话术正文：", userQuestion.trim(), content);
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            );
            Long resolvedModelId = modelHelper.resolveModelIdForStream(modelId);
            if (resolvedModelId == null) {
                modelChatStreamService.writeErrorToStream(out, "无可用 AI 模型");
                return;
            }
            modelChatStreamService.streamChatToOutputStream(resolvedModelId, messages, out);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Broken pipe") || msg.contains("Connection reset"))) {
                log.debug("客户端断开连接: scriptId={}", scriptId);
            } else {
                log.warn("refine-script-sse 异常: {}", msg, e);
            }
            try {
                out.write(("event: error\ndata: " + objectMapper().writeValueAsString(Map.of("error", msg != null ? msg : "未知错误")) + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception ex) { log.debug("SSE error通知失败: {}", ex.getMessage()); }
        }
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
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此话术");
        }
        String content = script.getScriptContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术内容为空，无法改进");
        }
        java.math.BigDecimal score = script.getEffectivenessScore();
        if (score == null || score.intValue() >= 70) {
            return null; // 无效果数据或已良好，不生成改进版
        }
        List<AiModel> models = modelHelper.resolveModels(modelId);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }
        String systemPrompt = """
            你是直播话术优化助手。根据话术效果评分（0-100，当前偏低）生成改进版。
            原则：① 保持核心卖点不变 ② 增强吸引力和转化引导 ③ 更口语化、有感染力 ④ 避免违禁词
            ⑤ 只输出改进后话术正文，无解释
            """;
        String userPrompt = String.format(
                "当前话术效果评分：%s（偏低）。\n\n当前话术：\n%s\n\n请优化表达，生成更具吸引力的版本，只输出改进后的话术正文：",
                score, content);
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, resp.errorMsg() != null ? resp.errorMsg() : "AI 改进失败");
        }
        return resp.content().trim();
    }

    @Override
    public String chatForScript(Long scriptId, String userMessage, Long userId, Long modelId) {
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

        List<AiModel> models = modelHelper.resolveModels(modelId);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        List<LiveProduct> products = liveProductRepository.findBySessionId(session.getId()).stream()
                .sorted(java.util.Comparator.comparing(p -> (p.getPosition() != null ? p.getPosition() : 0)))
                .toList();
        Map<Long, Integer> productIdToIndex = new java.util.HashMap<>();
        for (int i = 0; i < products.size(); i++) {
            productIdToIndex.put(products.get(i).getProductId(), i);
        }
        String prevProduct = null;
        String nextProduct = null;
        String currentProductName = null;
        String currentProductTypeLabel = null;
        String productTypeHint = "";

        String scriptType = script.getScriptType() != null ? script.getScriptType() : "custom";
        if ("product".equals(scriptType) && script.getProductId() != null) {
            Integer idx = productIdToIndex.get(script.getProductId());
            if (idx != null) {
                LiveProduct p = products.get(idx);
                currentProductName = p.getProductName();
                currentProductTypeLabel = promptBuilder.formatProductTypeLabel(p.getProductType());
                productTypeHint = promptBuilder.getProductTypePromptHint(p.getProductType());
                if (idx > 0) prevProduct = products.get(idx - 1).getProductName();
                if (idx < products.size() - 1) nextProduct = products.get(idx + 1).getProductName();
            }
        } else if ("transition".equals(scriptType) && script.getProductId() != null) {
            Integer idx = productIdToIndex.get(script.getProductId());
            if (idx != null && idx < products.size() - 1) {
                prevProduct = products.get(idx).getProductName();
                nextProduct = products.get(idx + 1).getProductName();
            }
        } else if ("opening".equals(scriptType) && !products.isEmpty()) {
            nextProduct = products.get(0).getProductName();
        } else if ("closing".equals(scriptType) && !products.isEmpty()) {
            prevProduct = products.get(products.size() - 1).getProductName();
        }

        String typeLabel = "opening".equals(scriptType) ? "开场话术" : "product".equals(scriptType) ? "产品话术"
                : "transition".equals(scriptType) ? "转场话术" : "closing".equals(scriptType) ? "结尾话术"
                : "chat".equals(scriptType) ? "聊家常" : scriptType;
        String req = script.getRequirement() != null && !script.getRequirement().isBlank() ? script.getRequirement() : "";
        String dur = script.getDurationLimitSec() != null && script.getDurationLimitSec() > 0
                ? script.getDurationLimitSec() + "秒" : "";
        String current = script.getScriptContent();
        boolean hasContent = current != null && !current.isBlank() && !"[待填写]".equals(current.trim());

        String systemPrompt = """
            你是直播话术写作助手，输出「即用型」话术供操作人微调。
            产品类型时长规则（必须遵守）：亏品3-10秒；平价品约15秒；爆品1-5分钟；利润品30-60秒。
            人气策略：人气高时主推爆品和高客单价高利润品，炸爆款、做转化；人气低时可用亏品引流。
            产品侧重：爆品→限时抢购/库存紧张；利润品→品质/价值感；亏品→引流/福利；平价品→性价比。
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
    public java.util.Map<Long, String> batchChatForScript(List<Long> scriptIds, String userMessage, Long userId, Long modelId) {
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
            result.put(scriptId, chatForScript(scriptId, userMessage, userId, modelId));
        }
        return result;
    }

    @Override
    public void chatForScriptStream(Long scriptId, String userMessage, Long userId, java.io.OutputStream out, Long modelId) throws java.io.IOException {
        if (scriptId == null || scriptId <= 0) {
            modelChatStreamService.writeErrorToStream(out, "话术 ID 无效");
            return;
        }
        if (userMessage == null || userMessage.isBlank()) {
            modelChatStreamService.writeErrorToStream(out, "请输入您的需求");
            return;
        }
        try {
            List<Map<String, String>> messages = buildChatForScriptMessages(scriptId, userMessage);
            Long resolvedModelId = modelHelper.resolveModelIdForStream(modelId);
            if (resolvedModelId == null) {
                modelChatStreamService.writeErrorToStream(out, "无可用 AI 模型");
                return;
            }
            modelChatStreamService.streamChatToOutputStream(resolvedModelId, messages, out);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Broken pipe") || msg.contains("Connection reset"))) {
                log.debug("客户端断开连接: scriptId={}", scriptId);
            } else {
                log.warn("chat-for-script-sse 异常: {}", msg, e);
            }
            try {
                out.write(("event: error\ndata: " + objectMapper().writeValueAsString(Map.of("error", msg != null ? msg : "未知错误")) + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception ex2) { log.debug("SSE error通知失败: {}", ex2.getMessage()); }
        }
    }

    /** 构建 chatForScript 的 messages，供流式调用复用 */
    private List<Map<String, String>> buildChatForScriptMessages(Long scriptId, String userMessage) {
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        LiveSession session = sessionRepository.findById(script.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        List<LiveProduct> products = liveProductRepository.findBySessionId(session.getId()).stream()
                .sorted(java.util.Comparator.comparing(p -> (p.getPosition() != null ? p.getPosition() : 0)))
                .toList();
        Map<Long, Integer> productIdToIndex = new java.util.HashMap<>();
        for (int i = 0; i < products.size(); i++) {
            productIdToIndex.put(products.get(i).getProductId(), i);
        }
        String prevProduct = null, nextProduct = null, currentProductName = null, currentProductTypeLabel = null, productTypeHint = "";
        String scriptType = script.getScriptType() != null ? script.getScriptType() : "custom";
        if ("product".equals(scriptType) && script.getProductId() != null) {
            Integer idx = productIdToIndex.get(script.getProductId());
            if (idx != null) {
                LiveProduct p = products.get(idx);
                currentProductName = p.getProductName();
                currentProductTypeLabel = promptBuilder.formatProductTypeLabel(p.getProductType());
                productTypeHint = promptBuilder.getProductTypePromptHint(p.getProductType());
                if (idx > 0) prevProduct = products.get(idx - 1).getProductName();
                if (idx < products.size() - 1) nextProduct = products.get(idx + 1).getProductName();
            }
        } else if ("transition".equals(scriptType) && script.getProductId() != null) {
            Integer idx = productIdToIndex.get(script.getProductId());
            if (idx != null && idx < products.size() - 1) {
                prevProduct = products.get(idx).getProductName();
                nextProduct = products.get(idx + 1).getProductName();
            }
        } else if ("opening".equals(scriptType) && !products.isEmpty()) nextProduct = products.get(0).getProductName();
        else if ("closing".equals(scriptType) && !products.isEmpty()) prevProduct = products.get(products.size() - 1).getProductName();
        String typeLabel = "opening".equals(scriptType) ? "开场话术" : "product".equals(scriptType) ? "产品话术"
                : "transition".equals(scriptType) ? "转场话术" : "closing".equals(scriptType) ? "结尾话术"
                : "chat".equals(scriptType) ? "聊家常" : scriptType;
        String req = script.getRequirement() != null && !script.getRequirement().isBlank() ? script.getRequirement() : "";
        String dur = script.getDurationLimitSec() != null && script.getDurationLimitSec() > 0 ? script.getDurationLimitSec() + "秒" : "";
        String current = script.getScriptContent();
        boolean hasContent = current != null && !current.isBlank() && !"[待填写]".equals(current.trim());
        String systemPrompt = """
            你是直播话术写作助手，输出「即用型」话术供操作人微调。
            产品类型时长规则（必须遵守）：亏品3-10秒；平价品约15秒；爆品1-5分钟；利润品30-60秒。
            人气策略：人气高时主推爆品和高客单价高利润品，炸爆款、做转化；人气低时可用亏品引流。
            产品侧重：爆品→限时抢购/库存紧张；利润品→品质/价值感；亏品→引流/福利；平价品→性价比。
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
        if (hasContent) userPrompt.append("当前话术：\n").append(current).append("\n");
        userPrompt.append("\n用户需求：").append(userMessage.trim());
        userPrompt.append("\n\n请根据用户需求").append(hasContent ? "修改" : "生成").append("话术。");
        userPrompt.append("输出即用型正文，操作人将直接粘贴并微调。只输出话术正文，无标题无解释：");
        return List.of(
                Map.<String, String>of("role", "system", "content", systemPrompt),
                Map.<String, String>of("role", "user", "content", userPrompt.toString())
        );
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
                } catch (Exception ignored) {}
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
    public List<SkeletonSlotVO> generateSkeleton(Long sessionId, Long userId, Long modelId) {
        return skeletonService.generateSkeleton(sessionId, userId, modelId);
    }

    @Override
    public void generateSkeletonStream(Long sessionId, Long userId, java.io.OutputStream out, Long modelId) throws java.io.IOException {
        skeletonService.generateSkeletonStream(sessionId, userId, out, modelId);
    }

}
