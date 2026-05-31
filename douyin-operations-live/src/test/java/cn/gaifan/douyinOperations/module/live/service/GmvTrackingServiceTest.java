package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.contract.payment.SessionGmvSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmvTrackingServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @InjectMocks private GmvTrackingService service;

    @Test
    void getSessionGmvReturnsZeroOnError() {
        when(jdbcTemplate.queryForMap(anyString(), anyLong()))
                .thenThrow(new RuntimeException("table missing"));
        var result = service.getSessionGmv(1L);
        assertEquals(0L, result.netGmv());
    }

    @Test
    void getTopSessionsReturnsEmptyOnError() {
        when(jdbcTemplate.query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class),
                anyInt()))
                .thenThrow(new RuntimeException("table missing"));
        var result = service.getTopSessionsByGmv(5);
        assertTrue(result.isEmpty());
    }

    @Test
    void completionRateEstimatedOnError() {
        when(jdbcTemplate.queryForMap(anyString(), anyLong()))
                .thenThrow(new RuntimeException("table missing"));
        var result = service.getCompletionRate(1L);
        assertEquals(0.85, result.fullWatchRatio(), 0.01);
        assertEquals("estimated", result.dataSource());
    }
}
