package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.contract.credit.CreditAccountSummary;
import cn.gaifan.douyinOperations.contract.credit.CreditQuoteRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditQuoteResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditLedgerService 单测")
class CreditLedgerServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CreditLedgerService creditLedgerService;

    @Test
    @DisplayName("demo-tenant 无库记录时使用演示余额")
    void account_demoTenant_fallback() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of());
        CreditAccountSummary account = creditLedgerService.account("demo-tenant");
        assertTrue(account.availableCredits().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("quote 对 video-insight 返回可报价结果")
    void quote_videoInsight_sufficient() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of());
        CreditQuoteResult quote = creditLedgerService.quote(new CreditQuoteRequest(
                "demo-tenant",
                "demo-user",
                null,
                "video-insight",
                "video-insight.breakdown",
                "WEB",
                BigDecimal.ONE,
                "standard",
                "trace-test"
        ));
        assertTrue(quote.sufficient());
        assertTrue(quote.totalCredits().compareTo(BigDecimal.ZERO) >= 0);
    }
}
