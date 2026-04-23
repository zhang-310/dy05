package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkTaskSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkTaskVO;

/**
 * 分析任务管理服务
 */
public interface BenchmarkTaskService {

    /**
     * 分页查询任务
     */
    PageResultVO<BenchmarkTaskVO> search(BenchmarkTaskSearchVO searchVO, Long ownerId);

    /**
     * 根据ID获取任务
     */
    BenchmarkTaskVO getById(Long id, Long ownerId);

    /**
     * 创建任务
     */
    BenchmarkTaskVO createTask(String taskType, Long benchmarkAccountId, String configJson, Long ownerId);

    /**
     * 更新任务进度
     */
    void updateProgress(Long taskId, Integer progress, Integer processedVideos, Integer failedVideos);

    /**
     * 更新任务状态
     */
    void updateStatus(Long taskId, String status, String resultSummary, String errorMessage);

    /**
     * 删除任务
     */
    void delete(Long id, Long ownerId);
}
