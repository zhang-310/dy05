package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SvScriptServiceCommercialTest {

    @Mock private CommercialProductChargeService commercialProductChargeService;
    @InjectMocks
    private SvScriptServiceImpl svScriptService;

    @Test
    void generateInvokesCharge() {
        ReflectionTestUtils.setField(svScriptService, "commercialProductChargeService", commercialProductChargeService);
        try {
            svScriptService.generate("daily", "theme", null, null, null, 60, 1L);
        } catch (Exception ignored) {
            // LLM/依赖未 mock 时可能失败，仅验证扣费调用
        }
        verify(commercialProductChargeService).charge(
                org.mockito.ArgumentMatchers.argThat(cmd ->
                        ProductCode.SHORTVIDEO_MAKER.equals(cmd.productCode())
                                && FeatureCode.SHORTVIDEO_SCRIPT_GENERATE.equals(cmd.featureCode())
                                && DeliveryProduct.SHORTVIDEO_MAKER.equals(cmd.deliveryProduct())));
    }
}
