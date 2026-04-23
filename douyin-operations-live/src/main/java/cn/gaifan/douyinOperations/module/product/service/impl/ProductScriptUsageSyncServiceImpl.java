package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.product.entity.ProductScriptUsage;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptUsageRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptUsageSyncService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 排品与话术 usage 表同步（轻量记录 applied_time，效果分等由复盘回写）
 */
@Service
public class ProductScriptUsageSyncServiceImpl implements ProductScriptUsageSyncService {

    private static final Logger log = LoggerFactory.getLogger(ProductScriptUsageSyncServiceImpl.class);

    @Resource
    private ProductScriptUsageRepository usageRepository;

    @Resource
    private DyProductScriptRepository dyProductScriptRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLiveProductScriptBinding(Long liveProductId, Long sessionId, Long productScriptId, Long userId) {
        if (sessionId == null || productScriptId == null || productScriptId <= 0) {
            return;
        }
        if (!dyProductScriptRepository.findById(productScriptId).isPresent()) {
            return;
        }
        if (liveProductId != null && liveProductId > 0) {
            if (usageRepository.existsByLiveProductIdAndProductScriptIdAndDeleted(liveProductId, productScriptId, 0)) {
                log.debug("skip duplicate usage liveProductId={} productScriptId={}", liveProductId, productScriptId);
                return;
            }
        } else if (usageRepository.existsBySessionIdAndProductScriptIdAndDeleted(sessionId, productScriptId, 0)) {
            log.debug("skip duplicate usage sessionId={} productScriptId={} (no liveProductId)", sessionId, productScriptId);
            return;
        }
        ProductScriptUsage u = new ProductScriptUsage();
        u.setProductScriptId(productScriptId);
        u.setSessionId(sessionId);
        u.setLiveProductId(liveProductId != null && liveProductId > 0 ? liveProductId : null);
        u.setLiveScriptId(null);
        usageRepository.save(u);
        log.debug("recordLiveProductScriptBinding liveProductId={} sessionId={} productScriptId={} userId={}",
                liveProductId, sessionId, productScriptId, userId);
    }
}
