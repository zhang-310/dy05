package cn.gaifan.douyinOperations.module.platform.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogServiceImplEdgeCaseTest {

    private ProductCatalogServiceImpl newService() {
        var s = new ProductCatalogServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(s, "jdbcTemplate", null);
        s.init();
        return s;
    }

    @Test
    void findProductIsCaseSensitive() {
        var s = newService();
        assertNull(s.findProduct("DOUYIN-OPS"));
        assertNotNull(s.findProduct("douyin-ops"));
    }

    @Test
    void findFeatureReturnsNullForUnknown() {
        var s = newService();
        assertNull(s.findFeature("nonexistent.feature"));
    }

    @Test
    void listProductsIsNotEmpty() {
        var s = newService();
        assertFalse(s.listProducts().isEmpty());
    }

    @Test
    void noNullFeatures() {
        var s = newService();
        for (var p : s.listProducts()) {
            assertNotNull(p.features());
            for (var f : p.features()) {
                assertNotNull(f.code());
                assertNotNull(f.name());
            }
        }
    }
}
