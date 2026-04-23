package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryCausalEngine;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 因果因子自适应学习定时任务（Phase 3.2）
 * 每周从 live_script_effectiveness 聚合更新话术类型因子
 */
@Component
public class CausalFactorAdaptScheduler {

    private static final Logger log = LoggerFactory.getLogger(CausalFactorAdaptScheduler.class);

    @Value("${app.ai.brain.causal-engine.adapt-enabled:true}")
    private boolean adaptEnabled;

    @Resource
    private IndustryCausalEngine causalEngine;

    @Scheduled(cron = "${app.ai.brain.causal-engine.adapt-cron:0 0 4 * * MON}")
    public void adaptFactors() {
        if (!adaptEnabled) {
            log.debug("因果因子自适应已禁用，跳过");
            return;
        }
        try {
            log.info("开始更新因果因子...");
            causalEngine.adaptFactorsFromEffectiveness();
            log.info("因果因子更新完成");
        } catch (Exception e) {
            log.error("因果因子更新失败", e);
        }
    }
}
