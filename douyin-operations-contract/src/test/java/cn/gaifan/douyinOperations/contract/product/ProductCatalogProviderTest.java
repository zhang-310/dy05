package cn.gaifan.douyinOperations.contract.product;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogProviderTest {

    @Test
    void productSummaryRecord() {
        var ps = new ProductCatalogProvider.ProductSummary("test", "测试产品", "preview", true, java.util.List.of());
        assertEquals("test", ps.code());
        assertEquals("测试产品", ps.name());
        assertTrue(ps.enabled());
        assertTrue(ps.features().isEmpty());
    }

    @Test
    void featureSummaryRecord() {
        var fs = new ProductCatalogProvider.FeatureSummary("f1", "功能1", "call", 100L);
        assertEquals("f1", fs.code());
        assertEquals(100L, fs.monthlyLimit());
    }
}
