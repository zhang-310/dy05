package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.module.script.service.ScriptGenerationService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationVO;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ScriptGenerationController 集成测试")
class ScriptGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScriptGenerationService scriptGenerationService;

    @Test
    @DisplayName("生成脚本 - 应返回 200")
    void generateScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productName", "测试护肤品");
        body.put("productPrice", 299.0);
        body.put("keyFeatures", List.of("保湿", "美白", "抗衰老"));
        body.put("duration", 60);
        body.put("style", "专业");

        ScriptGenerationVO result = new ScriptGenerationVO();
        result.setId(1L);

        when(scriptGenerationService.generateScript(anyLong(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/script/generate")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("生成脚本（未登录）- 应返回 2001")
    void generateScript_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", "生成一个护肤品直播脚本");

        mockMvc.perform(post("/api/v1/script/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
