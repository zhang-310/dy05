package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.KbDocumentQualityService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class KbDocumentQualityServiceImpl implements KbDocumentQualityService {

    private static final Logger log = LoggerFactory.getLogger(KbDocumentQualityServiceImpl.class);

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Value("${app.ai.kb-quality.heuristic.low-threshold:40}")
    private int lowThreshold;

    @Value("${app.ai.kb-quality.heuristic.healthy-threshold:65}")
    private int healthyThreshold;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTier(Long docId, int tier) {
        if (docId == null || docId <= 0) {
            return;
        }
        int t = Math.max(0, Math.min(2, tier));
        documentRepository.findById(docId).ifPresent(doc -> {
            doc.setQualityTier(t);
            doc.setLastQualityEvalAt(new Timestamp(System.currentTimeMillis()));
            documentRepository.save(doc);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanHeuristicPage(int pageIndex, int pageSize) {
        int ps = Math.max(1, Math.min(pageSize, 500));
        int pi = Math.max(0, pageIndex);
        Page<AiKbDocument> page = documentRepository.findByDeletedOrderByIdAsc(0, PageRequest.of(pi, ps));
        if (page.isEmpty()) {
            return -1;
        }
        return evaluateDocuments(page.getContent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanUnevaluated(int limit) {
        int ps = Math.max(1, Math.min(limit, 2000));
        List<AiKbDocument> docs = documentRepository.findUnevaluatedQualityDocuments(PageRequest.of(0, ps));
        if (docs.isEmpty()) {
            return -1;
        }
        return evaluateDocuments(docs);
    }

    private int evaluateDocuments(List<AiKbDocument> docs) {
        int updated = 0;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long nowMs = Instant.now().toEpochMilli();
        for (AiKbDocument d : docs) {
            int score = computeHeuristicScore(d, nowMs);
            int tier;
            if (score < lowThreshold) {
                tier = TIER_LOW;
            } else if (score >= healthyThreshold) {
                tier = TIER_HEALTHY;
            } else {
                tier = TIER_NEUTRAL;
            }
            Integer oldTier = d.getQualityTier() != null ? d.getQualityTier() : 0;
            Integer oldScore = d.getQualityHeuristicScore();
            if (tier != oldTier || oldScore == null || oldScore != score) {
                d.setQualityHeuristicScore(score);
                d.setQualityTier(tier);
                d.setLastQualityEvalAt(now);
                documentRepository.save(d);
                updated++;
            }
        }
        return updated;
    }

    private static int computeHeuristicScore(AiKbDocument d, long nowMs) {
        int s = 55;
        if (d.getStatus() != null && d.getStatus() == 1) {
            s += 8;
        } else {
            s -= 18;
        }
        int tokenCount = d.getTokenCount() != null ? d.getTokenCount() : 0;
        if (tokenCount >= 300 && tokenCount <= 4000) {
            s += 8;
        } else if (tokenCount > 0 && tokenCount < 80) {
            s -= 10;
        } else if (tokenCount > 8000) {
            s -= 6;
        }
        int chunkCount = d.getChunkCount() != null ? d.getChunkCount() : 0;
        if (chunkCount > 0 && chunkCount <= 120) {
            s += 4;
        } else if (chunkCount > 180) {
            s -= 6;
        }
        long ret = d.getRetrievalCount() != null ? d.getRetrievalCount() : 0;
        long cit = d.getCitationCount() != null ? d.getCitationCount() : 0;
        if (ret >= 30) {
            s += 12;
        } else if (ret >= 10) {
            s += 8;
        } else if (ret >= 3) {
            s += 4;
        }
        if (cit >= 15) {
            s += 15;
        } else if (cit >= 5) {
            s += 10;
        } else if (cit >= 1) {
            s += 4;
        } else if (ret >= 15) {
            s -= 12;
        }
        if (d.getBoostFactor() != null) {
            double b = d.getBoostFactor().doubleValue();
            if (b < 0.88) {
                s -= 14;
            } else if (b < 0.98) {
                s -= 6;
            } else if (b > 1.08) {
                s += 6;
            }
        }
        if (d.getExpiryStatus() != null && d.getExpiryStatus() == 2) {
            s -= 18;
        }
        if (d.getLastRetrievalAt() != null) {
            long days = (nowMs - d.getLastRetrievalAt().getTime()) / (86400000L);
            if (days > 400 && ret > 5) {
                s -= 8;
            }
        }
        return Math.max(0, Math.min(100, s));
    }
}
