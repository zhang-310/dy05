package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.module.ai.gateway.AiCommercialFacade;
import cn.gaifan.douyinOperations.module.ai.gateway.AiGatewayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentFunctionCallingCommercialTest {

    @Mock private AiGatewayService aiGatewayService;
    @InjectMocks
    private AgentFunctionCallingService agentFunctionCallingService;

    @Test
    void chargeCommercialRoundDelegatesToGateway() {
        when(aiGatewayService.chargeCommercialRound(eq(FeatureCode.AI_CHAT), anyString()))
                .thenReturn(new AiCommercialFacade.CommercialChargeResult(false, "积分不足", "r1", "t1"));
        AiCommercialFacade.CommercialChargeResult r =
                aiGatewayService.chargeCommercialRound(FeatureCode.AI_CHAT, "agent-1-conv-2-r1");
        assertFalse(r.allowed());
        assertEquals("积分不足", r.reason());
    }
}
