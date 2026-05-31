package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.ContentEffectivenessService;
import jakarta.annotation.Resource;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * 知识内容效果统计服务实现（阶段一）
 */
@Service
public class ContentEffectivenessServiceImpl implements ContentEffectivenessService {

    private static final Logger log = LoggerFactory.getLogger(ContentEffectivenessServiceImpl.class);
    private static final int BATCH_SIZE = 500;
    private static final int DEADLOCK_MAX_ATTEMPTS = 5;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private AiKbDocumentCounterTransactionalService counterTransactionalService;

    @Override
    public void recordRetrieval(Collection<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) return;
        List<Long> sorted = sortedDistinctIds(docIds);
        if (sorted.isEmpty()) return;
        for (int i = 0; i < sorted.size(); i += BATCH_SIZE) {
            List<Long> batch = sorted.subList(i, Math.min(i + BATCH_SIZE, sorted.size()));
            int updated = runWithDeadlockRetry(() -> counterTransactionalService.incrementRetrievalCount(batch));
            if (log.isTraceEnabled() && updated > 0) {
                log.trace("效果统计-检索: 更新 {} 个文档", updated);
            }
        }
    }

    @Override
    public void recordCitation(Collection<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) return;
        List<Long> sorted = sortedDistinctIds(docIds);
        if (sorted.isEmpty()) return;
        for (int i = 0; i < sorted.size(); i += BATCH_SIZE) {
            List<Long> batch = sorted.subList(i, Math.min(i + BATCH_SIZE, sorted.size()));
            int updated = runWithDeadlockRetry(() -> counterTransactionalService.incrementCitationCount(batch));
            if (log.isTraceEnabled() && updated > 0) {
                log.trace("效果统计-引用: 更新 {} 个文档", updated);
            }
        }
    }

    /** 升序去重，使并发 UPDATE 尽量按相同主键顺序加锁，降低死锁概率。 */
    static List<Long> sortedDistinctIds(Collection<Long> docIds) {
        TreeSet<Long> set = new TreeSet<>();
        for (Long id : docIds) {
            if (id != null) {
                set.add(id);
            }
        }
        return new ArrayList<>(set);
    }

    static boolean isPostgresDeadlock(Throwable t) {
        while (t != null) {
            if (t instanceof PSQLException psql && "40P01".equals(psql.getSQLState())) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private int runWithDeadlockRetry(IntSupplier op) {
        int attempt = 0;
        while (true) {
            try {
                return op.getAsInt();
            } catch (DataAccessException e) {
                attempt++;
                if (!isPostgresDeadlock(e) || attempt >= DEADLOCK_MAX_ATTEMPTS) {
                    throw e;
                }
                long sleepMs = 40L * attempt * attempt;
                log.warn("ai_kb_document 计数更新遇死锁，第 {}/{} 次重试，{}ms 后重试", attempt, DEADLOCK_MAX_ATTEMPTS, sleepMs);
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    @Override
    public Map<String, Object> getEffectivenessBySourceType(String sourceType) {
        List<AiKbDocument> docs = documentRepository.findBySourceTypeAndDeleted(sourceType, 0);
        long totalDocs = docs.size();
        long totalRetrieval = docs.stream()
                .mapToLong(d -> d.getRetrievalCount() != null ? d.getRetrievalCount() : 0)
                .sum();
        long totalCitation = docs.stream()
                .mapToLong(d -> d.getCitationCount() != null ? d.getCitationCount() : 0)
                .sum();
        return Map.of(
                "sourceType", sourceType,
                "totalDocs", totalDocs,
                "totalRetrievalCount", totalRetrieval,
                "totalCitationCount", totalCitation
        );
    }

    @Override
    public List<Map<String, Object>> getTopCitedDocs(String sourceType, int topN) {
        List<AiKbDocument> docs = documentRepository.findTopCitedBySourceType(sourceType, PageRequest.of(0, topN));
        return docs.stream()
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("docId", d.getId());
                    m.put("title", d.getTitle());
                    m.put("citationCount", d.getCitationCount());
                    String content = d.getContent();
                    m.put("summary", content != null && content.length() > 300
                            ? content.substring(0, 300) + "..."
                            : content);
                    return m;
                })
                .collect(Collectors.toList());
    }
}
