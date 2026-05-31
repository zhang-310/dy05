package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.product.ProductCatalogProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductCatalogServiceImpl 单元测试
 */
class ProductCatalogServiceImplTest {

    private ProductCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "jdbcTemplate", null);
        service.init();
    }

    @Test
    void allSixProducts() {
        var products = service.listProducts();
        assertTrue(products.size() >= 6, "catalog should list at least six Gaifan products");
    }

    @Test
    void findExistingProduct() {
        var p = service.findProduct("douyin-ops");
        assertNotNull(p);
        assertEquals("抖音运营", p.name());
    }

    @Test
    void findNonexistentProduct() {
        assertNull(service.findProduct("nonexistent"));
    }

    @Test
    void findFeature() {
        var f = service.findFeature("douyin-ops.account-mgmt");
        assertNotNull(f);
        assertEquals("账号管理", f.name());
    }

    @Test
    void productsHaveFeatures() {
        var p = service.findProduct("douyin-ops");
        assertTrue(p.features().size() >= 3);
    }

    @Test
    void launchedProductsAreEnabled() {
        var p = service.findProduct("douyin-ops");
        assertTrue(p.enabled());
    }
}
