package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiSearchLog;
import cn.gaifan.douyinOperations.module.ai.repository.AiSearchLogRepository;
import cn.gaifan.douyinOperations.module.ai.service.KbDocumentQualityService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBoostService;
import cn.gaifan.douyinOperations.module.ai.service.SearchLogService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检索日志服务实现：异步记录检索命中，支撑质量评分与进化规则
 */
@Service
public class SearchLogServiceImpl implements SearchLogService {

    private static final Logger log = LoggerFactory.getLogger(SearchLogServiceImpl.class);

    @Resource
    private AiSearchLogRepository searchLogRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private KnowledgeBoostService knowledgeBoostService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private KbDocumentQualityService kbDocumentQualityService;

    @Override
    @Async
    public void logSearchAsync(Long ownerId, String queryText, Long kbId, List<Long> hitDocIds,
                               Double top1Score, String searchType, int latencyMs) {
        try {
            if (ownerId == null || queryText == null) return;
            AiSearchLog entry = new AiSearchLog();
            entry.setOwnerId(ownerId);
            entry.setQueryText(queryText.length() > 2000 ? queryText.substring(0, 2000) : queryText);
            entry.setKbId(kbId);
            if (hitDocIds != null && !hitDocIds.isEmpty()) {
                entry.setHitDocIds(hitDocIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
                entry.setHitCount(hitDocIds.size());
            }
            entry.setTop1Score(top1Score);
            entry.setSearchType(searchType != null ? searchType : "hybrid");
            entry.setLatencyMs(latencyMs);
            searchLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("记录检索日志失败: {}", e.getMessage());
        }
    }

    @Override
    public void updateFeedback(Long logId, String feedback, Long ownerId) {
        if (logId == null || feedback == null || feedback.isBlank()) return;
        if (!"helpful".equals(feedback) && !"not_helpful".equals(feedback)) return;
        try {
            searchLogRepository.findById(logId).ifPresent(entry -> {
                if (ownerId != null && entry.getOwnerId() != null && !entry.getOwnerId().equals(ownerId)) {
                    log.warn("检索反馈 owner 不匹配 logId={}", logId);
                    return;
                }
                entry.setUserFeedback(feedback);
                searchLogRepository.save(entry);
                if (entry.getHitDocIds() != null && !entry.getHitDocIds().isBlank()) {
                    String[] parts = entry.getHitDocIds().split(",");
                    if (knowledgeBoostService != null) {
                        int rating = "helpful".equals(feedback) ? 1 : -1;
                        for (String p : parts) {
                            try {
                                long docId = Long.parseLong(p.trim());
                                if (docId > 0) {
                                    knowledgeBoostService.updateBoostForFeedback(docId, rating);
                                    break;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    if (kbDocumentQualityService != null) {
                        if ("helpful".equals(feedback)) {
                            for (String p : parts) {
                                try {
                                    long docId = Long.parseLong(p.trim());
                                    if (docId > 0) {
                                        kbDocumentQualityService.markTier(docId, KbDocumentQualityService.TIER_HEALTHY);
                                        break;
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        } else {
                            for (String p : parts) {
                                try {
                                    long docId = Long.parseLong(p.trim());
                                    if (docId > 0) {
                                        kbDocumentQualityService.markTier(docId, KbDocumentQualityService.TIER_LOW);
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.warn("更新检索反馈失败: logId={}, {}", logId, e.getMessage());
        }
    }

    @Override
    public List<String> suggestPopularQueries(Long ownerId, String prefix, int limit) {
        if (ownerId == null || limit <= 0) {
            return List.of();
        }
        try {
            String p = prefix != null ? prefix.trim() : "";
            return searchLogRepository.findTopQueriesByOwner(ownerId, p, Math.min(30, limit));
        } catch (Exception e) {
            log.debug("搜索建议失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public long getUsageCountForDoc(Long ownerId, Long docId, int days) {
        if (ownerId == null || docId == null) return 0;
        try {
            Timestamp since = Timestamp.from(LocalDateTime.now().minusDays(days).atZone(ZoneId.systemDefault()).toInstant());
            String pattern = "%," + docId + ",%";
            return searchLogRepository.countHitsForDocSince(ownerId, pattern, since);
        } catch (Exception e) {
            log.debug("统计文档检索次数失败: docId={}, {}", docId, e.getMessage());
            return 0;
        }
    }
}
