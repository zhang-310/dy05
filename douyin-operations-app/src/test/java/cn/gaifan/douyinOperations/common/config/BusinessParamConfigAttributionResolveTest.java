package cn.gaifan.douyinOperations.common.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessParamConfigAttributionResolveTest {

    @Test
    void resolveUsesCategoryMapIgnoreCase() {
        BusinessParamConfig.Attribution a = new BusinessParamConfig.Attribution();
        a.setWindowSeconds(30);
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("护肤", 45);
        m.put("skincare", 40);
        a.setWindowSecondsByCategory(m);
        assertEquals(45, a.resolveWindowSeconds(" 护肤 "));
        assertEquals(40, a.resolveWindowSeconds("SKINCARE"));
    }

    @Test
    void resolveFallsBackToDefaultWhenUnknownCategory() {
        BusinessParamConfig.Attribution a = new BusinessParamConfig.Attribution();
        a.setWindowSeconds(28);
        a.setWindowSecondsByCategory(Map.of("护肤", 99));
        assertEquals(28, a.resolveWindowSeconds("零食"));
    }

    @Test
    void resolveClampsExtremeValues() {
        BusinessParamConfig.Attribution a = new BusinessParamConfig.Attribution();
        a.setWindowSeconds(9000);
        assertEquals(600, a.resolveWindowSeconds(null));
        a.setWindowSecondsByCategory(Map.of("x", 2));
        assertEquals(5, a.resolveWindowSeconds("x"));
    }
}
