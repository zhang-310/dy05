package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;

import java.util.List;
import java.util.Map;

/**
 * 直播话术效果评分服务接口
 * W-04: 效果评分系统
 */
public interface EffectivenessScoreService {

    /**
     * 计算单个话术版本的效果评分
     * 公式：score = conversion_rate * 0.4 + (likes / max_likes) * 100 * 0.3
     *        + (comments / max_comments) * 100 * 0.2 + completion_rate * 100 * 0.1
     *
     * @param scriptId 话术 ID
     * @param sessionId 直播场次 ID
     * @return 评分结果 map，包含 totalScore/conversionRate/likes/comments/completionRate
     */
    Map<String, Object> calculateScore(Long scriptId, Long sessionId);

    /**
     * 批量计算直播场次所有话术的评分并生成排行榜
     *
     * @param sessionId 直播场次 ID
     * @return 排行榜列表（按评分降序）
     */
    List<Map<String, Object>> calculateSessionRanking(Long sessionId);

    /**
     * 版本对比：对比两个话术版本的效果差异
     *
     * @param versionA 版本 A 的脚本 ID
     * @param versionB 版本 B 的脚本 ID
     * @return 对比结果，包含双方的关键指标和增长/衰减趋势
     */
    Map<String, Object> compareVersions(Long versionA, Long versionB);

    /**
     * 获取排行榜（按评分排序）
     *
     * @param sessionId 直播场次 ID
     * @param page 页码（0-indexed）
     * @param pageSize 每页数量
     * @return 分页排行榜
     */
    PageResultVO<Map<String, Object>> getRanking(Long sessionId, int page, int pageSize);

    /**
     * 获取热门话术（评分排名前 3）
     *
     * @param sessionId 直播场次 ID
     * @return 热门话术列表
     */
    List<Map<String, Object>> getTopScripts(Long sessionId, int limit);

    /**
     * 获取推荐话术（评分 >= 7 分）
     *
     * @param sessionId 直播场次 ID
     * @return 推荐话术列表
     */
    List<Map<String, Object>> getRecommendedScripts(Long sessionId);

    /**
     * 获取新兴话术（最近 24h 排名上升的话术）
     *
     * @param sessionId 直播场次 ID
     * @return 新兴话术列表
     */
    List<Map<String, Object>> getEmergedScripts(Long sessionId);

    /**
     * 查询单个话术的效果详情
     *
     * @param scriptId 话术 ID
     * @return 效果详情（评分、排名、指标、标签等）
     */
    Map<String, Object> getScriptEffectiveness(Long scriptId);

    /**
     * 定时任务：每小时更新所有活跃场次的评分
     */
    void scheduleScoreUpdate();
}
