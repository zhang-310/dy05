package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.DeepEvolveService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 深度进化 Agent 定时任务：每周对待深化问题做专项研究
 */
@Component
public class DeepEvolveScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeepEvolveScheduler.class);

    @Value("${app.ai.deep-evolve.enabled:true}")
    private boolean deepEvolveEnabled;

    @Value("${app.ai.deep-evolve.max-per-run:3}")
    private int maxPerRun;

    @Resource
    private DeepEvolveService deepEvolveService;

    @Resource
    private EvolveEngineService evolveEngineService;

    @Scheduled(cron = "${app.ai.deep-evolve.cron:0 30 6 * * WED,SUN}")
    public void runDeepEvolve() {
        if (!deepEvolveEnabled) return;
        try {
            List<Long> kbIds = evolveEngineService.resolveEvolveKbIds();
            if (kbIds.isEmpty()) return;
            Long kbId = kbIds.get(0);
            int done = deepEvolveService.runDeepEvolve(kbId, maxPerRun);
            if (done > 0) log.info("深度进化定时任务完成: 入库 {} 篇", done);
        } catch (Exception e) {
            log.error("深度进化定时任务失败", e);
        }
    }
}
