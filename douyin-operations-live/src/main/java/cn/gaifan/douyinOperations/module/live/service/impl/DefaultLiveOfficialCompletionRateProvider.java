package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.service.LiveOfficialCompletionRateProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/** 默认无官方字段；可替换为对接抖音 API 的实现（见 application.yml / 文档）。 */
@Component
public class DefaultLiveOfficialCompletionRateProvider implements LiveOfficialCompletionRateProvider {

    @Override
    public Optional<BigDecimal> tryGetSessionCompletionPercent(Long sessionId) {
        return Optional.empty();
    }
}
