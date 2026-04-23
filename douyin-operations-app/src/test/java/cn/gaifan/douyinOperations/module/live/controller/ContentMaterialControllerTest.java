package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.ContentMaterialService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ContentMaterialController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ContentMaterialController 集成测试")
class ContentMaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContentMaterialService contentMaterialService;

    @Test
    @DisplayName("获取随机素材 - 应返回 200")
    void getRandomMaterials_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("materialType", "joke");
        body.put("category", "护肤");
        body.put("count", 5);

        List<Map<String, Object>> materials = List.of(
                Map.of("id", 1, "content", "笑话内容1", "category", "护肤"),
                Map.of("id", 2, "content", "笑话内容2", "category", "护肤")
        );

        when(contentMaterialService.getRandomMaterials(eq("joke"), eq("护肤"), eq(5)))
                .thenReturn(materials);

        mockMvc.perform(post("/api/v1/live/material/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].content").value("笑话内容1"))
                .andExpect(jsonPath("$.data[1].content").value("笑话内容2"));
    }

    @Test
    @DisplayName("获取随机素材（默认参数）- 应返回 200")
    void getRandomMaterials_withDefaults_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> materials = List.of(
                Map.of("id", 1, "content", "默认笑话")
        );

        when(contentMaterialService.getRandomMaterials(eq("joke"), isNull(), eq(3)))
                .thenReturn(materials);

        mockMvc.perform(post("/api/v1/live/material/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].content").value("默认笑话"));
    }

    @Test
    @DisplayName("获取随机素材（count 超过上限）- 应限制为 10")
    void getRandomMaterials_withLargeCount_shouldLimit10() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("count", 100);

        List<Map<String, Object>> materials = List.of(
                Map.of("id", 1, "content", "素材1")
        );

        when(contentMaterialService.getRandomMaterials(eq("joke"), isNull(), eq(10)))
                .thenReturn(materials);

        mockMvc.perform(post("/api/v1/live/material/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("按人设获取素材 - 应返回 200")
    void getMaterialsByPersona_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("materialType", "story");
        body.put("personaType", "professional");
        body.put("ageRange", "25-35");
        body.put("count", 3);

        List<Map<String, Object>> materials = List.of(
                Map.of("id", 1, "content", "专业故事1", "personaType", "professional")
        );

        when(contentMaterialService.getMaterialsByPersona(eq("story"), eq("professional"), eq("25-35"), eq(3)))
                .thenReturn(materials);

        mockMvc.perform(post("/api/v1/live/material/by-persona")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].content").value("专业故事1"));
    }

    @Test
    @DisplayName("获取素材分类 - 应返回 200")
    void getCategories_shouldReturn200() throws Exception {
        Map<String, List<String>> categories = Map.of(
                "joke", List.of("护肤", "彩妆", "生活"),
                "story", List.of("成功案例", "用户故事")
        );

        when(contentMaterialService.getMaterialCategories()).thenReturn(categories);

        mockMvc.perform(post("/api/v1/live/material/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.joke[0]").value("护肤"))
                .andExpect(jsonPath("$.data.story[0]").value("成功案例"));
    }

    @Test
    @DisplayName("构建素材提示词 - 应返回 200")
    void buildMaterialPrompt_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("materialType", "joke");
        body.put("category", "护肤");

        String prompt = "请生成一个关于护肤的幽默笑话";

        when(contentMaterialService.buildMaterialPrompt(eq("joke"), eq("护肤")))
                .thenReturn(prompt);

        mockMvc.perform(post("/api/v1/live/material/prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(prompt));
    }

    @Test
    @DisplayName("获取表演提示词 - 应返回 200")
    void getPerformancePrompt_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("category", "互动");

        String prompt = "请用生动的语言进行互动表演";

        when(contentMaterialService.buildPerformancePrompt(eq("互动")))
                .thenReturn(prompt);

        mockMvc.perform(post("/api/v1/live/material/performance-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(prompt));
    }

    @Test
    @DisplayName("匹配风险话术 - 应返回 200")
    void matchRiskScripts_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("riskType", "夸大宣传");

        List<String> riskScripts = List.of(
                "绝对有效",
                "100%治愈",
                "立即见效"
        );

        when(contentMaterialService.matchRiskScripts(eq("夸大宣传")))
                .thenReturn(riskScripts);

        mockMvc.perform(post("/api/v1/live/material/risk-match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0]").value("绝对有效"))
                .andExpect(jsonPath("$.data[2]").value("立即见效"));
    }
}
