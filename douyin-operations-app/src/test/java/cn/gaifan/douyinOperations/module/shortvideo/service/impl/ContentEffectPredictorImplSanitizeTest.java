package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentEffectPredictorImplSanitizeTest {

    @Test
    void sanitizeEffectPredictionLists_trimsArrays() throws Exception {
        Method m = ContentEffectPredictorImpl.class.getDeclaredMethod(
                "sanitizeEffectPredictionLists", Map.class, int.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) m.invoke(null,
                Map.of("strengths", List.of("a", "b", "c", "d"), "viewRange", Map.of("min", 1, "max", 2)),
                2);
        assertEquals(List.of("a", "b"), out.get("strengths"));
        assertEquals(Map.of("min", 1, "max", 2), out.get("viewRange"));
    }
}
