package cn.gaifan.douyinOperations.module.ai.service.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona;

import java.util.List;
import java.util.Map;

/**
 * 五位主播协同优化引擎（P2）
 * 流量路径、资源转化、跨主播协同
 */
public interface FiveHostsSynergyService {

    /**
     * 获取流量路径：入口→转化→B端
     */
    FlowPath getFlowPath();

    /**
     * 获取 C 端粉丝转化为 B 端客户的推荐路径
     */
    ResourceTransferPlan getTransferPlan();

    /**
     * 获取五位主播协同策略摘要
     */
    Map<String, Object> getSynergySummary();

    boolean isAvailable();

    record FlowPath(
            List<AiHostPersona> entranceHosts,
            List<AiHostPersona> conversionHosts,
            List<AiHostPersona> sinkHosts,
            String description
    ) {}

    record ResourceTransferPlan(
            List<String> transferTriggers,
            List<String> transferChannels,
            String expectedTransferRate,
            String suggestion
    ) {}
}
