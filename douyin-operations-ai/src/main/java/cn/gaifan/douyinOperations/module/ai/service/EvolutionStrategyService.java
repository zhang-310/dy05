package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 进化策略服务：基于效果统计动态调整 viral/live 入库阈值（P2 策略迭代）
 */
public interface EvolutionStrategyService {

    /**
     * 建议的爆款拆解入库质量阈值。当 viral 文档平均引用率低时，提高阈值以减少低质量入库。
     * @param defaultQuality 配置的默认值
     * @return 建议值，null 表示使用 defaultQuality
     */
    Integer suggestViralMinQuality(int defaultQuality);

    /**
     * 建议的直播复盘入库最小字符数。当 live_review 文档平均引用率低时，提高字数要求。
     * @param defaultChars 配置的默认值
     * @return 建议值，null 表示使用 defaultChars
     */
    Integer suggestLiveMinReportChars(int defaultChars);
}
