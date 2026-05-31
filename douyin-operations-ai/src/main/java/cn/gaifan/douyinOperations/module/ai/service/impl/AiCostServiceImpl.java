package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModelPricing;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelPricingRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiCostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * AI 成本计算服务实现 — Token 级计费 + 日预算管控
 */
@Service
public class AiCostServiceImpl implements AiCostService {

    private static final Logger log = LoggerFactory.getLogger(AiCostServiceImpl.class);
    private static final String DAILY_COST_KEY_PREFIX = "ai:cost:daily:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired(required = false)
    private AiModelPricingRepository pricingRepository;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Value("${app.ai.quota.daily-budget-cny:100}")
    private double dailyBudgetCny;

    @Override
    public BigDecimal calculateCost(String modelName, long inputTokens, long outputTokens) {
        if (pricingRepository == null || modelName == null) return BigDecimal.ZERO;
        return pricingRepository.findByModelNameAndDeleted(modelName, 0)
                .map(pricing -> {
                    BigDecimal inputCost = pricing.getInputPricePer1k()
                            .multiply(BigDecimal.valueOf(inputTokens))
                            .divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);
                    BigDecimal outputCost = pricing.getOutputPricePer1k()
                            .multiply(BigDecimal.valueOf(outputTokens))
                            .divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);
                    return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
                })
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public void recordCost(String modelName, String provider, long inputTokens, long outputTokens) {
        BigDecimal cost = calculateCost(modelName, inputTokens, outputTokens);
        if (cost.compareTo(BigDecimal.ZERO) <= 0) return;

        // 累加到 Redis 日统计
        if (stringRedisTemplate != null) {
            String key = DAILY_COST_KEY_PREFIX + LocalDate.now().format(DATE_FMT);
            try {
                stringRedisTemplate.opsForValue().increment(key, cost.doubleValue());
                stringRedisTemplate.expire(key, Duration.ofDays(7));
            } catch (Exception e) {
                log.debug("Redis 成本记录失败: {}", e.getMessage());
            }
        }

        // 预算告警
        double todayCost = getTodayCostCny().doubleValue();
        double budgetRatio = todayCost / dailyBudgetCny;
        if (budgetRatio > 0.95) {
            log.error("[AI 成本] 今日成本已超过日预算 95%: cost={}CNY, budget={}CNY", todayCost, dailyBudgetCny);
        } else if (budgetRatio > 0.80) {
            log.warn("[AI 成本] 今日成本已超过日预算 80%: cost={}CNY, budget={}CNY", todayCost, dailyBudgetCny);
        }

        log.debug("[AI 成本] provider={}, model={}, input={}, output={}, cost={}CNY",
                provider, modelName, inputTokens, outputTokens, cost);
    }

    @Override
    public BigDecimal getTodayCostCny() {
        if (stringRedisTemplate == null) return BigDecimal.ZERO;
        String key = DAILY_COST_KEY_PREFIX + LocalDate.now().format(DATE_FMT);
        try {
            String val = stringRedisTemplate.opsForValue().get(key);
            return val != null ? new BigDecimal(val).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.debug("Redis 成本查询失败: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public boolean isDailyBudgetExceeded() {
        return getTodayCostCny().doubleValue() >= dailyBudgetCny * 0.95;
    }
}
