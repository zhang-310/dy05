package cn.gaifan.douyinOperations.module.agent.controller;

import cn.gaifan.douyinOperations.module.agent.service.UserPreferenceService;
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
 * UserPreferenceController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("UserPreferenceController 集成测试")
class UserPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @Test
    @DisplayName("获取话术迭代建议 - 应返回 200")
    void getRefineSuggestions_shouldReturn200() throws Exception {
        List<String> instructions = List.of("优化开场白", "增加互动", "强调卖点");
        List<String> scriptTypes = List.of("直播话术", "短视频脚本", "商品介绍");

        when(userPreferenceService.getTopPreferences(eq(1L), eq("instruction_used"), anyInt()))
                .thenReturn(instructions);
        when(userPreferenceService.getTopPreferences(eq(1L), eq("script_type"), anyInt()))
                .thenReturn(scriptTypes);

        mockMvc.perform(post("/api/v1/agent/preference/refine-suggestions")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.instructionUsed.length()").value(3))
                .andExpect(jsonPath("$.data.scriptType.length()").value(3))
                .andExpect(jsonPath("$.data.instructionUsed[0]").value("优化开场白"))
                .andExpect(jsonPath("$.data.scriptType[0]").value("直播话术"));
    }

    @Test
    @DisplayName("获取话术迭代建议（默认 limit）- 应返回 200")
    void getRefineSuggestions_withDefaultLimit_shouldReturn200() throws Exception {
        List<String> instructions = List.of("优化开场白");
        List<String> scriptTypes = List.of("直播话术");

        when(userPreferenceService.getTopPreferences(eq(1L), eq("instruction_used"), eq(10)))
                .thenReturn(instructions);
        when(userPreferenceService.getTopPreferences(eq(1L), eq("script_type"), eq(10)))
                .thenReturn(scriptTypes);

        mockMvc.perform(post("/api/v1/agent/preference/refine-suggestions")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.instructionUsed.length()").value(1))
                .andExpect(jsonPath("$.data.scriptType.length()").value(1));
    }

    @Test
    @DisplayName("获取话术迭代建议（limit 超过上限）- 应返回 200")
    void getRefineSuggestions_withLargeLimit_shouldReturn200() throws Exception {
        List<String> instructions = List.of("优化开场白");
        List<String> scriptTypes = List.of("直播话术");

        when(userPreferenceService.getTopPreferences(eq(1L), eq("instruction_used"), eq(10)))
                .thenReturn(instructions);
        when(userPreferenceService.getTopPreferences(eq(1L), eq("script_type"), eq(10)))
                .thenReturn(scriptTypes);

        mockMvc.perform(post("/api/v1/agent/preference/refine-suggestions")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void getRefineSuggestions_withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/agent/preference/refine-suggestions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
