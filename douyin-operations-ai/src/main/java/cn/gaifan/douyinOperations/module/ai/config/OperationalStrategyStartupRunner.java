package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时确保 AI 学习中心具备抖音运营策略种子。
 */
@Component
@Order(121)
@ConditionalOnProperty(prefix = "app.ai.ops-strategy", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OperationalStrategyStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperationalStrategyStartupRunner.class);

    @Resource
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @Value("${app.douyin-school.collector.user-id:1}")
    private Long seedUserId;

    @Override
    public void run(ApplicationArguments args) {
        if (seedUserId == null || seedUserId <= 0) {
            return;
        }
        try {
            operationalStrategyKnowledgeService.ensureSeeded(seedUserId);
            log.info("抖音运营策略学习种子初始化完成 userId={}", seedUserId);
        } catch (Exception e) {
            log.warn("抖音运营策略学习种子初始化跳过 userId={}, err={}", seedUserId, e.getMessage());
        }
    }
}
