package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.LiveTemplateService;
import cn.gaifan.douyinOperations.module.live.vo.SaveSessionAsTemplateVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveTemplateController 集成测试")
class LiveTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveTemplateService liveTemplateService;

    @Test
    @DisplayName("从场次创建模板 - 应返回 200")
    void saveFromSession_shouldReturn200() throws Exception {
        SaveSessionAsTemplateVO vo = new SaveSessionAsTemplateVO();
        vo.setSessionId(100L);
        vo.setTemplateName("护肤品直播模板");
        vo.setScriptTypes(List.of("opening", "product", "closing"));

        when(liveTemplateService.saveSessionAsTemplate(eq(100L), eq("护肤品直播模板"), anyList(), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/session-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("从场次创建模板（包含全部话术类型）- 应返回 200")
    void saveFromSession_withAllScriptTypes_shouldReturn200() throws Exception {
        SaveSessionAsTemplateVO vo = new SaveSessionAsTemplateVO();
        vo.setSessionId(100L);
        vo.setTemplateName("完整直播模板");
        vo.setScriptTypes(null);

        when(liveTemplateService.saveSessionAsTemplate(eq(100L), eq("完整直播模板"), isNull(), eq(1L)))
                .thenReturn(2L);

        mockMvc.perform(post("/api/v1/live/session-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("从场次创建模板（空话术类型列表）- 应返回 200")
    void saveFromSession_withEmptyScriptTypes_shouldReturn200() throws Exception {
        SaveSessionAsTemplateVO vo = new SaveSessionAsTemplateVO();
        vo.setSessionId(100L);
        vo.setTemplateName("空类型模板");
        vo.setScriptTypes(List.of());

        when(liveTemplateService.saveSessionAsTemplate(eq(100L), eq("空类型模板"), anyList(), eq(1L)))
                .thenReturn(3L);

        mockMvc.perform(post("/api/v1/live/session-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void saveFromSession_withoutAuth_shouldReturn2001() throws Exception {
        SaveSessionAsTemplateVO vo = new SaveSessionAsTemplateVO();
        vo.setSessionId(100L);
        vo.setTemplateName("测试模板");

        mockMvc.perform(post("/api/v1/live/session-template/save-from-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
