package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.service.PublishTimeRecommendationService;
import cn.gaifan.douyinOperations.module.shortvideo.service.DouyinSeoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 抖音 SEO 服务实现 (Phase 8)
 * 基于入参和已落库分析数据生成建议；无真实数据时返回空结果。
 */
@Service
public class DouyinSeoServiceImpl implements DouyinSeoService {

    @Resource
    private PublishTimeRecommendationService publishTimeRecommendationService;

    @Override
    public List<String> suggestTags(String title, String description, String industry) {
        Set<String> tags = new LinkedHashSet<>();
        collectTokens(tags, title);
        collectTokens(tags, description);
        collectTokens(tags, industry);
        return tags.stream().limit(8).toList();
    }

    @Override
    public List<String> suggestPublishTime(Long accountId, List<Long> visibleOwnerIds) {
        if (accountId == null) {
            return List.of();
        }
        return publishTimeRecommendationService.getRecommendedTimes(accountId, visibleOwnerIds)
                .stream()
                .map(this::formatPublishTime)
                .filter(label -> !label.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

    @Override
    public String suggestCover(List<String> frameUrls) {
        return (frameUrls != null && !frameUrls.isEmpty()) ? frameUrls.get(0) : null;
    }

    @Override
    public List<String> suggestAbTestTitles(String baseTitle) {
        if (baseTitle == null || baseTitle.isBlank()) {
            return List.of();
        }
        return List.of(baseTitle.trim());
    }

    private void collectTokens(Set<String> tags, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.split("[\\s#，,。.!！?？、|｜/\\\\:：;；\\[\\]()（）{}<>《》\"'“”‘’]+")) {
            String normalized = token.trim();
            if (normalized.length() >= 2 && normalized.length() <= 16) {
                tags.add(normalized);
            }
        }
    }

    private String formatPublishTime(Map<String, Object> item) {
        Object label = item.get("label");
        if (label instanceof String s && !s.isBlank()) {
            return s;
        }
        int hour = item.get("hourOfDay") instanceof Number n ? n.intValue() : -1;
        if (hour < 0 || hour > 23) {
            return "";
        }
        return String.format("%02d:00", hour);
    }
}
