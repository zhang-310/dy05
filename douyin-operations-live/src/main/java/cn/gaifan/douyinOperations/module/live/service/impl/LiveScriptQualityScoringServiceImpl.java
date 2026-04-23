package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptQualityScore;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptQualityScoreRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityScoringService;
import cn.gaifan.douyinOperations.module.script.entity.ComplianceWord;
import cn.gaifan.douyinOperations.module.script.repository.ComplianceWordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 话术质量评分服务实现
 * 三维评分：合规（违禁词检测）+ 流畅（句式/长度/节奏）+ 吸引力（互动词/情感词/行动号召）
 */
@Service
public class LiveScriptQualityScoringServiceImpl implements LiveScriptQualityScoringService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptQualityScoringServiceImpl.class);

    @Resource
    private LiveScriptRepository liveScriptRepository;

    @Resource
    private LiveScriptQualityScoreRepository qualityScoreRepository;

    @Autowired(required = false)
    private ComplianceWordRepository complianceWordRepository;

    @Resource
    private ObjectMapper objectMapper;

    // 权重配置（含关键词/信息密度，对齐 upgrade-plan Phase2 Q-1）
    private static final BigDecimal W_COMPLIANCE = new BigDecimal("0.35");
    private static final BigDecimal W_FLUENCY = new BigDecimal("0.25");
    private static final BigDecimal W_ENGAGEMENT = new BigDecimal("0.25");
    private static final BigDecimal W_KEYWORD = new BigDecimal("0.15");

    private static final Set<String> PRODUCT_KEY_TERMS = Set.of(
            "成分", "功效", "补水", "保湿", "抗衰", "紧致", "修护", "敏肌", "油皮", "干皮",
            "限量", "福利", "秒杀", "试用", "正装", "小样"
    );

    // 互动词库
    private static final Set<String> ENGAGEMENT_WORDS = Set.of(
            "宝宝们", "家人们", "点赞", "关注", "评论", "扣1", "扣个",
            "赶紧", "马上", "立刻", "限时", "秒杀", "福利", "抢",
            "你们觉得", "有没有", "想不想", "要不要", "对不对"
    );

    // 行动号召词
    private static final Set<String> CTA_WORDS = Set.of(
            "下单", "购买", "加购", "拍", "抢购", "链接", "购物车", "小黄车"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scoreScript(Long scriptId, Long ownerId) {
        LiveScript script = liveScriptRepository.findById(scriptId).orElse(null);
        if (script == null) {
            return Map.of("error", "话术不存在", "scriptId", scriptId);
        }

        String content = script.getScriptContent();
        if (content == null || content.isBlank()) {
            return Map.of("scriptId", scriptId, "totalQualityScore", 0);
        }

        // 三维评分
        ComplianceResult compliance = scoreCompliance(content);
        FluencyResult fluency = scoreFluency(content);
        EngagementResult engagement = scoreEngagement(content);
        KeywordResult keyword = scoreKeywordDensity(content);

        BigDecimal total = compliance.score.multiply(W_COMPLIANCE)
                .add(fluency.score.multiply(W_FLUENCY))
                .add(engagement.score.multiply(W_ENGAGEMENT))
                .add(keyword.score.multiply(W_KEYWORD))
                .setScale(2, RoundingMode.HALF_UP);

        // 构建详情 JSON
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("compliance", Map.of("score", compliance.score, "violations", compliance.violations));
        details.put("fluency", Map.of("score", fluency.score, "avgSentenceLen", fluency.avgSentenceLen, "sentenceCount", fluency.sentenceCount));
        details.put("engagement", Map.of("score", engagement.score, "engagementWordCount", engagement.engagementWordCount, "ctaCount", engagement.ctaCount));
        details.put("keywordDensity", Map.of("score", keyword.score, "distinctChars", keyword.distinctChars, "productTermHits", keyword.productTermHits));

        // 持久化
        LiveScriptQualityScore qs = new LiveScriptQualityScore();
        qs.setScriptId(scriptId);
        qs.setSessionId(script.getSessionId());
        qs.setComplianceScore(compliance.score);
        qs.setFluencyScore(fluency.score);
        qs.setEngagementScore(engagement.score);
        qs.setTotalQualityScore(total);
        qs.setOwnerId(ownerId);
        try {
            qs.setDetailsJson(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.warn("序列化评分详情失败: {}", e.getMessage());
        }
        qualityScoreRepository.save(qs);

        return Map.of(
                "scriptId", scriptId,
                "complianceScore", compliance.score,
                "fluencyScore", fluency.score,
                "engagementScore", engagement.score,
                "totalQualityScore", total,
                "details", details
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scoreSession(Long sessionId, Long ownerId) {
        List<LiveScript> scripts = liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        for (LiveScript script : scripts) {
            try {
                scoreScript(script.getId(), ownerId);
            } catch (Exception e) {
                log.error("话术质量评分失败: scriptId={}", script.getId(), e);
            }
        }
    }

    // ───────────── 合规评分 ─────────────

    private ComplianceResult scoreCompliance(String content) {
        List<String> violations = new ArrayList<>();

        if (complianceWordRepository != null) {
            List<ComplianceWord> absoluteWords = complianceWordRepository.findByWordTypeAndIsEnabled("absolute", 1);
            List<ComplianceWord> medicalWords = complianceWordRepository.findByWordTypeAndIsEnabled("medical", 1);

            for (ComplianceWord w : absoluteWords) {
                if (content.contains(w.getWordValue())) {
                    violations.add("绝对化用语: " + w.getWordValue());
                }
            }
            for (ComplianceWord w : medicalWords) {
                if (content.contains(w.getWordValue())) {
                    violations.add("医疗功效: " + w.getWordValue());
                }
            }
        }

        // 每个违规扣 10 分，最低 0 分
        int deduction = violations.size() * 10;
        BigDecimal score = BigDecimal.valueOf(Math.max(0, 100 - deduction));
        return new ComplianceResult(score, violations);
    }

    // ───────────── 流畅度评分 ─────────────

    private FluencyResult scoreFluency(String content) {
        // 按句号/感叹号/问号分句
        String[] sentences = content.split("[。！？!?]+");
        int sentenceCount = 0;
        int totalLen = 0;

        for (String s : sentences) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                sentenceCount++;
                totalLen += trimmed.length();
            }
        }

        if (sentenceCount == 0) {
            return new FluencyResult(BigDecimal.valueOf(50), 0, 0);
        }

        double avgLen = (double) totalLen / sentenceCount;
        BigDecimal score = BigDecimal.valueOf(100);

        // 句子过长扣分（>80字）
        if (avgLen > 80) score = score.subtract(BigDecimal.valueOf(20));
        else if (avgLen > 60) score = score.subtract(BigDecimal.valueOf(10));

        // 句子过短扣分（<5字，可能是碎片）
        if (avgLen < 5) score = score.subtract(BigDecimal.valueOf(15));

        // 句子数量过少扣分
        if (sentenceCount < 3) score = score.subtract(BigDecimal.valueOf(10));

        // 重复句式检测（简单：相邻句首3字相同）
        int repeatCount = 0;
        for (int i = 1; i < sentences.length; i++) {
            String prev = sentences[i - 1].trim();
            String curr = sentences[i].trim();
            if (prev.length() >= 3 && curr.length() >= 3 && prev.substring(0, 3).equals(curr.substring(0, 3))) {
                repeatCount++;
            }
        }
        if (repeatCount > 2) score = score.subtract(BigDecimal.valueOf(10));

        score = score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
        return new FluencyResult(score, avgLen, sentenceCount);
    }

    // ───────────── 吸引力评分 ─────────────

    private EngagementResult scoreEngagement(String content) {
        int engagementCount = 0;
        for (String word : ENGAGEMENT_WORDS) {
            if (content.contains(word)) engagementCount++;
        }

        int ctaCount = 0;
        for (String word : CTA_WORDS) {
            if (content.contains(word)) ctaCount++;
        }

        BigDecimal score = BigDecimal.valueOf(40); // 基础分

        // 互动词加分（每个 +8，上限 40）
        score = score.add(BigDecimal.valueOf(Math.min(engagementCount * 8, 40)));

        // CTA 加分（每个 +10，上限 20）
        score = score.add(BigDecimal.valueOf(Math.min(ctaCount * 10, 20)));

        score = score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
        return new EngagementResult(score, engagementCount, ctaCount);
    }

    /**
     * 关键词与信息密度：去重字符占比 + 品类相关词命中（口播场景近似「卖点关键词密度」）
     */
    private KeywordResult scoreKeywordDensity(String content) {
        String text = content != null ? content : "";
        String compact = text.replaceAll("\\s+", "");
        if (compact.isEmpty()) {
            return new KeywordResult(BigDecimal.valueOf(50), 0, 0);
        }
        java.util.Set<Character> distinct = new java.util.HashSet<>();
        for (char c : compact.toCharArray()) {
            distinct.add(c);
        }
        double diversity = distinct.size() / (double) Math.max(12, compact.length());
        BigDecimal score = BigDecimal.valueOf(Math.min(95, 35 + diversity * 160));
        int productHits = 0;
        for (String term : PRODUCT_KEY_TERMS) {
            if (text.contains(term)) {
                productHits++;
            }
        }
        score = score.add(BigDecimal.valueOf(Math.min(30, productHits * 6)));
        score = score.min(BigDecimal.valueOf(100)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new KeywordResult(score, distinct.size(), productHits);
    }

    private record ComplianceResult(BigDecimal score, List<String> violations) {}
    private record FluencyResult(BigDecimal score, double avgSentenceLen, int sentenceCount) {}
    private record EngagementResult(BigDecimal score, int engagementWordCount, int ctaCount) {}
    private record KeywordResult(BigDecimal score, int distinctChars, int productTermHits) {}
}
