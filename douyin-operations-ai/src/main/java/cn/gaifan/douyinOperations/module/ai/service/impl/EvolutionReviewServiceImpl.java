package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolutionReviewTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolutionReviewTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.event.EvolveTaskCompletedEvent;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionFitnessService;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionReviewService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTaskDagSupport;
import cn.gaifan.douyinOperations.module.ai.service.PromptSelfOptimizationService;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 进化审核服务实现
 * <p>
 * 审核工作流：
 * - APPROVED → 入库（boost=1.0）
 * - REVISED → 修改后入库
 * - REJECTED → 记录丢弃原因
 * - 48h 超时自动 REJECTED
 * <p>
 * 审核反馈循环：
 * - 统计最近 100 审核任务通过率
 * - 通过率 < 20% → 触发 Prompt 自优化
 * - 通过率 > 80% → 可扩大灰色地带到 [35, 50)
 * - REVISED 内容 → 分析审核者修改模式
 */
@Slf4j
@Service
public class EvolutionReviewServiceImpl implements EvolutionReviewService {

    @Autowired
    private AiEvolutionReviewTaskRepository reviewTaskRepository;

    @Autowired(required = false)
    private PromptSelfOptimizationService promptSelfOptimizationService;

    @Autowired(required = false)
    private AiEvolveTaskRepository evolveTaskRepository;

    @Autowired(required = false)
    private AiEvolveTopicRepository evolveTopicRepository;

    @Autowired(required = false)
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired(required = false)
    private EvolutionFitnessService evolutionFitnessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReviewTask(Long evolveTaskId, String contentPreview, int qualityScore) {
        AiEvolutionReviewTask task = new AiEvolutionReviewTask();
        task.setEvolveTaskId(evolveTaskId);
        task.setContentPreview(contentPreview != null && contentPreview.length() > 500
                ? contentPreview.substring(0, 500) : contentPreview);
        task.setQualityScore(qualityScore);
        task.setReviewStatus("PENDING");
        reviewTaskRepository.save(task);
        log.info("[EvolutionReview] 创建审核任务: evolveTaskId={}, score={}", evolveTaskId, qualityScore);
    }

    @Override
    public PageResultVO<Map<String, Object>> listReviewTasks(String status, int page, int rows) {
        Specification<AiEvolutionReviewTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("reviewStatus"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AiEvolutionReviewTask> p = reviewTaskRepository.findAll(spec,
                PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime")));

        List<Map<String, Object>> list = p.getContent().stream().map(this::toMap).collect(Collectors.toList());
        return PageResultVO.of(p.getTotalElements(), list, page, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long reviewTaskId, Long reviewerId, String comment) {
        AiEvolutionReviewTask task = reviewTaskRepository.findById(reviewTaskId).orElseThrow();
        task.setReviewStatus("APPROVED");
        task.setReviewerId(reviewerId);
        task.setReviewComment(comment);
        task.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        reviewTaskRepository.save(task);
        log.info("[EvolutionReview] 审核通过: taskId={}", reviewTaskId);

        finishEvolveTaskAfterReview(task.getEvolveTaskId(), true, comment, "approve");
        // 反哺主题权重：通过 → 提升主题 scoreAvg 和优先级
        boostTopicWeight(task.getEvolveTaskId(), true, task.getQualityScore());
        checkAndTriggerFeedback();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long reviewTaskId, Long reviewerId, String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "拒绝审核时必须填写理由");
        }
        AiEvolutionReviewTask task = reviewTaskRepository.findById(reviewTaskId).orElseThrow();
        task.setReviewStatus("REJECTED");
        task.setReviewerId(reviewerId);
        task.setReviewComment(comment);
        task.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        reviewTaskRepository.save(task);
        log.info("[EvolutionReview] 审核拒绝: taskId={}", reviewTaskId);

        finishEvolveTaskAfterReview(task.getEvolveTaskId(), false,
                comment != null ? comment : "审核拒绝", "reject");
        // 反哺主题权重：拒绝 → 降低主题优先级
        boostTopicWeight(task.getEvolveTaskId(), false, task.getQualityScore());
        checkAndTriggerFeedback();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revise(Long reviewTaskId, Long reviewerId, String revisedContent, String comment) {
        AiEvolutionReviewTask task = reviewTaskRepository.findById(reviewTaskId).orElseThrow();
        task.setReviewStatus("REVISED");
        task.setReviewerId(reviewerId);
        task.setRevisedContent(revisedContent);
        task.setReviewComment(comment);
        task.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        reviewTaskRepository.save(task);
        log.info("[EvolutionReview] 审核修订: taskId={}", reviewTaskId);

        finishEvolveTaskAfterReview(task.getEvolveTaskId(), true, comment, "revise");
        boostTopicWeight(task.getEvolveTaskId(), true, task.getQualityScore());
        checkAndTriggerFeedback();
    }

    @Override
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pendingCount", reviewTaskRepository.countByStatus("PENDING"));
        stats.put("approvedCount", reviewTaskRepository.countByStatus("APPROVED"));
        stats.put("rejectedCount", reviewTaskRepository.countByStatus("REJECTED"));
        stats.put("revisedCount", reviewTaskRepository.countByStatus("REVISED"));

        // 最近 7 天通过率
        Timestamp sevenDaysAgo = new Timestamp(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        long approvedRecent = reviewTaskRepository.countApprovedSince(sevenDaysAgo);
        long totalRecent = reviewTaskRepository.countTotalSince(sevenDaysAgo);
        stats.put("approvalRate7d", totalRecent > 0 ? Math.round(approvedRecent * 1000.0 / totalRecent) / 10.0 : 0);

        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireTimeoutTasks() {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - 48L * 60 * 60 * 1000);
        List<AiEvolutionReviewTask> expired = reviewTaskRepository
                .findByReviewStatusAndAutoExpiredAndCreateTimeBefore("PENDING", false, cutoff);

        for (AiEvolutionReviewTask task : expired) {
            task.setReviewStatus("REJECTED");
            task.setAutoExpired(true);
            task.setReviewComment("48小时超时自动拒绝");
            task.setReviewedAt(new Timestamp(System.currentTimeMillis()));
            reviewTaskRepository.save(task);
            finishEvolveTaskAfterReview(task.getEvolveTaskId(), false, "48小时超时自动拒绝", "expire");
            boostTopicWeight(task.getEvolveTaskId(), false, task.getQualityScore());
        }

        if (!expired.isEmpty()) {
            log.info("[EvolutionReview] 自动过期 {} 条超时审核任务", expired.size());
        }
    }

    /**
     * pending_review 的进化任务：审核通过/修订通过 → completed + {@link EvolveTaskCompletedEvent} 解锁 DAG；
     * 拒绝/超时 → failed + errorMessage，不发布完成事件。
     */
    private void finishEvolveTaskAfterReview(Long evolveTaskDbId, boolean approved, String detail, String source) {
        if (evolveTaskDbId == null || evolveTaskRepository == null) {
            return;
        }
        AiEvolveTask et = evolveTaskRepository.findById(evolveTaskDbId).orElse(null);
        if (et == null) {
            return;
        }
        if (!"pending_review".equals(et.getStatus())) {
            log.debug("[EvolutionReview] 跳过进化任务状态流转: evolveTaskId={}, status={}", evolveTaskDbId, et.getStatus());
            return;
        }
        String parentNo = EvolveTaskDagSupport.parseDependsOnTaskNos(et.getDependsOnTaskNos()).stream()
                .findFirst().orElse(null);
        if (approved) {
            et.setStatus("completed");
            et.setBlockedReason(null);
            et.setErrorMessage(null);
            evolveTaskRepository.save(et);
            if (applicationEventPublisher != null && et.getKbId() != null && et.getTaskNo() != null) {
                applicationEventPublisher.publishEvent(new EvolveTaskCompletedEvent(et.getKbId(), et.getTaskNo()));
            }
            if (evolutionFitnessService != null && et.getTaskNo() != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("source", source);
                if (detail != null && !detail.isBlank()) {
                    String d = detail.length() > 300 ? detail.substring(0, 300) : detail;
                    payload.put("detail", d);
                }
                evolutionFitnessService.record(et.getKbId(), et.getTaskNo(), parentNo, "review_approved", 1.0,
                        JSON.toJSONString(payload), et.getAbExperimentId());
            }
        } else {
            et.setStatus("failed");
            String msg = detail != null && !detail.isBlank() ? detail : "审核未通过";
            if (msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            et.setErrorMessage(msg);
            evolveTaskRepository.save(et);
            if (evolutionFitnessService != null && et.getTaskNo() != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("source", source);
                payload.put("reason", msg);
                evolutionFitnessService.record(et.getKbId(), et.getTaskNo(), parentNo, "review_rejected", 0.0,
                        JSON.toJSONString(payload), et.getAbExperimentId());
            }
        }
    }

    private static int clampTopicPriority(Integer p) {
        if (p == null || p < 1 || p > 3) {
            return 2;
        }
        return p;
    }

    /**
     * 审核结果反哺主题权重（人类反馈闭环）
     * 通过：scoreAvg 上调、优先级提升（更频繁进化）
     * 拒绝：优先级下调（减少无效进化）
     */
    private void boostTopicWeight(Long evolveTaskId, boolean approved, Integer qualityScore) {
        if (evolveTaskId == null || evolveTaskRepository == null || evolveTopicRepository == null) return;
        try {
            AiEvolveTask evolveTask = evolveTaskRepository.findById(evolveTaskId).orElse(null);
            if (evolveTask == null || evolveTask.getTopicIds() == null || evolveTask.getTopicIds().isBlank()) return;

            String[] topicIdStrs = evolveTask.getTopicIds().split(",");
            for (String tidStr : topicIdStrs) {
                try {
                    Long topicId = Long.parseLong(tidStr.trim());
                    AiEvolveTopic topic = evolveTopicRepository.findById(topicId).orElse(null);
                    if (topic == null) continue;

                    if (approved) {
                        int score = qualityScore != null ? qualityScore : 60;
                        BigDecimal newScore = topic.getScoreAvg() == null
                                ? BigDecimal.valueOf(score)
                                : topic.getScoreAvg().multiply(BigDecimal.valueOf(0.8)).add(BigDecimal.valueOf(score * 0.2));
                        topic.setScoreAvg(newScore);
                        int p = clampTopicPriority(topic.getPriority());
                        topic.setPriority(Math.max(1, p - 1));
                    } else {
                        int p = clampTopicPriority(topic.getPriority());
                        topic.setPriority(Math.min(3, p + 1));
                    }
                    evolveTopicRepository.save(topic);
                    log.debug("[EvolutionReview] 主题权重反哺: topicId={}, approved={}, newPriority={}", topicId, approved, topic.getPriority());
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            log.warn("[EvolutionReview] 主题权重反哺失败: {}", e.getMessage());
        }
    }

    /**
     * D-2: 审核反馈循环 — 通过率低时触发 prompt 自优化
     */
    private void checkAndTriggerFeedback() {
        Timestamp recentWindow = new Timestamp(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        long approved = reviewTaskRepository.countApprovedSince(recentWindow);
        long total = reviewTaskRepository.countTotalSince(recentWindow);

        if (total < 10) return; // 样本量不足

        double approvalRate = (double) approved / total;

        if (approvalRate < 0.20 && promptSelfOptimizationService != null) {
            log.info("[EvolutionReview] 通过率过低({}%)，触发进化 prompt 自优化", String.format("%.1f", approvalRate * 100));
            try {
                promptSelfOptimizationService.forceOptimize("knowledge_evolve", null);
            } catch (Exception e) {
                log.warn("[EvolutionReview] 触发 prompt 自优化失败: {}", e.getMessage());
            }
        }

        if (approvalRate > 0.80) {
            log.info("[EvolutionReview] 通过率较高({}%)，可考虑扩大灰色地带范围", String.format("%.1f", approvalRate * 100));
        }
    }

    private Map<String, Object> toMap(AiEvolutionReviewTask task) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", task.getId());
        m.put("evolveTaskId", task.getEvolveTaskId());
        m.put("contentPreview", task.getContentPreview());
        m.put("qualityScore", task.getQualityScore());
        m.put("reviewerId", task.getReviewerId());
        m.put("reviewStatus", task.getReviewStatus());
        m.put("reviewComment", task.getReviewComment());
        m.put("revisedContent", task.getRevisedContent());
        m.put("reviewedAt", task.getReviewedAt());
        m.put("autoExpired", task.getAutoExpired());
        m.put("createTime", task.getCreateTime());
        return m;
    }
}
