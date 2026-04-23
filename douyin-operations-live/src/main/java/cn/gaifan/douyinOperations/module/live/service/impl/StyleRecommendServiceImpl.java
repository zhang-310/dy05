package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.StyleRecommendService;
import cn.gaifan.douyinOperations.module.live.vo.StyleRecommendationVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 live_script 的 effectiveness_score 按 style 聚合，推荐效果最优的风格
 */
@Service
public class StyleRecommendServiceImpl implements StyleRecommendService {

    private static final int DEFAULT_LIMIT = 10;

    @Resource
    private LiveSessionRepository liveSessionRepository;
    @Resource
    private LiveScriptRepository liveScriptRepository;

    @Override
    public List<StyleRecommendationVO> recommendStyles(Long ownerId, Long productId, int limit) {
        if (ownerId == null) {
            return List.of();
        }
        List<Long> sessionIds = liveSessionRepository.findIdsByUserIdIn(List.of(ownerId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        List<LiveScript> scripts = liveScriptRepository.findBySessionIdInAndDeleted(sessionIds, 0, Pageable.unpaged()).getContent();
        if (scripts == null || scripts.isEmpty()) {
            return List.of();
        }
        // 仅统计有评分且风格非空的记录
        Map<String, List<LiveScript>> byStyle = scripts.stream()
                .filter(s -> s.getEffectivenessScore() != null && s.getStyle() != null && !s.getStyle().isBlank())
                .filter(s -> productId == null || (s.getProductId() != null && s.getProductId().equals(productId)))
                .collect(Collectors.groupingBy(LiveScript::getStyle));

        List<StyleRecommendationVO> result = new ArrayList<>();
        for (Map.Entry<String, List<LiveScript>> e : byStyle.entrySet()) {
            List<LiveScript> list = e.getValue();
            BigDecimal sum = list.stream()
                    .map(LiveScript::getEffectivenessScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int n = list.size();
            Set<Long> sessions = list.stream().map(LiveScript::getSessionId).filter(Objects::nonNull).collect(Collectors.toSet());
            result.add(new StyleRecommendationVO(
                    e.getKey(),
                    n > 0 ? sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    (long) n,
                    sessions.size()));
        }
        int top = limit > 0 ? limit : DEFAULT_LIMIT;
        return result.stream()
                .sorted(Comparator.comparing(StyleRecommendationVO::getAvgEffectiveness).reversed())
                .limit(top)
                .toList();
    }
}
