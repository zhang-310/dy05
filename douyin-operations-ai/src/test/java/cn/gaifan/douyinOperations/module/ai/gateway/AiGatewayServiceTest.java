package cn.gaifan.douyinOperations.module.ai.gateway;

import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.auth.EntitlementDecisionCode;
import cn.gaifan.douyinOperations.module.ai.config.AiGatewayProperties;
import cn.gaifan.douyinOperations.module.ai.provider.AiProviderRouter;
import cn.gaifan.douyinOperations.module.ai.provider.MockAiProvider;
import cn.gaifan.douyinOperations.module.platform.ai.AiInvocationLedgerService;
import cn.gaifan.douyinOperations.module.platform.ai.AiInvocationRecord;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGatewayServiceTest {

    @Mock private PlatformEntitlementService platformEntitlementService;
    @Mock private AiInvocationLedgerService aiInvocationLedgerService;
    @Mock private AiProviderRouter aiProviderRouter;
    @Mock private AiCommercialFacade aiCommercialFacade;
    private AiGatewayService service;

    @BeforeEach
    void setUp() {
        service = new AiGatewayService();
        AiGatewayProperties props = new AiGatewayProperties();
        props.setRetailMarkup(new BigDecimal("1.5"));
        ReflectionTestUtils.setField(service, "platformEntitlementService", platformEntitlementService);
        ReflectionTestUtils.setField(service, "aiProviderRouter", aiProviderRouter);
        ReflectionTestUtils.setField(service, "aiGatewayProperties", props);
        ReflectionTestUtils.setField(service, "aiInvocationLedgerService", aiInvocationLedgerService);
        ReflectionTestUtils.setField(service, "aiCommercialFacade", aiCommercialFacade);
        when(aiInvocationLedgerService.newRequestId()).thenReturn("ai_req_test");
        lenient().when(aiProviderRouter.resolve(anyString())).thenReturn(new MockAiProvider());
    }

    @Test
    void callWhenDenied() {
        when(platformEntitlementService.check(any(EntitlementCheckRequest.class)))
                .thenReturn(new CommercialEntitlementDecision(
                        false, EntitlementDecisionCode.NO_ENTITLEMENT, "douyin-ops", "ai.chat",
                        "quota", null, null, false));
        var result = service.call("ai.chat", "hello");
        assertFalse(result.success());
        assertEquals("quota", result.error());
        assertEquals("ai_req_test", result.requestId());
        verify(aiInvocationLedgerService).record(any(AiInvocationRecord.class));
    }

    @Test
    void callWhenGranted() {
        when(platformEntitlementService.check(any(EntitlementCheckRequest.class)))
                .thenReturn(new CommercialEntitlementDecision(
                        true, EntitlementDecisionCode.ALLOW, "douyin-ops", "ai.chat",
                        "ok", null, null, false));
        var result = service.call("ai.chat", "hello world");
        assertTrue(result.success());
        assertTrue(result.output().contains("[MOCK AI"));
        verify(aiInvocationLedgerService).record(argThat(r -> r != null && r.success()));
    }
}
