package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentReviewSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentReviewVO;

import java.util.Map;

/**
 * 智能体评论服务接口
 */
public interface AgentReviewService {

    /**
     * 获取智能体的评分统计
     */
    Map<String, Object> getRatingStats(Long agentId);

    /**
     * 分页获取智能体评论列表
     */
    PageResultVO<AgentReviewVO> listReviews(Long agentId, int page, int rows);

    /**
     * 提交或更新评论
     */
    long submitReview(Long userId, AgentReviewSaveVO vo);

    /**
     * 获取用户对某智能体的评论
     */
    AgentReviewVO getUserReview(Long agentId, Long userId);
}
