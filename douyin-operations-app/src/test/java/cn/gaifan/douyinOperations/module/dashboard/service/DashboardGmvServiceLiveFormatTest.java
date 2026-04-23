package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardGmvServiceLiveFormatTest {

    @Mock
    private LiveSessionRepository sessionRepository;

    @Mock
    private LiveProductRepository productRepository;

    private DashboardGmvService service;

    @BeforeEach
    void setUp() {
        service = new DashboardGmvService();
        ReflectionTestUtils.setField(service, "sessionRepository", sessionRepository);
        ReflectionTestUtils.setField(service, "productRepository", productRepository);
    }

    @Test
    void nullUserId_returnsEmptyRows() {
        Map<String, Object> m = service.getLiveFormatGmv(null, 90);
        assertThat((List<?>) m.get("rows")).isEmpty();
    }

    @Test
    void mapsSessionsByFormat() {
        LiveSession s = new LiveSession();
        s.setId(10L);
        s.setSessionType("heavy_paid_category");
        s.setCreateTime(new Timestamp(System.currentTimeMillis()));
        when(sessionRepository.findByUserIdAndDeleted(eq(1L), eq(0), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 200), 1));
        when(productRepository.sumRevenueBySessionId(10L)).thenReturn(new BigDecimal("100.00"));

        Map<String, Object> m = service.getLiveFormatGmv(1L, 90);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) m.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("liveFormat")).isEqualTo("heavy_paid_category");
        assertThat(rows.get(0).get("totalGmv")).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void profitMatrixPreview_usesEstimatedMargin() {
        LiveSession s = new LiveSession();
        s.setId(10L);
        s.setSessionType("warehouse");
        s.setCreateTime(new Timestamp(System.currentTimeMillis()));
        when(sessionRepository.findByUserIdAndDeleted(eq(1L), eq(0), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 200), 1));
        when(productRepository.sumRevenueBySessionId(10L)).thenReturn(new BigDecimal("200.00"));

        Map<String, Object> m = service.getProfitMatrixPreview(1L, 90);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) m.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("estimatedMarginRate")).isEqualTo(new BigDecimal("0.25"));
    }
}
