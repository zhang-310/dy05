package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiQuotaController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiQuotaController 集成测试")
class AiQuotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiQuotaService aiQuotaService;

    @Test
    @DisplayName("获取配额信息 - 应返回 200")
    void getQuota_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        when(aiQuotaService.getAdminQuotaOverview()).thenReturn(Map.of(
                "items", java.util.List.of(Map.of("feature", "script_gen", "limit", 500, "used", 12))
        ));

        mockMvc.perform(post("/api/v1/ai/admin/quota/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("更新配额上限 - 应返回 204")
    void updateQuota_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        body.put("maxCount", 1000);

        mockMvc.perform(post("/api/v1/ai/admin/quota/update")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("配额历史记录 - 应返回 200")
    void quotaHistory_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 20);
        body.put("feature", "live_script");
        when(aiQuotaService.getQuotaHistory(0, 20, "live_script"))
                .thenReturn(cn.gaifan.douyinOperations.common.vo.PageResultVO.of(1L,
                        java.util.List.of(Map.of(
                                "id", "live_script-2026-04-11",
                                "feature", "live_script",
                                "used", 5,
                                "limit", 100,
                                "period", "daily",
                                "createTime", "2026-04-11T00:00:00"
                        )), 0, 20));

        mockMvc.perform(post("/api/v1/ai/admin/quota/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("获取当前用户当日额度信息 - 应返回 200")
    void getQuotaInfo_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        AiQuotaService.QuotaInfo quotaInfo = new AiQuotaService.QuotaInfo(50, 100, 50);
        when(aiQuotaService.getQuota(anyLong())).thenReturn(quotaInfo);

        mockMvc.perform(post("/api/v1/ai/admin/quota/info")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.usedCount").value(50))
                .andExpect(jsonPath("$.data.maxCount").value(100))
                .andExpect(jsonPath("$.data.remaining").value(50));
    }

    @Test
    @DisplayName("非管理员访问管理端点 - 应返回 2002")
    void getQuota_withoutAdmin_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/quota/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }

    @Test
    @DisplayName("未认证访问用户端点 - 应返回 2001")
    void getQuotaInfo_withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/quota/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
