package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.ColdDocDetectionService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ColdDocDetectionServiceImpl implements ColdDocDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ColdDocDetectionServiceImpl.class);

    @Resource
    private AiKbDocumentRepository documentRepository;
    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Value("${app.ai.cold-doc.enabled:true}")
    private boolean enabled;

    @Override
    public Map<String, Object> detectColdDocs(int coldDays, int maxResults) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("coldCount", 0);
        result.put("coldDocIds", List.<Long>of());

        if (!enabled) {
            result.put("reason", "冷门文档检测已禁用");
            return result;
        }

        Timestamp cutoff = Timestamp.from(
                LocalDateTime.now().minusDays(coldDays).atZone(ZoneId.systemDefault()).toInstant());

        List<AiKbDocument> cold = documentRepository.findColdDocs(cutoff, PageRequest.of(0, maxResults));
        result.put("coldCount", cold.size());
        result.put("coldDocIds", cold.stream().map(AiKbDocument::getId).collect(Collectors.toList()));
        result.put("coldDays", coldDays);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> archiveColdDocs(List<Long> docIds) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("archived", 0);
        result.put("failed", 0);
        result.put("errors", new ArrayList<String>());

        if (docIds == null || docIds.isEmpty()) {
            return result;
        }

        List<AiKbDocument> docs = documentRepository.findByIdIn(docIds);
        if (docs.isEmpty()) {
            result.put("reason", "未找到任何文档");
            return result;
        }

        int archived = 0;
        int failed = 0;
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");

        for (AiKbDocument doc : docs) {
            if (doc.getDeleted() != null && doc.getDeleted() == 1) {
                failed++;
                errors.add("docId=" + doc.getId() + ": 已删除");
                continue;
            }
            Optional<AiKnowledgeBase> kbOpt = knowledgeBaseRepository.findById(doc.getKbId());
            if (kbOpt.isEmpty()) {
                failed++;
                errors.add("docId=" + doc.getId() + ": 知识库不存在");
                continue;
            }
            Long userId = kbOpt.get().getUserId();
            try {
                knowledgeBaseService.deleteDocument(doc.getId(), userId);
                archived++;
                log.info("冷门文档归档成功: docId={}, kbId={}", doc.getId(), doc.getKbId());
            } catch (Exception e) {
                failed++;
                errors.add("docId=" + doc.getId() + ": " + e.getMessage());
                log.warn("冷门文档归档失败 docId={}: {}", doc.getId(), e.getMessage());
            }
        }

        result.put("archived", archived);
        result.put("failed", failed);
        return result;
    }
}
