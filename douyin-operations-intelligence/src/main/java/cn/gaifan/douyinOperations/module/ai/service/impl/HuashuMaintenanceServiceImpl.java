package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.HuashuMaintenanceService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HuashuMaintenanceServiceImpl implements HuashuMaintenanceService {

    private static final String SOURCE_TYPE_CROSS_SHARE = "cross_share";
    private static final String SOURCE_TYPE_MANUAL = "manual";

    @Resource
    private AiKbDocumentRepository documentRepository;
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cleanupCrossShare(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", 0);
        result.put("reason", "");

        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, "huashu");
        if (kbId == null) {
            result.put("reason", "用户无 huashu 知识库");
            return result;
        }

        List<AiKbDocument> crossShareDocs = documentRepository.findByKbIdAndSourceTypeAndDeleted(kbId, SOURCE_TYPE_CROSS_SHARE, 0);
        int deleted = 0;
        for (AiKbDocument doc : crossShareDocs) {
            try {
                knowledgeBaseService.deleteDocument(doc.getId(), userId);
                deleted++;
            } catch (Exception e) {
                result.put("lastError", e.getMessage());
                break;
            }
        }
        result.put("deleted", deleted);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> backfillSourceTypeAndReindex(Long userId, int maxDocs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", 0);
        result.put("reindexed", 0);
        result.put("errors", 0);
        result.put("reason", "");

        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, "huashu");
        if (kbId == null) {
            result.put("reason", "用户无 huashu 知识库");
            return result;
        }

        List<AiKbDocument> toBackfill = documentRepository.findByKbIdAndSourceTypeNullOr(kbId, SOURCE_TYPE_CROSS_SHARE, PageRequest.of(0, Math.min(maxDocs, 200)));
        int updated = 0;
        int reindexed = 0;
        int errors = 0;
        for (AiKbDocument doc : toBackfill) {
            try {
                doc.setSourceType(SOURCE_TYPE_MANUAL);
                documentRepository.save(doc);
                updated++;
                int ok = knowledgeBaseService.retrySyncDocument(doc);
                if (ok > 0) reindexed++;
            } catch (Exception e) {
                errors++;
            }
        }
        result.put("updated", updated);
        result.put("reindexed", reindexed);
        result.put("errors", errors);
        if (updated > 0) {
            knowledgeBaseService.invalidateSearchCache(kbId);
        }
        return result;
    }
}
