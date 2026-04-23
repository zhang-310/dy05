package cn.gaifan.douyinOperations.module.live.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * L-3：抖音开放平台若提供场次级「完播/观看完成率」等字段，由实现类接入；缺省时返回 empty，回退既有监控双点推算。
 */
public interface LiveOfficialCompletionRateProvider {

    Optional<BigDecimal> tryGetSessionCompletionPercent(Long sessionId);
}
