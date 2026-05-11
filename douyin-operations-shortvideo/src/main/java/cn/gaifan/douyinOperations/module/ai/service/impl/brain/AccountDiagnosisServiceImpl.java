package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.common.config.ShortVideoBusinessConfig;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeQualityScoreRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.brain.AccountDiagnosisService;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptEffectivenessRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账号诊断服务实现（Phase2）
 * 依赖 douyin 账号、shortvideo 等数据
 */
@Service
public class AccountDiagnosisServiceImpl implements AccountDiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(AccountDiagnosisServiceImpl.class);

    @Value("${app.ai.brain.account-diagnosis.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private DouyinAccountRepository douyinAccountRepository;

    @Autowired(required = false)
    private DyPersonaRepository personaRepository;

    @Autowired(required = false)
    private SvVideoRepository videoRepository;

    @Autowired(required = false)
    private KnowledgeQualityScoreRepository qualityScoreRepository;

    @Autowired(required = false)
    private LiveScriptEffectivenessRepository effectivenessRepository;

    @Autowired(required = false)
    private ShortVideoBusinessConfig shortVideoBusinessConfig;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Override
    public DiagnosisResult diagnose(Long accountId, Long userId) {
        if (!enabled || accountId == null) {
            return new DiagnosisResult(accountId, 0, 0, 0, "未知", List.of("诊断未启用"), "");
        }
        return douyinAccountRepository.findById(accountId)
                .map(acc -> {
                    Long uid = userId != null ? userId : acc.getUserId();
                    var clarityResult = computeClarityWithEstimated(uid, accountId, acc);
                    double competitiveness = computeCompetitiveness(accountId, acc);
                    double contentQuality = computeContentQuality(uid);
                    double growth = computeGrowth(acc, uid);
                    boolean anyEstimated = clarityResult.estimated();
                    double blendedCompetitiveness = competitiveness * 0.6 + contentQuality * 0.4;
                    if (videoRepository == null) {
                        anyEstimated = true;
                    } else {
                        var views = videoRepository.findViewCountsByAccountId(accountId);
                        if (views == null || views.isEmpty()) anyEstimated = true;
                    }
                    String risk = blendedCompetitiveness < 0.4 ? "中" : (clarityResult.clarity() < 0.5 ? "中" : "低");
                    List<String> priorities = buildPriorities(clarityResult.clarity(), blendedCompetitiveness, growth);
                    String summary = String.format("账号 %s 诊断完成，建议优先 %s", acc.getAccountName(), String.join("、", priorities));
                    if (anyEstimated) summary += "（部分指标为估算值，数据完善后更准确）";
                    return new DiagnosisResult(accountId, clarityResult.clarity(), blendedCompetitiveness, growth, risk,
                            priorities, summary, anyEstimated);
                })
                .orElse(new DiagnosisResult(accountId, 0, 0, 0, "未知", List.of("账号不存在"), "", false));
    }

    @Override
    public List<DiagnosisResult> diagnoseBatch(List<Long> accountIds, Long userId) {
        if (!enabled || accountIds == null) return List.of();
        List<DiagnosisResult> results = new ArrayList<>();
        for (Long id : accountIds) {
            results.add(diagnose(id, userId));
        }
        return results;
    }

    @Override
    public Map<String, Object> diagnoseContent(Long userId, String category) {
        String system = "你是抖音直播运营诊断专家。请基于以下信息，分析该用户的话术效果并与行业平均水平做对比。" +
                "输出 JSON 格式包含：overallScore(0-100)、industryAvg、strengths(数组)、weaknesses(数组)、recommendations(数组)。";
        String prompt = String.format("用户 ID: %d，行业分类: %s。请给出内容诊断分析。", userId, category);
        return callLlmAndWrap("content-diagnosis", system, prompt);
    }

    @Override
    public Map<String, Object> diagnoseProductStrategy(Long userId) {
        String system = "你是抖音直播选品策略专家。请分析该用户的商品讲解策略，包括选品搭配、讲解时长分配、价格梯度是否合理。" +
                "输出 JSON 格式包含：strategyScore(0-100)、productMix(对象)、suggestions(数组)、benchmarkComparison(对象)。";
        String prompt = String.format("用户 ID: %d。请给出选品策略诊断分析。", userId);
        return callLlmAndWrap("product-diagnosis", system, prompt);
    }

    @Override
    public Map<String, Object> diagnoseRhythm(Long userId) {
        String system = "你是抖音直播节奏优化专家。请分析该用户的直播时段安排、各时段观众流失率，给出节奏优化建议。" +
                "输出 JSON 格式包含：rhythmScore(0-100)、peakPeriods(数组)、dropOffPeriods(数组)、optimizedSchedule(数组)、tips(数组)。";
        String prompt = String.format("用户 ID: %d。请给出直播节奏诊断分析。", userId);
        return callLlmAndWrap("rhythm-diagnosis", system, prompt);
    }

    private Map<String, Object> callLlmAndWrap(String diagnosisType, String system, String prompt) {
        Map<String, Object> result = new HashMap<>();
        result.put("diagnosisType", diagnosisType);
        if (llmClient == null || aiModelRepository == null) {
            result.put("status", "unavailable");
            result.put("message", "LLM 服务未配置");
            return result;
        }
        try {
            List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0)
                    .stream().limit(3).toList();
            if (models.isEmpty()) {
                result.put("status", "no_model");
                result.put("message", "无可用 AI 模型");
                return result;
            }
            LlmClient.LlmResponse response = llmClient.chatWithFallback(models, system, prompt);
            if (response.success()) {
                result.put("status", "success");
                result.put("analysis", response.content());
                result.put("tokensUsed", response.tokensUsed());
            } else {
                result.put("status", "failed");
                result.put("message", response.errorMsg());
            }
        } catch (Exception e) {
            log.warn("诊断 LLM 调用失败: type={}, error={}", diagnosisType, e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    private record ClarityResult(double clarity, boolean estimated) {}

    private ClarityResult computeClarityWithEstimated(Long userId, Long accountId, DouyinAccount acc) {
        String personaDesc = "";
        Set<String> personaKeywords = new HashSet<>();
        if (userId != null && personaRepository != null) {
            for (DyPersona p : personaRepository.findByOwnerIdAndDeleted(userId, 0)) {
                if (p.getDescription() != null) personaDesc += " " + p.getDescription();
                if (p.getKeywords() != null) personaKeywords.addAll(extractKeywords(p.getKeywords()));
            }
        }
        if (acc.getDescription() != null) personaDesc += " " + acc.getDescription();
        personaKeywords.addAll(extractKeywords(personaDesc));
        Set<String> contentKeywords = new HashSet<>();
        boolean hasVideoData = false;
        if (videoRepository != null && userId != null) {
            List<SvVideo> videos = videoRepository.findByOwnerIdAndDeleted(userId, 0);
            for (SvVideo v : videos.stream().limit(30).toList()) {
                hasVideoData = true;
                String t = (v.getTitle() != null ? v.getTitle() : "") + " " + (v.getDescription() != null ? v.getDescription() : "");
                contentKeywords.addAll(extractKeywords(t));
            }
        }
        if (personaKeywords.isEmpty()) {
            double fallback = (personaDesc != null && personaDesc.length() >= 50) ? 0.7 : 0.5;
            return new ClarityResult(fallback, !hasVideoData);
        }
        long overlap = personaKeywords.stream().filter(contentKeywords::contains).count();
        double ratio = (double) overlap / personaKeywords.size();
        double clarity;
        if (ratio >= 0.7) clarity = Math.min(0.95, 0.8 + ratio * 0.1);
        else if (ratio >= 0.3) clarity = 0.5 + ratio * 0.3;
        else clarity = Math.max(0.3, ratio);
        return new ClarityResult(clarity, !hasVideoData);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(text.replaceAll("[，。、；：！？\\s]+", " ").split(" "))
                .filter(s -> s.length() >= 2)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private double computeCompetitiveness(Long accountId, DouyinAccount acc) {
        // 使用行业基准数据（护肤品类）
        long avgView = 5000;
        double avgLikeRate = 0.05;
        double avgCompletion = 0.35;

        if (videoRepository != null) {
            List<Long> views = videoRepository.findViewCountsByAccountId(accountId);
            if (!views.isEmpty()) {
                long myAvgView = (long) views.stream().mapToLong(Long::longValue).average().orElse(0);
                double myLikeRate = acc.getVideoCount() != null && acc.getVideoCount() > 0 && acc.getTotalLikes() != null
                        ? (double) acc.getTotalLikes() / (acc.getVideoCount() * Math.max(1, myAvgView)) : 0;
                double viewRatio = myAvgView >= avgView ? 1.0 : (double) myAvgView / avgView;
                double likeRatio = myLikeRate >= avgLikeRate ? 1.0 : myLikeRate / avgLikeRate;
                return Math.min(0.95, 0.3 + viewRatio * 0.35 + likeRatio * 0.35);
            }
        }
        long fans = acc.getFanCount() != null ? acc.getFanCount() : 0;
        long likes = acc.getTotalLikes() != null ? acc.getTotalLikes() : 0;
        long videos = acc.getVideoCount() != null ? acc.getVideoCount() : 0;
        if (videos > 0 && likes > 0) {
            double avgLike = (double) likes / videos;
            if (avgLike >= 50000) return 0.85;
            if (avgLike >= 10000) return 0.75;
            if (avgLike >= 5000) return 0.65;
            if (avgLike >= 1000) return 0.55;
        }
        return fans >= 100000 ? 0.65 : fans >= 10000 ? 0.55 : 0.5;
    }

    private double computeContentQuality(Long userId) {
        if (userId == null) return 0.5;
        double docQuality = 0.5;
        double scriptEffect = 0.5;
        if (qualityScoreRepository != null) {
            BigDecimal avg = qualityScoreRepository.calculateAverageQualityScore(userId);
            if (avg != null) docQuality = avg.doubleValue() / 100.0;
        }
        if (effectivenessRepository != null) {
            Double avgConv = effectivenessRepository.findAvgConversionByUserId(userId);
            if (avgConv != null) scriptEffect = Math.min(1.0, avgConv / 10.0);
        }
        return docQuality * 0.4 + scriptEffect * 0.6;
    }

    private double computeGrowth(DouyinAccount acc, Long userId) {
        long fans = acc.getFanCount() != null ? acc.getFanCount() : 0;
        long likes = acc.getTotalLikes() != null ? acc.getTotalLikes() : 0;
        long videos = acc.getVideoCount() != null ? acc.getVideoCount() : 0;
        double base = 0.5;
        if (videos > 0) {
            double avgLike = (double) likes / videos;
            if (fans >= 100000 && avgLike >= 5000) base = 0.75;
            else if (fans >= 10000 && avgLike >= 2000) base = 0.65;
            else if (fans >= 1000) base = 0.55;
        }
        if (videoRepository != null && userId != null) {
            long count = videoRepository.countByOwnerIdAndDeleted(userId, 0);
            if (count >= 20) base = Math.min(0.9, base + 0.1);
            if (count >= 50) base = Math.min(0.9, base + 0.05);
        }
        return Math.min(0.9, base);
    }

    private List<String> buildPriorities(double clarity, double competitiveness, double growth) {
        List<String> p = new ArrayList<>();
        if (clarity < 0.5) p.add("完善账号简介与定位");
        if (clarity >= 0.6 && competitiveness < 0.5) {
            p.add("人设强化");
            p.add("内容密度提升");
        }
        if (competitiveness >= 0.6) {
            p.add("商业化");
            p.add("矩阵扩展");
        }
        if (p.size() < 2 && growth < 0.6) p.add("提升内容质量与更新频率");
        if (p.isEmpty()) p.add("持续优化内容与数据复盘");
        return p.stream().limit(4).toList();
    }
}
