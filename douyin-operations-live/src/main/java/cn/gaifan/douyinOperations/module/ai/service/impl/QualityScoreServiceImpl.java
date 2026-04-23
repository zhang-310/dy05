package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import cn.gaifan.douyinOperations.module.ai.service.QualityScoreService;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.ScriptQualityEvaluator;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 统一评分体系：overall = library*0.3 + script*0.4 + rag*0.3
 * Grade: S(≥90), A(≥75), B(≥60), C(≥40), D(&lt;40)
 */
@Service
public class QualityScoreServiceImpl implements QualityScoreService {

    private static final Logger log = LoggerFactory.getLogger(QualityScoreServiceImpl.class);

    @Autowired(required = false)
    private KnowledgeQualityScoringService kbScoringService;

    @Autowired(required = false)
    private ScriptQualityEvaluator scriptQualityEvaluator;

    @Autowired
    private LiveScriptRepository liveScriptRepository;

    @Resource
    private AiKbDocumentRepository aiKbDocumentRepository;

    @Override
    public BigDecimal getLibraryScore(Long userId) {
        if (kbScoringService == null) return BigDecimal.ZERO;
        try {
            return kbScoringService.calculateLibraryQualityScore(userId);
        } catch (Exception e) {
            log.debug("知识库质量评分失败: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Map<String, Object> evaluateScript(String content, String ipType, Long userId) {
        if (scriptQualityEvaluator == null) {
            return Map.of("error", "评估服务不可用");
        }
        return scriptQualityEvaluator.evaluate(content, ipType, userId);
    }

    @Override
    public Map<String, Object> getOverallQuality(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // libraryScore: 0-100
        BigDecimal libraryScore = getLibraryScore(userId);
        double libVal = libraryScore != null ? libraryScore.doubleValue() : 0;
        result.put("libraryScore", libraryScore != null ? libraryScore : BigDecimal.ZERO);

        // scriptScore: LiveScript.effectivenessScore 均值，0-100 分制（若<15 则按 0-10 换算）
        List<LiveScript> scored = liveScriptRepository.findByUserIdAndEffectivenessScoreGte(userId, BigDecimal.ZERO);
        double scriptVal = scored.stream()
                .filter(s -> s.getEffectivenessScore() != null)
                .mapToDouble(s -> s.getEffectivenessScore().doubleValue())
                .average().orElse(0);
        if (scriptVal > 0 && scriptVal < 15) scriptVal *= 10; // 0-10 换算为 0-100
        BigDecimal scriptScore = BigDecimal.valueOf(scriptVal).setScale(2, RoundingMode.HALF_UP);
        result.put("effectivenessScore", scriptScore);
        result.put("scriptScore", scriptScore);
        result.put("scoredScriptCount", scored.size());

        // ragEffectiveness: 引用率 0-100
        double ragVal = 0;
        try {
            Object[] arr = aiKbDocumentRepository.sumCitationAndRetrievalByUserId(userId);
            if (arr != null && arr.length >= 2) {
                long citation = ((Number) arr[0]).longValue();
                long retrieval = ((Number) arr[1]).longValue();
                ragVal = retrieval > 0 ? Math.min(100, citation * 100.0 / retrieval) : 0;
            }
        } catch (Exception e) {
            log.debug("RAG 引用率统计失败: {}", e.getMessage());
        }
        result.put("ragEffectiveness", BigDecimal.valueOf(ragVal).setScale(2, RoundingMode.HALF_UP));

        // overall = library*0.3 + script*0.4 + rag*0.3
        double overall = libVal * 0.3 + scriptVal * 0.4 + ragVal * 0.3;
        result.put("overallScore", BigDecimal.valueOf(overall).setScale(2, RoundingMode.HALF_UP));

        // Grade: S(≥90), A(≥75), B(≥60), C(≥40), D(<40)
        String grade;
        if (overall >= 90) grade = "S";
        else if (overall >= 75) grade = "A";
        else if (overall >= 60) grade = "B";
        else if (overall >= 40) grade = "C";
        else grade = "D";
        result.put("grade", grade);

        return result;
    }
}
