package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.product.ProductCatalogProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductCatalogServiceImpl 完整测试
 */
class ProductCatalogServiceImplCompleteTest {

    private ProductCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "jdbcTemplate", null);
        service.init();
    }

    @Test
    void allSixProductsHaveNames() {
        for (var p : service.listProducts()) {
            assertNotNull(p.name(), p.code() + " should have name");
            assertFalse(p.name().isEmpty());
        }
    }

    @Test
    void douyinOpsHasAtLeastThreeFeatures() {
        var p = service.findProduct("douyin-ops");
        assertNotNull(p);
        assertTrue(p.features().size() >= 3, "douyin-ops should have >= 3 features");
    }

    @Test
    void allFeatureCodesUseDotNotation() {
        for (var p : service.listProducts()) {
            for (var f : p.features()) {
                assertTrue(f.code().contains("."),
                        "Feature " + f.code() + " should use dot notation");
            }
        }
    }

    @Test
    void previewProductsExist() {
        var hasPreview = service.listProducts().stream()
                .anyMatch(p -> "preview".equals(p.stage()));
        assertTrue(hasPreview, "Should have preview-stage products");
    }

    @Test
    void launchedProductsExist() {
        var hasLaunched = service.listProducts().stream()
                .anyMatch(p -> "launched".equals(p.stage()));
        assertTrue(hasLaunched, "Should have launched products");
    }
}
