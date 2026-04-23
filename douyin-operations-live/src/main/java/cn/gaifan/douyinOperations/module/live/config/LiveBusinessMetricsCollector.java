package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LiveBusinessMetricsCollector {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired
    private LiveSessionRepository sessionRepository;

    @PostConstruct
    public void registerMetrics() {
        if (meterRegistry == null) {
            log.debug("[LiveBusinessMetrics] MeterRegistry 不可用，跳过指标注册");
            return;
        }

        Gauge.builder("live.sessions.active", () -> {
            try {
                return sessionRepository.findByStatusAndDeleted(1, 0, Pageable.unpaged()).getTotalElements();
            } catch (Exception e) {
                return 0;
            }
        }).description("当前直播中场次数").register(meterRegistry);

        log.info("[LiveBusinessMetrics] 业务指标注册完成");
    }
}
