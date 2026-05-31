package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Benchmark analyze commercial mapping")
class BenchmarkAnalysisChargeTest {

    @Test
    void viralAnalysisFeatureMapped() {
        var cmd = CommercialProductChargeService.CommercialProductChargeCommand.of(
                ProductCode.VIDEO_INSIGHT,
                FeatureCode.VIDEO_VIRAL_ANALYSIS,
                "benchmark analyze videoId=1",
                DeliveryProduct.VIDEO_INSIGHT
        );
        assertThat(cmd.featureCode()).isEqualTo(FeatureCode.VIDEO_VIRAL_ANALYSIS);
        assertThat(cmd.productCode()).isEqualTo(ProductCode.VIDEO_INSIGHT);
    }
}
