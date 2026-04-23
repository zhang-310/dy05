package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 索引队列积压（pending 且可重试）暴露到 Prometheus，供阶段 B SLO 与告警使用。
 */
@Component
public class IndexQueueMetrics {

    private static final Logger log = LoggerFactory.getLogger(IndexQueueMetrics.class);

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired
    private AiIndexQueueRepository indexQueueRepository;

    @PostConstruct
    public void register() {
        if (meterRegistry == null) {
            return;
        }
        Counter scrapeErrors = Counter.builder("ai.index.queue.gauge_scrape_error")
                .description("Errors while scraping index queue gauges (repository failures)")
                .register(meterRegistry);

        Gauge.builder("ai.index.queue.pending", indexQueueRepository, repo -> {
                    try {
                        return (double) repo.countPendingEligible();
                    } catch (Exception e) {
                        log.warn("ai.index.queue.pending scrape failed: {}", e.getMessage());
                        scrapeErrors.increment();
                        return 0.0;
                    }
                })
                .description("Index queue tasks in pending status (retryCount < 3)")
                .register(meterRegistry);

        Gauge.builder("ai.index.queue.pending_lag_ms_max", indexQueueRepository, repo -> {
                    try {
                        return repo.maxPendingLagMsEligible();
                    } catch (Exception e) {
                        log.warn("ai.index.queue.pending_lag_ms_max scrape failed: {}", e.getMessage());
                        scrapeErrors.increment();
                        return 0.0;
                    }
                })
                .description("Max age in ms of oldest pending eligible index queue task")
                .register(meterRegistry);
    }
}
