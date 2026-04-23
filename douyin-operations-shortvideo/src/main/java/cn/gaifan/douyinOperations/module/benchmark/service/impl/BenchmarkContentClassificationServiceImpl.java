package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkContentClassificationService;
import cn.gaifan.douyinOperations.module.benchmark.vo.ContentClassificationVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 内容分类验证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkContentClassificationServiceImpl implements BenchmarkContentClassificationService {

    private final BenchmarkQualityScriptRepository qualityScriptRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    // 行业分类字典
    private static final List<String> INDUSTRY_CATEGORIES = Arrays.asList(
            "护肤", "彩妆", "美妆工具", "香水", "个护",
            "人生感悟", "情感励志", "职场成长", "生活哲理",
            "搞笑段子", "剧情演绎", "才艺展示",
            "知识科普", "技能教学", "产品测评",
            "美食", "旅游", "时尚", "健身", "母婴"
    );

    // 场景类型字典
    private static final List<String> SCENE_TYPES = Arrays.asList(
            "直播", "短视频", "图文", "长视频", "直播切片"
    );

    // 脚本类型字典
    private static final List<String> SCRIPT_TYPES = Arrays.asList(
            "产品介绍", "情感共鸣", "知识科普", "剧情演绎",
            "痛点切入", "场景代入", "对比展示", "用户见证",
            "限时优惠", "互动问答", "才艺展示", "生活分享"
    );

    // 行业关键词映射
    private static final Map<String, List<String>> INDUSTRY_KEYWORDS = new HashMap<String, List<String>>() {{
        put("护肤", Arrays.asList("护肤", "面霜", "精华", "水乳", "防晒", "抗衰", "保湿", "美白", "修复", "敏感肌"));
        put("彩妆", Arrays.asList("彩妆", "口红", "粉底", "眼影", "腮红", "眉笔", "睫毛膏", "妆容", "化妆"));
        put("人生感悟", Arrays.asList("人生", "感悟", "成长", "经历", "领悟", "道理", "哲理", "感慨", "体会"));
        put("搞笑段子", Arrays.asList("搞笑", "段子", "幽默", "笑话", "逗", "好笑", "有趣", "梗", "包袱"));
        put("知识科普", Arrays.asList("科普", "知识", "原理", "解释", "为什么", "怎么", "科学", "研究", "数据"));
    }};

    @Override
    public ContentClassificationVO validateClassification(
            String scriptContent,
            String targetIndustry,
            String targetSceneType,
            Long ownerId
    ) {
        log.info("验证内容分类，目标行业：{}，目标场景：{}", targetIndustry, targetSceneType);

        // 1. AI自动分类
        ContentClassificationVO autoResult = autoClassify(scriptContent, ownerId);

        // 2. 计算与目标分类的相关度
        autoResult.setTargetIndustry(targetIndustry);
        autoResult.setTargetSceneType(targetSceneType);

        // 3. 关键词匹配度检测
        BigDecimal keywordScore = calculateKeywordMatchScore(scriptContent, targetIndustry);

        // 4. 综合评分
        BigDecimal industryMatch = autoResult.getDetectedIndustry().equals(targetIndustry) ?
                BigDecimal.valueOf(100) : keywordScore;
        BigDecimal sceneMatch = autoResult.getDetectedSceneType().equals(targetSceneType) ?
                BigDecimal.valueOf(100) : BigDecimal.valueOf(50);

        autoResult.setIndustryRelevanceScore(industryMatch);
        autoResult.setSceneRelevanceScore(sceneMatch);
        autoResult.setOverallRelevanceScore(
                industryMatch.multiply(BigDecimal.valueOf(0.7))
                        .add(sceneMatch.multiply(BigDecimal.valueOf(0.3)))
        );

        // 5. 判断是否匹配
        boolean isMatch = autoResult.getOverallRelevanceScore().compareTo(BigDecimal.valueOf(70)) >= 0;
        autoResult.setIsMatch(isMatch);

        // 6. 判断是否需要人工审核
        boolean needsReview = !isMatch || autoResult.getConfidence().compareTo(BigDecimal.valueOf(0.7)) < 0;
        autoResult.setNeedsManualReview(needsReview);

        if (!isMatch) {
            autoResult.setMismatchReason(String.format(
                    "AI识别为【%s】类，与目标【%s】类不匹配，相关度仅%.1f%%",
                    autoResult.getDetectedIndustry(),
                    targetIndustry,
                    autoResult.getOverallRelevanceScore()
            ));
        }

        return autoResult;
    }

    @Override
    public ContentClassificationVO autoClassify(String scriptContent, Long ownerId) {
        log.info("AI自动分类脚本内容");

        String prompt = buildClassificationPrompt(scriptContent);

        try {
            // 使用默认模型进行分类
            cn.gaifan.douyinOperations.module.ai.entity.AiModel model =
                new cn.gaifan.douyinOperations.module.ai.entity.AiModel();
            model.setModelName("gpt-4");
            model.setModelProvider("openai");
            model.setModelVersion("gpt-4");

            cn.gaifan.douyinOperations.module.ai.service.LlmClient.LlmResponse response =
                llmClient.chat(model, null, prompt);

            if (response.success()) {
                return parseClassificationResult(scriptContent, response.content());
            } else {
                log.warn("AI分类失败：{}", response.errorMsg());
                return buildFallbackClassification(scriptContent);
            }
        } catch (Exception e) {
            log.error("AI分类失败", e);
            return buildFallbackClassification(scriptContent);
        }
    }

    @Override
    @Transactional
    public List<ContentClassificationVO> batchValidate(List<Long> scriptIds, Long ownerId) {
        log.info("批量验证分类，数量：{}", scriptIds.size());

        List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAllById(scriptIds);

        return scripts.stream()
                .filter(script -> script.getOwnerId().equals(ownerId))
                .map(script -> validateClassification(
                        script.getScriptContent(),
                        script.getIndustry(),
                        script.getSceneType(),
                        ownerId
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getIndustryCategories() {
        return new ArrayList<>(INDUSTRY_CATEGORIES);
    }

    @Override
    public List<String> getSceneTypes() {
        return new ArrayList<>(SCENE_TYPES);
    }

    @Override
    public List<String> getScriptTypes() {
        return new ArrayList<>(SCRIPT_TYPES);
    }

    @Override
    @Transactional
    public void markForManualReview(Long scriptId, String reason, Long ownerId) {
        log.info("标记脚本需要人工审核，scriptId：{}，原因：{}", scriptId, reason);

        BenchmarkQualityScript script = qualityScriptRepository.findById(scriptId)
                .orElseThrow(() -> new IllegalArgumentException("脚本不存在"));

        if (!script.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("无权限操作");
        }

        // 在keyFeatures中添加审核标记
        try {
            JsonNode features = script.getKeyFeatures() != null ?
                    objectMapper.readTree(script.getKeyFeatures()) : objectMapper.createObjectNode();
            ((com.fasterxml.jackson.databind.node.ObjectNode) features).put("needsManualReview", true);
            ((com.fasterxml.jackson.databind.node.ObjectNode) features).put("reviewReason", reason);
            script.setKeyFeatures(objectMapper.writeValueAsString(features));
            qualityScriptRepository.save(script);
        } catch (Exception e) {
            log.error("标记审核失败", e);
        }
    }

    @Override
    @Transactional
    public void confirmClassification(
            Long scriptId,
            String confirmedIndustry,
            String confirmedSceneType,
            String confirmedScriptType,
            Long ownerId
    ) {
        log.info("人工确认分类，scriptId：{}，行业：{}，场景：{}，类型：{}",
                scriptId, confirmedIndustry, confirmedSceneType, confirmedScriptType);

        BenchmarkQualityScript script = qualityScriptRepository.findById(scriptId)
                .orElseThrow(() -> new IllegalArgumentException("脚本不存在"));

        if (!script.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("无权限操作");
        }

        script.setIndustry(confirmedIndustry);
        script.setSceneType(confirmedSceneType);
        script.setScriptType(confirmedScriptType);

        // 移除审核标记
        try {
            JsonNode features = script.getKeyFeatures() != null ?
                    objectMapper.readTree(script.getKeyFeatures()) : objectMapper.createObjectNode();
            ((com.fasterxml.jackson.databind.node.ObjectNode) features).put("needsManualReview", false);
            ((com.fasterxml.jackson.databind.node.ObjectNode) features).put("manuallyConfirmed", true);
            script.setKeyFeatures(objectMapper.writeValueAsString(features));
        } catch (Exception e) {
            log.error("更新审核标记失败", e);
        }

        qualityScriptRepository.save(script);
    }

    // ==================== 私有方法 ====================

    private String buildClassificationPrompt(String scriptContent) {
        return String.format("""
                请分析以下短视频脚本内容，并进行精准分类。

                脚本内容：
                %s

                请从以下维度进行分类：

                1. 行业分类（从以下选项中选择最匹配的）：
                %s

                2. 场景类型（从以下选项中选择）：
                %s

                3. 脚本类型（从以下选项中选择）：
                %s

                请以JSON格式返回结果：
                {
                  "industry": "行业分类",
                  "sceneType": "场景类型",
                  "scriptType": "脚本类型",
                  "confidence": 0.95,
                  "reasoning": "分类理由",
                  "matchedKeywords": ["关键词1", "关键词2"],
                  "suggestedIndustry": "如果不确定，建议的行业",
                  "suggestedSceneType": "如果不确定，建议的场景",
                  "suggestedScriptType": "如果不确定，建议的脚本类型"
                }
                """,
                scriptContent,
                String.join("、", INDUSTRY_CATEGORIES),
                String.join("、", SCENE_TYPES),
                String.join("、", SCRIPT_TYPES)
        );
    }

    private ContentClassificationVO parseClassificationResult(String scriptContent, String aiResponse) {
        ContentClassificationVO result = new ContentClassificationVO();
        result.setScriptContent(scriptContent);

        try {
            // 提取JSON部分
            String jsonStr = aiResponse;
            if (aiResponse.contains("```json")) {
                jsonStr = aiResponse.substring(
                        aiResponse.indexOf("```json") + 7,
                        aiResponse.lastIndexOf("```")
                ).trim();
            } else if (aiResponse.contains("{")) {
                jsonStr = aiResponse.substring(
                        aiResponse.indexOf("{"),
                        aiResponse.lastIndexOf("}") + 1
                );
            }

            JsonNode json = objectMapper.readTree(jsonStr);

            result.setDetectedIndustry(json.get("industry").asText());
            result.setDetectedSceneType(json.get("sceneType").asText());
            result.setDetectedScriptType(json.get("scriptType").asText());
            result.setConfidence(BigDecimal.valueOf(json.get("confidence").asDouble()));
            result.setAiAnalysisDetail(json.get("reasoning").asText());

            if (json.has("matchedKeywords")) {
                List<String> keywords = new ArrayList<>();
                json.get("matchedKeywords").forEach(node -> keywords.add(node.asText()));
                result.setMatchedKeywords(keywords);
            }

            result.setSuggestedIndustry(json.has("suggestedIndustry") ?
                    json.get("suggestedIndustry").asText() : result.getDetectedIndustry());
            result.setSuggestedSceneType(json.has("suggestedSceneType") ?
                    json.get("suggestedSceneType").asText() : result.getDetectedSceneType());
            result.setSuggestedScriptType(json.has("suggestedScriptType") ?
                    json.get("suggestedScriptType").asText() : result.getDetectedScriptType());

        } catch (Exception e) {
            log.error("解析AI分类结果失败", e);
            return buildFallbackClassification(scriptContent);
        }

        return result;
    }

    private ContentClassificationVO buildFallbackClassification(String scriptContent) {
        ContentClassificationVO result = new ContentClassificationVO();
        result.setScriptContent(scriptContent);

        // 基于关键词的简单分类
        String detectedIndustry = "未分类";
        BigDecimal maxScore = BigDecimal.ZERO;

        for (Map.Entry<String, List<String>> entry : INDUSTRY_KEYWORDS.entrySet()) {
            BigDecimal score = calculateKeywordMatchScore(scriptContent, entry.getKey());
            if (score.compareTo(maxScore) > 0) {
                maxScore = score;
                detectedIndustry = entry.getKey();
            }
        }

        result.setDetectedIndustry(detectedIndustry);
        result.setDetectedSceneType("短视频");
        result.setDetectedScriptType("产品介绍");
        result.setConfidence(maxScore.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        result.setSuggestedIndustry(detectedIndustry);
        result.setSuggestedSceneType("短视频");
        result.setSuggestedScriptType("产品介绍");

        return result;
    }

    private BigDecimal calculateKeywordMatchScore(String content, String industry) {
        List<String> keywords = INDUSTRY_KEYWORDS.getOrDefault(industry, Collections.emptyList());
        if (keywords.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long matchCount = keywords.stream()
                .filter(content::contains)
                .count();

        return BigDecimal.valueOf(matchCount)
                .divide(BigDecimal.valueOf(keywords.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
