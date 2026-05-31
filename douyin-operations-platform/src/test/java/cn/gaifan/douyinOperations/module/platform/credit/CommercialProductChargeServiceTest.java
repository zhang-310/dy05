package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.auth.EntitlementDecisionCode;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommercialProductChargeServiceTest {

    @Mock private PlatformEntitlementService entitlementService;
    @Mock private CommercialCreditHelper commercialCreditHelper;
    @Mock private DeliveryLedgerService deliveryLedgerService;

    private CommercialProductChargeService service;

    @BeforeEach
    void setUp() {
        service = new CommercialProductChargeService(
                entitlementService, commercialCreditHelper, deliveryLedgerService);
    }

    @Test
    void chargeDeniedThrowsEntitlementDenied() {
        when(entitlementService.check(any(EntitlementCheckRequest.class)))
                .thenReturn(new CommercialEntitlementDecision(
                        false, EntitlementDecisionCode.NO_ENTITLEMENT,
                        ProductCode.VIDEO_INSIGHT, FeatureCode.VIDEO_BREAKDOWN,
                        "no entitlement", null, null, false));
        var cmd = CommercialProductChargeService.CommercialProductChargeCommand.of(
                ProductCode.VIDEO_INSIGHT, FeatureCode.VIDEO_BREAKDOWN, "test", DeliveryProduct.VIDEO_INSIGHT);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.charge(cmd));
        assertEquals(ErrorCode.ENTITLEMENT_DENIED, ex.getCode());
        verifyNoInteractions(commercialCreditHelper);
    }

    @Test
    void chargeGrantedConsumesAndRecordsDelivery() {
        when(entitlementService.check(any(EntitlementCheckRequest.class)))
                .thenReturn(new CommercialEntitlementDecision(
                        true, EntitlementDecisionCode.ALLOW,
                        ProductCode.VIDEO_INSIGHT, FeatureCode.VIDEO_BREAKDOWN,
                        "ok", null, null, false));
        var cmd = CommercialProductChargeService.CommercialProductChargeCommand.of(
                ProductCode.VIDEO_INSIGHT, FeatureCode.VIDEO_BREAKDOWN, "拆解", DeliveryProduct.VIDEO_INSIGHT);
        service.charge(cmd);
        ArgumentCaptor<CreditConsumeRequest> consumeCaptor = ArgumentCaptor.forClass(CreditConsumeRequest.class);
        verify(commercialCreditHelper).consume(consumeCaptor.capture());
        assertEquals(ProductCode.VIDEO_INSIGHT, consumeCaptor.getValue().productCode());
        verify(deliveryLedgerService).recordDelivery(eq(DeliveryProduct.VIDEO_INSIGHT), anyString(), anyString(), eq("STARTED"));
    }
}
