package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Benchmark 分析扣费独立事务：分析流水线失败时不回滚已扣积分。
 */
@Component
public class BenchmarkCommercialChargeHelper {

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void chargeForAnalyze(Long benchmarkVideoId) {
        if (commercialProductChargeService == null) {
            return;
        }
        commercialProductChargeService.charge(
                CommercialProductChargeService.CommercialProductChargeCommand.of(
                        ProductCode.VIDEO_INSIGHT,
                        FeatureCode.VIDEO_VIRAL_ANALYSIS,
                        "benchmark analyze videoId=" + benchmarkVideoId,
                        DeliveryProduct.VIDEO_INSIGHT
                ));
    }
}
