package cn.gaifan.douyinOperations.module.ai.service.brain;

import cn.gaifan.douyinOperations.module.ai.vo.CounterfactualResultVO;

import java.util.List;
import java.util.Map;

/**
 * 行业因果推理引擎（Phase1 + Phase3.2）
 * 输入：话术类型、主播人设、产品类型、时间段
 * 输出：预期转化率、关键影响因素、风险点
 */
public interface IndustryCausalEngine {

    /**
     * 因果推断：给定策略要素，预测效果与影响因素
     *
     * @param input 话术类型、人设、产品、时段等
     * @return 预期转化率、影响因素、风险点
     */
    CausalInferenceResult infer(Map<String, Object> input);

    /**
     * 反事实推理：如果改变某因素，转化率会如何变化（Phase 3.2）
     *
     * @param currentState 当前状态
     * @param intervention 干预（要改变的因素）
     * @return 预测变化、影响路径、置信区间、建议
     */
    CounterfactualResultVO counterfactual(Map<String, Object> currentState, Map<String, Object> intervention);

    /**
     * 策略解释：说明为什么某种组合效果好/差
     */
    String explainStrategy(String strategyId, Map<String, Object> context);

    boolean isAvailable();

    /**
     * Phase 3.2：从 live_script_effectiveness 聚合更新话术类型因子（实现类在 app 域 glue）
     */
    void adaptFactorsFromEffectiveness();

    record CausalInferenceResult(
            double expectedConversionRate,
            List<String> keyFactors,
            List<String> riskPoints,
            String explanation
    ) {}
}
