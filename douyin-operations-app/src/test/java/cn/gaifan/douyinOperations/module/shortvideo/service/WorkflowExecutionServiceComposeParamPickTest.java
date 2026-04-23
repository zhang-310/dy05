package cn.gaifan.douyinOperations.module.shortvideo.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * V-3：工作流 compose 从 merge 后的 materials 读取 BGM/转场等与 auto-compose 对齐。
 */
class WorkflowExecutionServiceComposeParamPickTest {

    @Test
    void workflowParamPick_mergedWins() throws Exception {
        Method pick = WorkflowExecutionService.class.getDeclaredMethod(
                "workflowParamPick", Map.class, Map.class, String.class);
        pick.setAccessible(true);
        Map<String, Object> merged = new HashMap<>();
        merged.put("bgmUrl", "http://merged");
        Map<String, Object> params = Map.of("bgmUrl", "http://top");
        assertEquals("http://merged", pick.invoke(null, merged, params, "bgmUrl"));
    }

    @Test
    void workflowParamPick_fallsBackToParams() throws Exception {
        Method pick = WorkflowExecutionService.class.getDeclaredMethod(
                "workflowParamPick", Map.class, Map.class, String.class);
        pick.setAccessible(true);
        Map<String, Object> params = Map.of("transition", "wipeleft");
        assertEquals("wipeleft", pick.invoke(null, new HashMap<>(), params, "transition"));
    }

    @Test
    void workflowTransitionParam_defaultFade() throws Exception {
        Method m = WorkflowExecutionService.class.getDeclaredMethod(
                "workflowTransitionParam", Map.class, Map.class);
        m.setAccessible(true);
        assertEquals("fade", m.invoke(null, new HashMap<>(), new HashMap<>()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void workflowListParam_mergedSfx() throws Exception {
        Method m = WorkflowExecutionService.class.getDeclaredMethod(
                "workflowListParam", Map.class, Map.class, String.class);
        m.setAccessible(true);
        Map<String, Object> merged = Map.of("sfxUrls", List.of("http://a", "http://b"));
        List<String> out = (List<String>) m.invoke(null, merged, new HashMap<>(), "sfxUrls");
        assertEquals(2, out.size());
        assertEquals("http://a", out.get(0));
    }

    @Test
    void workflowStringParam_nullWhenMissing() throws Exception {
        Method m = WorkflowExecutionService.class.getDeclaredMethod(
                "workflowStringParam", Map.class, Map.class, String.class);
        m.setAccessible(true);
        assertNull(m.invoke(null, new HashMap<>(), new HashMap<>(), "bgmUrl"));
    }
}
