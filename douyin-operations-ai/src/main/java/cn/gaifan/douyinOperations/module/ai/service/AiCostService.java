package cn.gaifan.douyinOperations.module.ai.service;

import java.math.BigDecimal;

/**
 * AI 调用成本计算服务
 */
public interface AiCostService {

    /**
     * 计算单次 LLM 调用成本
     * @param modelName 模型版本名（如 deepseek-chat）
     * @param inputTokens 输入 token 数
     * @param outputTokens 输出 token 数
     * @return 成本（CNY），无价格数据时返回 ZERO
     */
    BigDecimal calculateCost(String modelName, long inputTokens, long outputTokens);

    /**
     * 记录一次 LLM 调用的成本（累加到当日统计）
     */
    void recordCost(String modelName, String provider, long inputTokens, long outputTokens);

    /**
     * 获取今日累计成本（CNY）
     */
    BigDecimal getTodayCostCny();

    /**
     * 检查是否超出日预算
     * @return true 表示已超出 95% 预算
     */
    boolean isDailyBudgetExceeded();
}
