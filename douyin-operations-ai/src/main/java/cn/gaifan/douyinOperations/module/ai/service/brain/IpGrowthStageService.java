package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.Map;

/**
 * IP 增长阶段判断服务。
 * 基于 BusinessParamConfig.IpGrowth 参数判断账号所处增长阶段，
 * 驱动差异化内容策略。
 */
public interface IpGrowthStageService {

    /**
     * 判断现象级 IP 所处阶段
     * @param followerCount 当前粉丝数
     * @return stage: seed/boost/stable/super_ip
     */
    String getPhenomenalStage(long followerCount);

    /**
     * 判断顶级 IP 所处阶段
     * @param operatingMonths 运营月数
     * @return stage: foundation/expansion/ecosystem
     */
    String getTopStage(int operatingMonths);

    /**
     * 获取阶段对应的内容策略建议
     * @param ipType "phenomenal" 或 "top"
     * @param stage 阶段标识
     * @return 策略描述 Map
     */
    Map<String, Object> getStageStrategy(String ipType, String stage);

    /**
     * 获取 IP 运营指标基线（用于效果评估）
     * @param ipType "phenomenal" 或 "top"
     * @return 基线指标 Map
     */
    Map<String, Object> getMetricsBaseline(String ipType);
}
