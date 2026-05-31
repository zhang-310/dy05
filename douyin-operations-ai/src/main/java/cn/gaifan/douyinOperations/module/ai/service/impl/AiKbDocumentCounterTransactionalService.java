package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库文档计数打点：独立短事务，避免与进化/RAG 长事务同事务导致死锁后整段回滚。
 */
@Service
public class AiKbDocumentCounterTransactionalService {

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int incrementCitationCount(List<Long> ids) {
        return documentRepository.incrementCitationCount(ids);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int incrementRetrievalCount(List<Long> ids) {
        return documentRepository.incrementRetrievalCount(ids);
    }
}
