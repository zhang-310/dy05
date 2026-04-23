package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkTask;
import cn.gaifan.douyinOperations.module.benchmark.metrics.BenchmarkMetrics;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkTaskRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkTaskService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkTaskSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkTaskVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分析任务管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkTaskServiceImpl implements BenchmarkTaskService {

    private final BenchmarkTaskRepository taskRepository;
    private final BenchmarkMetrics metrics;

    @Override
    @Cacheable(value = "CACHE_TASK_LIST", key = "#searchVO.hashCode() + '_' + #ownerId")
    public PageResultVO<BenchmarkTaskVO> search(BenchmarkTaskSearchVO searchVO, Long ownerId) {
        searchVO.validateParams();

        log.debug("查询任务列表: ownerId={}, accountId={}, type={}, status={}",
                ownerId, searchVO.getBenchmarkAccountId(), searchVO.getTaskType(), searchVO.getTaskStatus());

        Specification<BenchmarkTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据隔离
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            // 账号ID过滤
            if (searchVO.getBenchmarkAccountId() != null) {
                predicates.add(cb.equal(root.get("benchmarkAccountId"), searchVO.getBenchmarkAccountId()));
            }

            // 任务类型过滤
            if (StringUtils.hasText(searchVO.getTaskType())) {
                predicates.add(cb.equal(root.get("taskType"), searchVO.getTaskType()));
            }

            // 任务状态过滤
            if (StringUtils.hasText(searchVO.getTaskStatus())) {
                predicates.add(cb.equal(root.get("taskStatus"), searchVO.getTaskStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(), sort);
        Page<BenchmarkTask> page = taskRepository.findAll(spec, pageable);

        List<BenchmarkTaskVO> voList = page.getContent().stream()
                .map(this::entityToVO)
                .toList();

        log.debug("查询任务列表完成: total={}, page={}", page.getTotalElements(), searchVO.getPage());

        return new PageResultVO<>(page.getTotalElements(), voList, searchVO.getPage(), searchVO.getRows());
    }

    @Override
    public BenchmarkTaskVO getById(Long id, Long ownerId) {
        log.debug("查询任务详情: id={}, ownerId={}", id, ownerId);

        BenchmarkTask task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (!task.getOwnerId().equals(ownerId)) {
            log.warn("无权访问任务: id={}, ownerId={}, taskOwnerId={}", id, ownerId, task.getOwnerId());
            throw new RuntimeException("无权访问该任务");
        }

        return entityToVO(task);
    }

    @Override
    @Transactional
    @CacheEvict(value = "CACHE_TASK_LIST", allEntries = true)
    public BenchmarkTaskVO createTask(String taskType, Long benchmarkAccountId, String configJson, Long ownerId) {
        MDC.put("ownerId", String.valueOf(ownerId));
        MDC.put("taskType", taskType);

        try {
            log.info("创建任务: type={}, accountId={}, ownerId={}", taskType, benchmarkAccountId, ownerId);

            BenchmarkTask task = new BenchmarkTask();
            task.setOwnerId(ownerId);
            task.setBenchmarkAccountId(benchmarkAccountId);
            task.setTaskType(taskType);
            task.setTaskStatus("pending");
            task.setProgress(0);
            task.setConfigJson(configJson);
            task.setStartedAt(LocalDateTime.now());

            task = taskRepository.save(task);

            metrics.recordTaskCreated(taskType);

            log.info("任务创建成功: id={}, type={}", task.getId(), taskType);

            return entityToVO(task);
        } finally {
            MDC.clear();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "CACHE_TASK_LIST", allEntries = true)
    public void updateProgress(Long taskId, Integer progress, Integer processedVideos, Integer failedVideos) {
        log.debug("更新任务进度: id={}, progress={}%, processed={}, failed={}",
                taskId, progress, processedVideos, failedVideos);

        taskRepository.findById(taskId).ifPresent(task -> {
            task.setProgress(progress);
            task.setProcessedVideos(processedVideos);
            task.setFailedVideos(failedVideos);
            taskRepository.save(task);
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = "CACHE_TASK_LIST", allEntries = true)
    public void updateStatus(Long taskId, String status, String resultSummary, String errorMessage) {
        MDC.put("taskId", String.valueOf(taskId));
        MDC.put("status", status);

        try {
            log.info("更新任务状态: id={}, status={}, summary={}, error={}",
                    taskId, status, resultSummary, errorMessage);

            taskRepository.findById(taskId).ifPresent(task -> {
                String oldStatus = task.getTaskStatus();
                task.setTaskStatus(status);
                task.setResultSummary(resultSummary);
                task.setErrorMessage(errorMessage);

                if ("completed".equals(status) || "failed".equals(status)) {
                    task.setCompletedAt(LocalDateTime.now());
                    task.setProgress(100);

                    // 计算任务耗时
                    if (task.getStartedAt() != null) {
                        long duration = java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis();
                        metrics.recordTaskDuration(task.getTaskType(), duration);
                    }

                    // 记录任务完成指标
                    metrics.recordTaskCompleted(task.getTaskType(), "completed");
                }

                taskRepository.save(task);

                log.info("任务状态更新成功: id={}, {} -> {}", taskId, oldStatus, status);
            });
        } finally {
            MDC.clear();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "CACHE_TASK_LIST", allEntries = true)
    public void delete(Long id, Long ownerId) {
        log.info("删除任务: id={}, ownerId={}", id, ownerId);

        BenchmarkTask task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (!task.getOwnerId().equals(ownerId)) {
            log.warn("无权删除任务: id={}, ownerId={}, taskOwnerId={}", id, ownerId, task.getOwnerId());
            throw new RuntimeException("无权删除该任务");
        }

        taskRepository.delete(task);
        log.info("任务删除成功: id={}", id);
    }

    private BenchmarkTaskVO entityToVO(BenchmarkTask entity) {
        BenchmarkTaskVO vo = new BenchmarkTaskVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
