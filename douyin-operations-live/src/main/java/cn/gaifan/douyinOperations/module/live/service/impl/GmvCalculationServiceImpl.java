package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.service.GmvCalculationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GMV 统一计算服务实现。
 * <p>所有 GMV 均基于 {@code live_product.revenue}（排品行），详见接口注释。
 */
@Service
@Transactional(readOnly = true)
public class GmvCalculationServiceImpl implements GmvCalculationService {

    @Resource
    private LiveProductRepository liveProductRepository;

    @Override
    public BigDecimal sessionGmv(Long sessionId) {
        if (sessionId == null) return BigDecimal.ZERO;
        BigDecimal gmv = liveProductRepository.sumRevenueBySessionId(sessionId);
        return gmv != null ? gmv : BigDecimal.ZERO;
    }

    @Override
    public Map<Long, BigDecimal> batchSessionGmv(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return Map.of();
        List<Object[]> rows = liveProductRepository.sumRevenueGroupBySessionIdIn(sessionIds);
        Map<Long, BigDecimal> result = new HashMap<>(sessionIds.size());
        // 先初始化所有场次为 ZERO（保证无商品的场次也有条目）
        for (Long id : sessionIds) result.put(id, BigDecimal.ZERO);
        for (Object[] row : rows) {
            if (row[0] instanceof Long sid && row[1] instanceof BigDecimal gmv) {
                result.put(sid, gmv);
            }
        }
        return result;
    }

    @Override
    public BigDecimal todayGmv(Long orgId) {
        Timestamp todayStart = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp tomorrowStart = Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay());
        return gmvBetween(orgId, todayStart, tomorrowStart);
    }

    @Override
    public BigDecimal gmvBetween(Long orgId, Timestamp startTime, Timestamp endTime) {
        if (startTime == null || endTime == null) return BigDecimal.ZERO;
        BigDecimal gmv = liveProductRepository.sumRevenueBetween(startTime, endTime);
        return gmv != null ? gmv : BigDecimal.ZERO;
    }
}
