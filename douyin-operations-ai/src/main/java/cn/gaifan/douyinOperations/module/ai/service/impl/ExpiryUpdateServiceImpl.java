package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.ExpiryUpdateService;
import cn.gaifan.douyinOperations.module.ai.service.IndexQueueAmqpPublisher;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 时效性闭环：将 expiry_status=2 的文档入队，由 IndexQueueConsumer 处理后删除旧文档并以 LLM 更新内容重新入库
 */
@Service
public class ExpiryUpdateServiceImpl implements ExpiryUpdateService {

    private static final Logger log = LoggerFactory.getLogger(ExpiryUpdateServiceImpl.class);
    private static final String SOURCE_TYPE = "expiry_refresh";

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private AiIndexQueueRepository indexQueueRepository;

    @Autowired(required = false)
    private IndexQueueAmqpPublisher indexQueueAmqpPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int enqueueExpiredForUpdate(int maxDocs) {
        var expired = documentRepository.findExpiredForUpdate(PageRequest.of(0, maxDocs));
        if (expired.isEmpty()) return 0;

        for (AiKbDocument doc : expired) {
            AiIndexQueue q = new AiIndexQueue();
            q.setSourceType(SOURCE_TYPE);
            q.setSourceId(doc.getId());
            q.setTargetKbId(doc.getKbId());
            q.setContent(doc.getContent() != null ? doc.getContent() : "");
            q.setPriority(3); // 高于普通，低于 viral
            indexQueueRepository.save(q);
        }
        if (indexQueueAmqpPublisher != null) {
            indexQueueAmqpPublisher.notifyIndexTask();
        }
        log.info("时效性闭环：{} 篇过期文档已入队更新", expired.size());
        return expired.size();
    }
}
