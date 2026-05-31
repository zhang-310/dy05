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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardGmvServiceCoreTest {

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
    void getUnifiedKpi_aggregatesViewersAndTodayGmv() {
        LiveSession first = session(10L, "首场,新品", "brand", 1, 120, 12L);
        LiveSession second = session(11L, "第二场", null, 2, 80, 8L);
        mockSessions(7L, first, second);
        when(productRepository.sumRevenueBySessionId(10L)).thenReturn(new BigDecimal("199.50"));
        when(productRepository.sumRevenueBySessionId(11L)).thenReturn(null);

        Map<String, Object> result = service.getUnifiedKpi(7L, 30);

        @SuppressWarnings("unchecked")
        Map<String, Object> traffic = (Map<String, Object>) result.get("traffic");
        @SuppressWarnings("unchecked")
        Map<String, Object> revenue = (Map<String, Object>) result.get("revenue");
        assertThat(traffic.get("liveViewerSum")).isEqualTo(200L);
        assertThat(revenue.get("todayGmv")).isEqualTo(new BigDecimal("199.50"));
        assertThat(revenue.get("avgOrderValueToday")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getLiveFormatGmv_defaultsMissingFormatAndZeroRevenue() {
        LiveSession session = session(20L, "未分类场次", null, 1, 0, 0L);
        mockSessions(7L, session);
        when(productRepository.sumRevenueBySessionId(20L)).thenReturn(null);

        Map<String, Object> result = service.getLiveFormatGmv(7L, 30);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.get("liveFormat")).isEqualTo("普通直播");
                    assertThat(row.get("sessionCount")).isEqualTo(1L);
                    assertThat(row.get("totalGmv")).isEqualTo(BigDecimal.ZERO);
                });
    }

    @Test
    void getProductGmvSummary_groupsProductsAndSkipsMissingProductId() {
        LiveSession session = session(30L, "商品场", "brand", 1, 0, 0L);
        mockSessions(7L, session);
        LiveProduct first = product(101L, "面霜", "99.00");
        LiveProduct second = product(101L, "面霜旧名", "1.00");
        LiveProduct missingId = product(null, "无 ID", "500.00");
        when(productRepository.findBySessionIdAndDeleted(30L, 0))
                .thenReturn(List.of(first, second, missingId));

        Map<String, Object> result = service.getProductGmvSummary(7L, 30);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.get("productId")).isEqualTo(101L);
                    assertThat(row.get("productName")).isEqualTo("面霜");
                    assertThat(row.get("sessionCount")).isEqualTo(2L);
                    assertThat(row.get("totalGmv")).isEqualTo(new BigDecimal("100.00"));
                });
    }

    @Test
    void getCockpitPreview_filtersStatusAndBuildsRows() {
        LiveSession matched = session(40L, "可导出场", "brand", 2, 100, 10L);
        matched.setStartTime(Timestamp.valueOf(LocalDate.now().atStartOfDay()));
        matched.setEndTime(new Timestamp(System.currentTimeMillis()));
        LiveSession filtered = session(41L, "过滤场", "brand", 1, 100, 10L);
        mockSessions(7L, matched, filtered);
        when(productRepository.sumRevenueBySessionId(40L)).thenReturn(new BigDecimal("300.00"));
        when(productRepository.countBySessionId(40L)).thenReturn(3L);

        Map<String, Object> result = service.getCockpitPreview(7L, Map.of(
                "dateFrom", LocalDate.now().minusDays(1).toString(),
                "sessionStatus", 2
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertThat(result.get("rowCount")).isEqualTo(1);
        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.get("sessionId")).isEqualTo(40L);
                    assertThat(row.get("gmv")).isEqualTo(new BigDecimal("300.00"));
                    assertThat(row.get("productLineCount")).isEqualTo(3L);
                });
    }

    @Test
    void getConversionFunnel_calculatesRatesAgainstViewerTotal() {
        LiveSession first = session(50L, "漏斗一", "brand", 2, 100, 25L);
        LiveSession second = session(51L, "漏斗二", "brand", 2, 50, 5L);
        mockSessions(7L, first, second);
        when(productRepository.countBySessionId(50L)).thenReturn(3L);
        when(productRepository.countBySessionId(51L)).thenReturn(0L);

        Map<String, Object> result = service.getConversionFunnel(7L, 30);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.get("steps");
        assertThat(steps).extracting(step -> step.get("name"))
                .containsExactly("观看", "点赞", "进入商品");
        assertThat(steps.get(0).get("rate")).isEqualTo(100.0);
        assertThat(steps.get(1).get("value")).isEqualTo(30L);
        assertThat(steps.get(1).get("rate")).isEqualTo(20.0);
        assertThat(steps.get(2).get("rate")).isEqualTo(2.0);
    }

    @Test
    void exportCockpitCsv_escapesCommaQuoteAndNewlineInTitle() {
        LiveSession session = session(60L, "新品,\"强卖点\"\n专场", "brand", 2, 10, 1L);
        mockSessions(7L, session);
        when(productRepository.sumRevenueBySessionId(60L)).thenReturn(new BigDecimal("12.30"));
        when(productRepository.countBySessionId(60L)).thenReturn(1L);

        Map<String, Object> result = service.exportCockpitCsv(7L, Map.of("sessionStatus", 2));

        assertThat(result.get("rowCount")).isEqualTo(1);
        assertThat(result.get("filename").toString()).startsWith("cockpit_");
        assertThat(result.get("csv").toString())
                .contains("\"新品,\"\"强卖点\"\"\n专场\"")
                .contains("12.30,1");
    }

    private void mockSessions(Long userId, LiveSession... sessions) {
        when(sessionRepository.findByUserIdAndDeleted(eq(userId), eq(0), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sessions), PageRequest.of(0, 200), sessions.length));
    }

    private LiveSession session(Long id, String title, String type, Integer status, Integer viewers, Long likes) {
        LiveSession session = new LiveSession();
        session.setId(id);
        session.setUserId(7L);
        session.setAccountId(1000L + id);
        session.setLiveTitle(title);
        session.setSessionType(type);
        session.setStatus(status);
        session.setViewers(viewers);
        session.setLikes(likes);
        session.setCreateTime(new Timestamp(System.currentTimeMillis()));
        return session;
    }

    private LiveProduct product(Long productId, String name, String revenue) {
        LiveProduct product = new LiveProduct();
        product.setProductId(productId);
        product.setProductName(name);
        product.setRevenue(new BigDecimal(revenue));
        return product;
    }
}
