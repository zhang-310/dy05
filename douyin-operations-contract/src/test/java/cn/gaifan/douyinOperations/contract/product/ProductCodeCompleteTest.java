package cn.gaifan.douyinOperations.contract.product;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductCodeCompleteTest {

    @Test
    void sevenProductsDefined() {
        var fields = ProductCode.class.getDeclaredFields();
        // All constants should be public static final String
        var codes = java.util.Arrays.stream(fields)
                .filter(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .count();
        assertTrue(codes >= 7);
    }

    @Test
    void noDuplicateCodes() {
        var codes = java.util.Arrays.stream(ProductCode.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .map(f -> { try { return (String) f.get(null); } catch (Exception e) { return ""; }})
                .filter(s -> !s.isEmpty())
                .distinct()
                .count();
        var total = java.util.Arrays.stream(ProductCode.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .count();
        assertEquals(total, codes, "All product codes must be unique");
    }
}
