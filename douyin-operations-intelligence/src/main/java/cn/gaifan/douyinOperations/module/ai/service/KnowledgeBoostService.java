package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 知识权重反向更新服务（BR-31）
 * high_perform: boost_factor += 0.15；low_perform: boost_factor -= 0.05
 */
@Service
public class KnowledgeBoostService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBoostService.class);

    private static final BigDecimal DELTA_HIGH = new BigDecimal("0.15");
    private static final BigDecimal DELTA_LOW = new BigDecimal("-0.05");
    private static final BigDecimal DELTA_FEEDBACK_USEFUL = new BigDecimal("0.10");
    private static final BigDecimal DELTA_FEEDBACK_USELESS = new BigDecimal("-0.05");
    private static final BigDecimal MIN_BOOST = new BigDecimal("0.50");
    private static final BigDecimal MAX_BOOST = new BigDecimal("2.00");

    @Resource
    private AiKbDocumentRepository documentRepository;

    /**
     * 根据归因结果更新引用知识的 boost_factor
     * chunkId 格式：docId * 10000 + chunkIndex
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateBoostForAttribution(AiCallLog logEntry) {
        String contentEffect = logEntry.getContentEffect();
        String referencedChunkIds = logEntry.getReferencedChunkIds();
        if (contentEffect == null || referencedChunkIds == null || referencedChunkIds.isBlank()) {
            return 0;
        }
        if (!"high_perform".equals(contentEffect) && !"low_perform".equals(contentEffect)) {
            return 0;
        }

        List<Number> chunkIds;
        try {
            chunkIds = JSON.parseArray(referencedChunkIds, Number.class);
        } catch (Exception e) {
            log.warn("解析 referenced_chunk_ids 失败: {}", referencedChunkIds);
            return 0;
        }
        if (chunkIds == null || chunkIds.isEmpty()) return 0;

        Set<Long> docIds = new HashSet<>();
        for (Number n : chunkIds) {
            long chunkId = n.longValue();
            long docId = chunkId / 10000;
            if (docId > 0) docIds.add(docId);
        }
        if (docIds.isEmpty()) return 0;

        BigDecimal delta = "high_perform".equals(contentEffect) ? DELTA_HIGH : DELTA_LOW;
        List<AiKbDocument> docs = documentRepository.findByIdIn(List.copyOf(docIds));
        int updated = 0;
        for (AiKbDocument doc : docs) {
            BigDecimal current = doc.getBoostFactor() != null ? doc.getBoostFactor() : BigDecimal.ONE;
            BigDecimal next = current.add(delta).setScale(2, RoundingMode.HALF_UP);
            if (next.compareTo(MIN_BOOST) < 0) next = MIN_BOOST;
            if (next.compareTo(MAX_BOOST) > 0) next = MAX_BOOST;
            if (next.compareTo(current) != 0) {
                doc.setBoostFactor(next);
                documentRepository.save(doc);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("BR-31 知识权重更新: contentEffect={}, docIds={}, updated={}", contentEffect, docIds, updated);
        }
        return updated;
    }

    /**
     * P2 用户反馈：有用 +0.10，无用 -0.05，更新文档 boost_factor
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateBoostForFeedback(Long docId, int rating) {
        if (docId == null) return 0;
        if (rating == 0) return 0;
        BigDecimal delta = rating > 0 ? DELTA_FEEDBACK_USEFUL : DELTA_FEEDBACK_USELESS;

        AiKbDocument doc = documentRepository.findById(docId).orElse(null);
        if (doc == null) return 0;

        BigDecimal current = doc.getBoostFactor() != null ? doc.getBoostFactor() : BigDecimal.ONE;
        BigDecimal next = current.add(delta).setScale(2, RoundingMode.HALF_UP);
        if (next.compareTo(MIN_BOOST) < 0) next = MIN_BOOST;
        if (next.compareTo(MAX_BOOST) > 0) next = MAX_BOOST;
        if (next.compareTo(current) != 0) {
            doc.setBoostFactor(next);
            documentRepository.save(doc);
            log.info("P2 用户反馈更新 boost: docId={}, rating={}, boost {} -> {}", docId, rating, current, next);
            return 1;
        }
        return 0;
    }
}
