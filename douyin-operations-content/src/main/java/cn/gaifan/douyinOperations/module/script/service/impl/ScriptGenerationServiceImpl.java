package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.entity.ScriptGeneration;
import cn.gaifan.douyinOperations.module.script.entity.ScriptVariant;
import cn.gaifan.douyinOperations.module.script.repository.ScriptGenerationRepository;
import cn.gaifan.douyinOperations.module.script.repository.ScriptVariantRepository;
import cn.gaifan.douyinOperations.module.script.service.ScriptGenerationService;
import cn.gaifan.douyinOperations.module.script.service.AiService;
import cn.gaifan.douyinOperations.module.script.service.ScriptCacheService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptVariantVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScriptGenerationServiceImpl implements ScriptGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(ScriptGenerationServiceImpl.class);
    private static final long CACHE_TTL_SECONDS = 3600; // 1 小时缓存

    @Autowired
    private ScriptGenerationRepository generationRepository;

    @Autowired
    private ScriptVariantRepository variantRepository;

    @Autowired
    private AiService aiService;

    @Autowired
    private ScriptCacheService cacheService;

    @Override
    @Transactional
    public ScriptGenerationVO generateScript(Long userId, ScriptGenerationRequestVO request) {
        long startTime = System.currentTimeMillis();

        // 1. 保存生成记录
        ScriptGeneration generation = new ScriptGeneration();
        generation.setOwnerId(userId);
        generation.setProductName(request.getProductName());
        generation.setProductPrice(request.getProductPrice());
        generation.setKeyFeatures(String.join(",", request.getKeyFeatures()));
        generation.setDuration(request.getDuration());
        generation.setStyle(request.getStyle());
        generation.setVariantCount(request.getVariants());
        generation = generationRepository.save(generation);

        // 2. 生成多个版本
        List<ScriptVariantVO> variants = new ArrayList<>();
        for (int i = 0; i < request.getVariants(); i++) {
            String prompt = buildPrompt(request);
            String content = generateScriptContent(prompt, i);
            BigDecimal score = scoreScript(content);
            String keyPoints = extractKeyPoints(content);

            // 保存到数据库
            ScriptVariant variant = new ScriptVariant();
            variant.setGenerationId(generation.getId());
            variant.setVariantIndex(i + 1);
            variant.setContent(content);
            variant.setScore(score);
            variant.setKeyPoints(keyPoints);
            variantRepository.save(variant);

            variants.add(new ScriptVariantVO(
                UUID.randomUUID().toString(),
                content,
                score,
                keyPoints
            ));
        }

        // 3. 按评分排序
        variants.sort((a, b) -> b.getScore().compareTo(a.getScore()));

        long generationTime = System.currentTimeMillis() - startTime;
        logger.info("Script generated: product={}, duration={}s, variants={}, time={}ms",
            request.getProductName(), request.getDuration(), request.getVariants(), generationTime);

        return new ScriptGenerationVO(generation.getId(), variants, generationTime);
    }

    private String buildPrompt(ScriptGenerationRequestVO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("生成一个").append(request.getDuration()).append("秒的直播话术\n");
        prompt.append("产品: ").append(request.getProductName()).append("\n");
        prompt.append("价格: ¥").append(request.getProductPrice()).append("\n");
        prompt.append("卖点: ").append(String.join(", ", request.getKeyFeatures())).append("\n");
        prompt.append("风格: ").append(request.getStyle()).append("\n");
        prompt.append("要求:\n");
        prompt.append("1. 开场 (5-10秒): 吸引关注\n");
        prompt.append("2. 介绍 (10-15秒): 强调卖点\n");
        prompt.append("3. 促销 (3-5秒): 制造紧迫感\n");
        prompt.append("4. 收尾 (2-3秒): 行动号召\n");
        return prompt.toString();
    }

    private String generateScriptContent(String prompt, int variant) {
        // 尝试从缓存获取
        String cacheKey = "script:" + prompt.hashCode() + ":" + variant;
        Optional<String> cached = cacheService.getScript(cacheKey);
        if (cached.isPresent()) {
            logger.debug("从缓存返回话术");
            return cached.get();
        }

        // 调用 AI 服务生成
        String content = aiService.generateScript(prompt);

        // 缓存结果
        cacheService.cacheScript(cacheKey, content, CACHE_TTL_SECONDS);

        return content;
    }

    private BigDecimal scoreScript(String content) {
        // 使用 AI 服务评分
        Double aiScore = aiService.scoreScript(content);
        return BigDecimal.valueOf(aiScore);
    }

    private String extractKeyPoints(String content) {
        // 提取关键点
        return "开场吸引 | 产品介绍 | 促销引导 | 行动号召";
    }
}
