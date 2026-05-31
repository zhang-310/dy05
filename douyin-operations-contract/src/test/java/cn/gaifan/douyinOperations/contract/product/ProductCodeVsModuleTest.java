package cn.gaifan.douyinOperations.contract.product;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductCodeVsModuleTest {

    @Test
    void allProductsHaveValidCode() {
        String[] codes = {ProductCode.DOUYIN_OPS, ProductCode.VIDEO_INSIGHT,
                ProductCode.KNOWLEDGE_BASE, ProductCode.DRAMA_AI,
                ProductCode.DIGITAL_HUMAN, ProductCode.PHOTO_AVATAR_VIDEO,
                ProductCode.SHORTVIDEO_MAKER};
        for (String c : codes) {
            assertNotNull(c);
            assertTrue(c.contains("-"), "Code should use kebab-case: " + c);
        }
    }

    @Test
    void productCountIsSeven() {
        var fields = ProductCode.class.getDeclaredFields();
        var count = java.util.Arrays.stream(fields)
                .filter(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertEquals(7, count, "7 products should be defined");
    }
}
