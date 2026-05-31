package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue;
import cn.gaifan.douyinOperations.module.ai.entity.AiLiveReview;
import cn.gaifan.douyinOperations.module.ai.entity.AiViralAnalysis;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiLiveReviewRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiViralAnalysisRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionDashboardService;
import jakarta.annotation.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EvolutionDashboardServiceImpl implements EvolutionDashboardService {

    @Resource private AiViralAnalysisRepository viralRepo;
    @Resource private AiLiveReviewRepository liveReviewRepo;
    @Resource private AiIndexQueueRepository indexQueueRepo;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority) {
        return enqueueIndex(sourceType, sourceId, content, priority, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority, Long targetKbId) {
        AiIndexQueue queue = new AiIndexQueue();
        queue.setSourceType(sourceType);
        queue.setSourceId(sourceId);
        queue.setContent(content);
        queue.setPriority(priority != null ? priority : 5);
        queue.setTargetKbId(targetKbId);
        return indexQueueRepo.save(queue).getId();
    }

    @Override
    public Map<String, Object> getEvolutionStats() {
        long totalViral = viralRepo.count();
        long doneViral = viralRepo.findAll(
                (Specification<AiViralAnalysis>) (root, q, cb) ->
                        cb.and(cb.equal(root.get("status"), 1), cb.equal(root.get("deleted"), 0))
        ).size();
        long totalLive = liveReviewRepo.count();
        long doneLive = liveReviewRepo.findAll(
                (Specification<AiLiveReview>) (root, q, cb) ->
                        cb.and(cb.equal(root.get("status"), 1), cb.equal(root.get("deleted"), 0))
        ).size();
        long pendingQueue = indexQueueRepo.findAll().stream()
                .filter(q -> "pending".equals(q.getStatus())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("viralAnalysisTotal", totalViral);
        stats.put("viralAnalysisDone", doneViral);
        stats.put("liveReviewTotal", totalLive);
        stats.put("liveReviewDone", doneLive);
        stats.put("indexQueuePending", pendingQueue);
        return stats;
    }
}
