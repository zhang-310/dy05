package cn.gaifan.douyinOperations.module.shortvideo.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * D-4：POST 与 SSE 共用 {@code extractCliffhangerCarryover}，与全剧生成服务同一入口。
 */
class DramaControllerExtractCarryoverTest {

    @Test
    void extractCliffhangerCarryover_readsBody() throws Exception {
        Method m = DramaController.class.getDeclaredMethod("extractCliffhangerCarryover", Map.class);
        m.setAccessible(true);
        assertEquals("strong", m.invoke(null, Map.of("cliffhangerCarryover", "strong")));
        assertEquals("off", m.invoke(null, Map.of("cliffhangerCarryover", "off")));
    }

    @Test
    void extractCliffhangerCarryover_nullBody() throws Exception {
        Method m = DramaController.class.getDeclaredMethod("extractCliffhangerCarryover", Map.class);
        m.setAccessible(true);
        assertNull(m.invoke(null, new Object[] { null }));
    }
}
