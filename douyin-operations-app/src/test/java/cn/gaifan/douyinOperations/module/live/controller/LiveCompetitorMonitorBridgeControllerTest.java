package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.CompetitorMonitorService;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveCompetitorMonitorBridgeController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveCompetitorMonitorBridgeController 集成测试")
class LiveCompetitorMonitorBridgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompetitorMonitorService competitorMonitorService;

    @Test
    @DisplayName("竞品账号列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> competitor1 = new HashMap<>();
        competitor1.put("id", 1L);
        competitor1.put("accountName", "竞品A");
        competitor1.put("platform", "douyin");

        Map<String, Object> competitor2 = new HashMap<>();
        competitor2.put("id", 2L);
        competitor2.put("accountName", "竞品B");
        competitor2.put("platform", "kuaishou");

        when(competitorMonitorService.listCompetitors(eq(1L)))
                .thenReturn(List.of(competitor1, competitor2));

        mockMvc.perform(post("/api/v1/live/competitor-monitor/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].accountName").value("竞品A"))
                .andExpect(jsonPath("$.data[1].accountName").value("竞品B"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/live/competitor-monitor/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
