package cn.gaifan.douyinOperations.module.douyinapi.controller;

import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Douyin OAuth callback charge mapping")
class DouyinOAuthCallbackChargeTest {

    @Test
    void oauthAccountMgmtUsesDouyinOpsProduct() {
        var cmd = CommercialProductChargeService.CommercialProductChargeCommand.of(
                ProductCode.DOUYIN_OPS,
                FeatureCode.DOUYIN_ACCOUNT_MGMT,
                "抖音 OAuth 授权 userId=1",
                DeliveryProduct.DOUYIN_OPS
        );
        assertThat(cmd.productCode()).isEqualTo(ProductCode.DOUYIN_OPS);
        assertThat(cmd.featureCode()).isEqualTo(FeatureCode.DOUYIN_ACCOUNT_MGMT);
        assertThat(cmd.deliveryProduct()).isEqualTo(DeliveryProduct.DOUYIN_OPS);
    }
}
