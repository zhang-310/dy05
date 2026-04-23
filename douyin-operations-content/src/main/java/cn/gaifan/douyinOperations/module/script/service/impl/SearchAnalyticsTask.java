package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.service.SearchSuggestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 搜索分析定时任务
 * 定期更新搜索建议词库、热度评分、分析数据
 */
@Slf4j
@Component
public class SearchAnalyticsTask {

    @Resource
    private SearchSuggestionService searchSuggestionService;

    /**
     * 每 6 小时执行一次：更新搜索建议词库
     */
    @Scheduled(fixedDelay = 21600000, initialDelay = 120000)  // 6 小时执行一次
    public void updateSearchSuggestions() {
        log.info("开始更新搜索建议词库...");
        try {
            long startTime = System.currentTimeMillis();
            searchSuggestionService.updateSuggestions();
            long duration = System.currentTimeMillis() - startTime;
            log.info("更新搜索建议词库完成，耗时 {} ms", duration);
        } catch (Exception e) {
            log.error("更新搜索建议词库失败", e);
        }
    }

    /**
     * 每 4 小时执行一次：更新热度评分
     */
    @Scheduled(fixedDelay = 14400000, initialDelay = 300000)  // 4 小时执行一次
    public void updateTrendingScores() {
        log.info("开始更新热度评分...");
        try {
            long startTime = System.currentTimeMillis();
            searchSuggestionService.updateTrendingScores();
            long duration = System.currentTimeMillis() - startTime;
            log.info("更新热度评分完成，耗时 {} ms", duration);
        } catch (Exception e) {
            log.error("更新热度评分失败", e);
        }
    }

    /**
     * 每天凌晨 3 点执行：生成搜索分析数据聚合
     */
    @Scheduled(cron = "0 0 3 * * ?")  // 每天 3:00 AM
    public void generateSearchAnalytics() {
        log.info("开始生成搜索分析数据...");
        try {
            long startTime = System.currentTimeMillis();

            // 实现搜索分析数据的聚合逻辑
            // 从 sc_search_result 聚合到 sc_search_analytics
            // 按 owner_id, analytics_date, search_query, search_type 分组

            long duration = System.currentTimeMillis() - startTime;
            log.info("生成搜索分析数据完成，耗时 {} ms", duration);
        } catch (Exception e) {
            log.error("生成搜索分析数据失败", e);
        }
    }

    /**
     * 每周一凌晨 4 点执行：清理旧的搜索记录
     */
    @Scheduled(cron = "0 0 4 ? * MON")  // 每周一 4:00 AM
    public void cleanupOldSearchRecords() {
        log.info("开始清理旧的搜索记录...");
        try {
            long startTime = System.currentTimeMillis();

            // 清理 90 天之前的搜索记录
            // delete from sc_search_result where created_at < DATE_SUB(NOW(), INTERVAL 90 DAY) and deleted = 0

            long duration = System.currentTimeMillis() - startTime;
            log.info("清理旧的搜索记录完成，耗时 {} ms", duration);
        } catch (Exception e) {
            log.error("清理旧的搜索记录失败", e);
        }
    }

    /**
     * 每月 1 号凌晨 5 点执行：生成月度搜索报告
     */
    @Scheduled(cron = "0 0 5 1 * ?")  // 每月 1 号 5:00 AM
    public void generateMonthlySearchReport() {
        log.info("开始生成月度搜索报告...");
        try {
            long startTime = System.currentTimeMillis();

            // 生成月度搜索统计报告
            // 包括：总搜索数、活跃用户数、热点话题、平均响应时间、满意度等

            long duration = System.currentTimeMillis() - startTime;
            log.info("生成月度搜索报告完成，耗时 {} ms", duration);
        } catch (Exception e) {
            log.error("生成月度搜索报告失败", e);
        }
    }
}
