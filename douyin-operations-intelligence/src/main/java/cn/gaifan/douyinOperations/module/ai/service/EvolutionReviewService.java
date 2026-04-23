package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;

import java.util.Map;

/**
 * 进化审核服务：管理灰色地带（40-50分）内容的人工审核工作流
 */
public interface EvolutionReviewService {

    /**
     * 创建审核任务（由进化引擎在评分 40-50 时调用）
     */
    void createReviewTask(Long evolveTaskId, String contentPreview, int qualityScore);

    /**
     * 分页查询审核任务
     */
    PageResultVO<Map<String, Object>> listReviewTasks(String status, int page, int rows);

    /**
     * 通过审核
     */
    void approve(Long reviewTaskId, Long reviewerId, String comment);

    /**
     * 拒绝
     */
    void reject(Long reviewTaskId, Long reviewerId, String comment);

    /**
     * 修订后通过
     */
    void revise(Long reviewTaskId, Long reviewerId, String revisedContent, String comment);

    /**
     * 获取审核统计
     */
    Map<String, Object> getReviewStats();

    /**
     * 自动过期超时审核任务（48h）
     */
    void expireTimeoutTasks();
}
