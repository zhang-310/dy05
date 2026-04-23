package cn.gaifan.douyinOperations.module.douyinapi.schedule;

import cn.gaifan.douyinOperations.module.douyinapi.entity.OAuthToken;
import cn.gaifan.douyinOperations.module.douyinapi.repository.OAuthTokenRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抖音 OAuth token 探活（基于 DB 过期时间与 refresh_token 可用性，不打开放平台）。
 * 配合 {@link cn.gaifan.douyinOperations.module.douyinapi.collector.DouyinDataCollector} 的跳过计数器做可观测性。
 */
@Component
@ConditionalOnProperty(name = "douyin.api.token-health.enabled", havingValue = "true", matchIfMissing = true)
public class DouyinOAuthTokenHealthProbe {

    private static final Logger log = LoggerFactory.getLogger(DouyinOAuthTokenHealthProbe.class);
    private static final String PROVIDER_DOUYIN = "douyin";

    @Resource
    private OAuthTokenRepository oauthTokenRepository;

    @Resource
    private MeterRegistry meterRegistry;

    private final AtomicInteger gaugeExpired = new AtomicInteger();
    private final AtomicInteger gaugeExpiringSoon = new AtomicInteger();
    private final AtomicInteger gaugeOk = new AtomicInteger();
    private final AtomicInteger gaugeMissingRefreshWhenExpired = new AtomicInteger();

    @PostConstruct
    void registerGauges() {
        Gauge.builder("douyin.oauth.tokens.expired", gaugeExpired, AtomicInteger::get)
                .description("Count of Douyin OAuth rows where access token is expired (last probe)")
                .register(meterRegistry);
        Gauge.builder("douyin.oauth.tokens.expiring_soon", gaugeExpiringSoon, AtomicInteger::get)
                .description("Count of Douyin OAuth rows expiring within 30 minutes (last probe)")
                .register(meterRegistry);
        Gauge.builder("douyin.oauth.tokens.ok", gaugeOk, AtomicInteger::get)
                .description("Count of Douyin OAuth rows not expired and not in soon window (last probe)")
                .register(meterRegistry);
        Gauge.builder("douyin.oauth.tokens.expired_without_refresh", gaugeMissingRefreshWhenExpired, AtomicInteger::get)
                .description("Expired Douyin OAuth rows with blank refresh_token (last probe)")
                .register(meterRegistry);
    }

    @Scheduled(fixedRateString = "${douyin.api.token-health.fixed-rate-ms:300000}",
            initialDelayString = "${douyin.api.token-health.initial-delay-ms:120000}")
    public void probe() {
        List<OAuthToken> tokens = oauthTokenRepository.findAllByProviderAndDeleted(PROVIDER_DOUYIN, 0);
        int expired = 0;
        int soon = 0;
        int ok = 0;
        int expiredNoRefresh = 0;
        for (OAuthToken t : tokens) {
            if (t.isExpired()) {
                expired++;
                if (t.getRefreshToken() == null || t.getRefreshToken().isBlank()) {
                    expiredNoRefresh++;
                }
            } else if (t.isExpiringSoon()) {
                soon++;
            } else {
                ok++;
            }
        }
        gaugeExpired.set(expired);
        gaugeExpiringSoon.set(soon);
        gaugeOk.set(ok);
        gaugeMissingRefreshWhenExpired.set(expiredNoRefresh);
        meterRegistry.counter("douyin.oauth.probe.runs").increment();
        if (expiredNoRefresh > 0) {
            log.warn("Douyin OAuth：{} 条 token 已过期且无 refresh_token，采集将跳过对应场次（若开启 skip-when-token-not-refreshable）",
                    expiredNoRefresh);
        } else {
            log.debug("Douyin OAuth 探活完成：总数={}, 未过期={}, 将过期={}, 已过期={}",
                    tokens.size(), ok, soon, expired);
        }
    }
}
