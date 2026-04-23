package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
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
class DashboardGmvServiceProductSummaryTest {

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
    void nullUser_returnsEmptyProductRows() {
        Map<String, Object> m = service.getProductGmvSummary(null, 90);
        assertThat((List<?>) m.get("rows")).isEmpty();
    }

    @Test
    void aggregatesProductsAcrossSessions() {
        LiveSession s = new LiveSession();
        s.setId(10L);
        s.setCreateTime(new Timestamp(System.currentTimeMillis()));
        when(sessionRepository.findByUserIdAndDeleted(eq(1L), eq(0), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 200), 1));

        LiveProduct lp = new LiveProduct();
        lp.setProductId(101L);
        lp.setProductName("面霜A");
        lp.setRevenue(new BigDecimal("999.50"));
        when(productRepository.findBySessionIdAndDeleted(10L, 0)).thenReturn(List.of(lp));

        Map<String, Object> m = service.getProductGmvSummary(1L, 90);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) m.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("productId")).isEqualTo(101L);
        assertThat(rows.get(0).get("productName")).isEqualTo("面霜A");
        assertThat(rows.get(0).get("totalGmv")).isEqualTo(new BigDecimal("999.50"));
    }
}
