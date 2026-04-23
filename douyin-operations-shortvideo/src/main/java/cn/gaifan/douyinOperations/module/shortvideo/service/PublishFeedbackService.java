package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * 发布后数据反馈闭环 (Phase 6.2)
 *
 * 流程:
 * 1. 视频发布到抖音
 * 2. 定期拉取播放量/点赞/评论/转发数据
 * 3. 与生成参数 (模型/运镜/prompt) 关联
 * 4. 计算内容效果评分
 * 5. 生成周报/反思报告
 * 6. 反馈到知识库，优化推荐
 */
public interface PublishFeedbackService {

    /**
     * 内容效果评分
     * 基于抖音数据计算，满分 100
     */
    record ContentScore(
        double overallScore,
        double completionRate,
        double engagementRate,
        double followerGrowth,
        double viewsVsAvg,
        double durationFit,
        String performance,
        List<String> insights
    ) {}

    /** 分析单个视频的发布效果（需 visibleOwnerIds 校验，null 表示管理员不限制） */
    ContentScore analyzePerformance(Long videoId, java.util.List<Long> visibleOwnerIds);

    /** 生成周报 */
    Map<String, Object> generateWeeklyReport(Long userId);

    /** 反思报告（需 visibleOwnerIds 校验） */
    Map<String, Object> generateReflectionReport(Long videoId, java.util.List<Long> visibleOwnerIds);

    /** 反馈到知识库 */
    void feedbackToKnowledge(Long videoId, ContentScore score);
}
