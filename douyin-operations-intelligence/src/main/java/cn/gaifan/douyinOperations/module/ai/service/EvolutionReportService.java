package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * 进化报告服务：周报、月报、看板数据聚合
 */
public interface EvolutionReportService {

    /**
     * 进化看板报告（供前端仪表盘）
     * 含：ROI 指标、周/月统计、各来源效果、冷门数量
     */
    Map<String, Object> getDashboardReport();
}
