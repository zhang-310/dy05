package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 * 每日拍摄脚本 Service
 */
public interface DailyShootService {

    /**
     * AI 生成每日拍摄脚本（1-3 条）
     *
     * @param personaId    人设 ID
     * @param scheduleDate 计划拍摄日期 yyyy-MM-dd
     * @param count        生成数量 1-3
     * @param style        风格偏好（种草/测评/剧情/口播）
     * @param duration     目标时长（30秒/60秒）
     * @param topic        指定主题（空则 AI 自动选题）
     * @return plans 创建的方案列表，quotaRemaining 剩余配额
     */
    Map<String, Object> generateDaily(Long userId, Long personaId, String scheduleDate,
                                      Integer count, String style, String duration, String topic);

    /**
     * 每日脚本列表（按日期/人设筛选）
     */
    List<Map<String, Object>> dailyList(Long userId, String scheduleDate, Long personaId);

    /**
     * 分镜审核：标记通过/需修改
     *
     * @return allApproved 是否全部分镜通过，shootStatus 若全通过则更新为 ready
     */
    Map<String, Object> reviewShot(Long userId, Long shotId, String reviewStatus, String reviewerNote);

    /**
     * 更新拍摄状态
     */
    void updateShootStatus(Long userId, Long projectId, String shootStatus);

    /**
     * 导出拍摄脚本（可打印格式）
     */
    String exportScript(Long userId, Long projectId);
}
