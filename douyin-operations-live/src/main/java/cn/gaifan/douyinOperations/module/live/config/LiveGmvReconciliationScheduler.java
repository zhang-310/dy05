package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * 已结束场次：支付订单实收 vs LiveMonitor GMV 峰值的粗略对账（INFRA-03）。
 * 二者口径可能不一致，仅供运营与告警参考。
 */
@Component
@ConditionalOnProperty(prefix = "app.live.gmv-reconciliation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LiveGmvReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveGmvReconciliationScheduler.class);

    private final LiveSessionRepository liveSessionRepository;
    private final LiveMonitorRepository liveMonitorRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final MeterRegistry meterRegistry;

    private Counter mismatchCounter;
    private Counter sessionsScannedCounter;

    @Value("${app.live.gmv-reconciliation.lookback-hours:72}")
    private int lookbackHours;
    @Value("${app.live.gmv-reconciliation.max-sessions-per-run:200}")
    private int maxSessionsPerRun;
    @Value("${app.live.gmv-reconciliation.min-delta-yuan:10}")
    private BigDecimal minDeltaYuan;

    public LiveGmvReconciliationScheduler(LiveSessionRepository liveSessionRepository,
                                          LiveMonitorRepository liveMonitorRepository,
                                          PaymentOrderRepository paymentOrderRepository,
                                          MeterRegistry meterRegistry) {
        this.liveSessionRepository = liveSessionRepository;
        this.liveMonitorRepository = liveMonitorRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMeters() {
        this.mismatchCounter = Counter.builder("live.gmv.reconciliation.mismatch")
                .description("Ended live sessions where |payment sum - monitor max gmv| exceeds threshold")
                .register(meterRegistry);
        this.sessionsScannedCounter = Counter.builder("live.gmv.reconciliation.sessions_scanned")
                .description("Ended live sessions scanned in reconciliation run")
                .register(meterRegistry);
    }

    @Scheduled(cron = "${app.live.gmv-reconciliation.cron:0 20 3 * * *}")
    public void reconcile() {
        Instant since = Instant.now().minusSeconds(Math.max(1, lookbackHours) * 3600L);
        List<LiveSession> sessions = liveSessionRepository.findEndedSessionsWithEndTimeSince(Timestamp.from(since));
        int scanned = 0;
        for (LiveSession s : sessions) {
            if (scanned >= Math.max(1, maxSessionsPerRun)) {
                break;
            }
            scanned++;
            Long sid = s.getId();
            if (sid == null) {
                continue;
            }
            List<OrderStatus> statuses = List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED);
            BigDecimal paySum = paymentOrderRepository.sumActualAmountByLiveSessionIdAndStatuses(sid, statuses);
            if (paySum == null) {
                paySum = BigDecimal.ZERO;
            }
            BigDecimal monitorMax = liveMonitorRepository.findMaxGmvBySessionId(sid);
            if (monitorMax == null) {
                monitorMax = BigDecimal.ZERO;
            }
            BigDecimal delta = paySum.subtract(monitorMax).abs();
            if (delta.compareTo(minDeltaYuan != null ? minDeltaYuan : BigDecimal.TEN) > 0) {
                mismatchCounter.increment();
                log.warn("GMV reconciliation mismatch sessionId={} paySum={} monitorMaxGmv={} delta={}",
                        sid, paySum, monitorMax, delta);
            }
        }
        sessionsScannedCounter.increment(scanned);
        log.debug("GMV reconciliation finished, scanned={}", scanned);
    }
}
