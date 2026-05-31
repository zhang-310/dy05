package cn.gaifan.douyinOperations.module.ai.config.official;

import cn.gaifan.douyinOperations.module.ai.service.official.DouyinSchoolCollectorService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DouyinSchoolCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(DouyinSchoolCollectorScheduler.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Resource
    private DouyinSchoolCollectorProperties properties;

    @Resource
    private DouyinSchoolCollectorService collectorService;

    @Resource
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Scheduled(cron = "${app.douyin-school.collector.cron:0 40 2 * * ?}")
    public void dailyCollect() {
        if (!properties.isEnabled()) {
            log.debug("抖音学习中心官方资料采集已禁用，跳过");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("抖音学习中心官方资料采集仍在运行，跳过本轮");
            return;
        }
        taskExecutor.execute(() -> {
            try {
                log.info("抖音学习中心官方资料每日采集开始");
                Map<String, Object> result = collectorService.collect();
                log.info("抖音学习中心官方资料每日采集完成: {}", result);
            } catch (Exception e) {
                log.error("抖音学习中心官方资料每日采集失败", e);
            } finally {
                running.set(false);
            }
        });
    }
}
