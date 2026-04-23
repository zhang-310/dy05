package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.LiveAnalysisVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveReviewVO;

/**
 * 直播 AI 分析服务
 */
public interface LiveAnalysisService {

    /**
     * 生成 AI 分析报告并保存
     */
    LiveAnalysisVO generate(Long sessionId);

    /**
     * 获取已保存的 AI 分析报告（来自 live_session_data.ai_analysis）
     */
    LiveAnalysisVO get(Long sessionId);

    /**
     * 获取 AI 复盘报告（来自 ai_live_review，通过 ai_review_id 或 sessionId 关联）
     */
    LiveReviewVO getReview(Long sessionId);
}
