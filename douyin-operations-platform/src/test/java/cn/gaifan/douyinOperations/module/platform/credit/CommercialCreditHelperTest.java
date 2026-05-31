package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommercialCreditHelper 单测")
class CommercialCreditHelperTest {

    @Mock
    private CreditLedgerService creditLedgerService;

    @InjectMocks
    private CommercialCreditHelper commercialCreditHelper;

    @Test
    @DisplayName("enforce=false 时 bypass 消费")
    void consume_bypassWhenNotEnforced() {
        ReflectionTestUtils.setField(commercialCreditHelper, "creditEnforce", false);
        CreditConsumeResult result = commercialCreditHelper.consume(sampleConsume());
        assertTrue(result.allowed());
        verify(creditLedgerService, never()).consume(any());
    }

    @Test
    @DisplayName("enforce=true 时委托 CreditLedgerService")
    void consume_delegatesWhenEnforced() {
        ReflectionTestUtils.setField(commercialCreditHelper, "creditEnforce", true);
        when(creditLedgerService.consume(any())).thenReturn(new CreditConsumeResult(
                "tx-1", true, "ALLOW", "ok", null, BigDecimal.ONE, BigDecimal.TEN, null, null
        ));
        CreditConsumeResult result = commercialCreditHelper.consume(sampleConsume());
        assertTrue(result.allowed());
        verify(creditLedgerService).consume(any());
    }

    private static CreditConsumeRequest sampleConsume() {
        return new CreditConsumeRequest(
                "demo-tenant",
                "demo-user",
                null,
                "video-insight",
                "video-insight.breakdown",
                "WEB",
                BigDecimal.ONE,
                "standard",
                "trace-1",
                "test"
        );
    }
}
