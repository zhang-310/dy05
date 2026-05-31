package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptRecommendService;
import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendRequestVO;
import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendVO;
import cn.gaifan.douyinOperations.module.script.service.ScriptLibraryService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptSearchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 多场景话术智能推荐服务实现。
 *
 * <h3>推荐策略说明</h3>
 * <p>分两阶段执行：
 * <ol>
 *   <li><b>规则召回</b>：从历史高效话术（{@code live_script}）+ 话术库（{@code script_library}）取候选，
 *       按效果分 + 时段 + 人数加权打分。</li>
 *   <li><b>语义召回（可选）</b>：当 {@code app.live.recommendation.semantic-enabled=true} 且
 *       KnowledgeBaseService 可用时，用当前商品名/人设/场次标题构造 query 调用
 *       {@code KnowledgeBaseService.hybridSearchAllKbs()} 获取语义相关话术 chunk，
 *       与规则候选合并后按融合分（effectivenessScore × 0.5 + semanticScore × 0.5）排序。</li>
 * </ol>
 */
@Slf4j
@Service
public class LiveScriptRecommendServiceImpl implements LiveScriptRecommendService {

    private static final BigDecimal HIGH_SCORE_THRESHOLD = new BigDecimal("60");
    private static final int CANDIDATE_LIMIT = 50;
    private static final double EFFECTIVENESS_WEIGHT = 0.5;
    private static final double SEMANTIC_WEIGHT = 0.5;

    @Value("${app.live.recommendation.semantic-enabled:false}")
    private boolean semanticEnabled;

    @Autowired(required = false)
    private LiveScriptRepository liveScriptRepository;

    @Autowired(required = false)
    private LiveSessionRepository liveSessionRepository;

    @Autowired(required = false)
    private LiveProductRepository liveProductRepository;

    @Autowired(required = false)
    private ScriptLibraryService scriptLibraryService;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public List<ScriptRecommendVO> recommend(ScriptRecommendRequestVO req, Long userId) {
        if (req == null || userId == null) return List.of();

        int topN = req.getTopN() != null && req.getTopN() > 0 ? Math.min(req.getTopN(), 20) : 5;
        List<ScriptRecommendVO> candidates = new ArrayList<>();

        // 1. 规则候选：历史高效话术
        Map<Long, Double> ruleScores = new HashMap<>();
        if (liveScriptRepository != null) {
            try {
                List<LiveScript> topScripts = liveScriptRepository
                        .findTopByUserIdOrderByEffectivenessScoreDesc(userId, PageRequest.of(0, CANDIDATE_LIMIT));
                for (LiveScript s : topScripts) {
                    if (s.getScriptContent() == null || s.getScriptContent().isBlank()) continue;
                    double score = computeRuleScore(s, req);
                    ruleScores.put(s.getId(), score);
                    candidates.add(ScriptRecommendVO.builder()
                            .scriptId(s.getId())
                            .sourceType("live_script")
                            .scriptType(s.getScriptType())
                            .reason(buildReason(s, req))
                            .contentPreview(preview(s.getScriptContent()))
                            .effectivenessScore(s.getEffectivenessScore())
                            .useCount(s.getExecuted() != null && s.getExecuted() == 1 ? 1 : 0)
                            .recommendScore(score)
                            .build());
                }
            } catch (Exception e) {
                log.debug("[ScriptRecommend] 历史话术查询失败: {}", e.getMessage());
            }
        }

        // 2. 规则候选：话术库补充
        if (scriptLibraryService != null && candidates.size() < topN * 2) {
            try {
                ScriptSearchVO searchVO = new ScriptSearchVO();
                searchVO.setUserId(userId);
                searchVO.setPage(0);
                searchVO.setRows(CANDIDATE_LIMIT / 2);
                var libResult = scriptLibraryService.search(searchVO);
                if (libResult != null && libResult.getList() != null) {
                    for (var s : libResult.getList()) {
                        if (s.getContent() == null || s.getContent().isBlank()) continue;
                        candidates.add(ScriptRecommendVO.builder()
                                .scriptId(s.getId())
                                .sourceType("script_library")
                                .scriptType(s.getCategory())
                                .reason("话术库收录的高效话术")
                                .contentPreview(preview(s.getContent()))
                                .effectivenessScore(null)
                                .useCount(s.getUseCount() != null ? s.getUseCount().intValue() : 0)
                                .recommendScore(computeLibraryScore(s.getUseCount() != null ? s.getUseCount().intValue() : 0))
                                .build());
                    }
                }
            } catch (Exception e) {
                log.debug("[ScriptRecommend] 话术库查询失败: {}", e.getMessage());
            }
        }

        // 3. 语义召回（可选）：与规则候选融合
        if (semanticEnabled && knowledgeBaseService != null) {
            candidates = mergeWithSemanticResults(candidates, req, userId, ruleScores, topN);
        }

        // 4. 按推荐分排序，取 topN
        return candidates.stream()
                .sorted(Comparator.comparingDouble(ScriptRecommendVO::getRecommendScore).reversed())
                .limit(topN)
                .toList();
    }

    /**
     * 语义召回并与规则候选融合。
     * 用场次标题 + 当前商品名 + 话术类型构造 query，调用 hybridSearchAllKbs 进行语义检索，
     * 检索结果作为新候选加入列表，对已在规则候选中出现的条目提升分数（加权融合）。
     */
    private List<ScriptRecommendVO> mergeWithSemanticResults(
            List<ScriptRecommendVO> ruleCandidates,
            ScriptRecommendRequestVO req,
            Long userId,
            Map<Long, Double> ruleScores,
            int topN) {
        try {
            String semanticQuery = buildSemanticQuery(req, userId);
            if (semanticQuery == null || semanticQuery.isBlank()) return ruleCandidates;

            // 语义检索 huashu 知识库，过滤 source_type = live_script
            List<cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO> semanticResults =
                    knowledgeBaseService.hybridSearchAllKbs(
                            userId, semanticQuery, topN * 3, "user", List.of("huashu"));

            if (semanticResults == null || semanticResults.isEmpty()) return ruleCandidates;

            // 为规则候选建立 docId 快速查找
            Map<Long, ScriptRecommendVO> ruleById = ruleCandidates.stream()
                    .filter(c -> c.getScriptId() != null)
                    .collect(Collectors.toMap(ScriptRecommendVO::getScriptId, c -> c, (a, _b) -> a));

            List<ScriptRecommendVO> result = new ArrayList<>(ruleCandidates);

            for (var item : semanticResults) {
                double normalizedSemanticScore = Math.min(100.0, item.getScore() * 100.0);

                // 如果规则候选中已有此条目，提升分数
                if (item.getDocId() != null && ruleById.containsKey(item.getDocId())) {
                    ScriptRecommendVO existing = ruleById.get(item.getDocId());
                    double fusedScore = existing.getRecommendScore() * EFFECTIVENESS_WEIGHT
                            + normalizedSemanticScore * SEMANTIC_WEIGHT;
                    existing.setRecommendScore(fusedScore);
                    existing.setReason(existing.getReason() + "，语义匹配度高");
                } else {
                    // 新语义候选
                    if (item.getContent() == null || item.getContent().isBlank()) continue;
                    result.add(ScriptRecommendVO.builder()
                            .scriptId(item.getDocId())
                            .sourceType("knowledge_base")
                            .scriptType(inferScriptType(item.getContent()))
                            .reason("语义相关话术（知识库召回）")
                            .contentPreview(preview(item.getContent()))
                            .effectivenessScore(null)
                            .useCount(0)
                            .recommendScore(normalizedSemanticScore * SEMANTIC_WEIGHT)
                            .build());
                }
            }

            log.debug("[ScriptRecommend] 语义召回 {} 条，与 {} 条规则候选合并",
                    semanticResults.size(), ruleCandidates.size());
            return result;
        } catch (Exception e) {
            log.warn("[ScriptRecommend] 语义召回失败，回退到纯规则模式: {}", e.getMessage());
            return ruleCandidates;
        }
    }

    /**
     * 构造语义检索 query：场次标题 + 商品名 + 推荐时段。
     */
    private String buildSemanticQuery(ScriptRecommendRequestVO req, Long userId) {
        StringBuilder query = new StringBuilder("直播话术");
        if (req.getSessionId() != null && liveSessionRepository != null) {
            try {
                liveSessionRepository.findByIdAndDeleted(req.getSessionId(), 0).ifPresent(s -> {
                    if (s.getLiveTitle() != null && !s.getLiveTitle().isBlank()) {
                        query.append(" ").append(s.getLiveTitle());
                    }
                });
            } catch (Exception ignored) {
                // 查询场次失败，不影响推荐
            }
        }
        if (req.getProductId() != null && liveProductRepository != null) {
            try {
                liveProductRepository.findById(req.getProductId()).ifPresent(p -> {
                    if (p.getProductName() != null) query.append(" ").append(p.getProductName());
                });
            } catch (Exception ignored) {
                // 查询商品失败，不影响推荐
            }
        }
        // 时段提示
        if (req.getTimeElapsed() != null) {
            if (req.getTimeElapsed() < 600) query.append(" 开场暖场");
            else if (req.getTimeElapsed() > 5400) query.append(" 收尾结束");
            else query.append(" 商品介绍推销");
        }
        return query.toString();
    }

    private String inferScriptType(String content) {
        if (content == null) return "product";
        if (content.contains("欢迎") || content.contains("来了") || content.contains("大家好")) return "opening";
        if (content.contains("再见") || content.contains("下播") || content.contains("感谢")) return "closing";
        if (content.contains("互动") || content.contains("扣") || content.contains("留言")) return "interaction";
        return "product";
    }

    // =========== 评分辅助方法 ===========

    private double computeRuleScore(LiveScript script, ScriptRecommendRequestVO req) {
        double score = 0.0;
        if (script.getEffectivenessScore() != null) {
            score += script.getEffectivenessScore().doubleValue() * 0.6;
        }
        if (req.getTimeElapsed() != null) {
            long elapsed = req.getTimeElapsed();
            String scriptType = script.getScriptType();
            if (elapsed < 600 && "opening".equals(scriptType)) score += 20;
            else if (elapsed > 5400 && "closing".equals(scriptType)) score += 20;
            else if (elapsed >= 600 && elapsed <= 5400 && "product".equals(scriptType)) score += 10;
        }
        if (req.getViewerCount() != null) {
            long viewers = req.getViewerCount();
            String scriptType = script.getScriptType();
            if (viewers > 500 && "closing_deal".equals(scriptType)) score += 15;
            else if (viewers < 100 && "interaction".equals(scriptType)) score += 15;
        }
        return score;
    }

    private double computeLibraryScore(Integer useCount) {
        return useCount != null ? Math.min(useCount * 2.0, 30.0) : 0.0;
    }

    private String buildReason(LiveScript script, ScriptRecommendRequestVO req) {
        StringBuilder sb = new StringBuilder();
        if (script.getEffectivenessScore() != null) {
            sb.append(String.format("历史效果%.0f分", script.getEffectivenessScore().doubleValue()));
        }
        if (req.getTimeElapsed() != null && req.getTimeElapsed() < 600 && "opening".equals(script.getScriptType())) {
            sb.append("，适合开播阶段");
        } else if (req.getViewerCount() != null && req.getViewerCount() > 500) {
            sb.append("，高人气时段适用");
        }
        return sb.length() > 0 ? sb.toString() : "历史高效话术";
    }

    private static String preview(String content) {
        if (content == null) return "";
        return content.substring(0, Math.min(100, content.length())) + (content.length() > 100 ? "…" : "");
    }
}
