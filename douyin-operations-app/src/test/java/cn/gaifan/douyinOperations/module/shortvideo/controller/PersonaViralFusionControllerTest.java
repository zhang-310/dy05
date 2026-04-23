package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.PersonaViralFusionService;
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
 * PersonaViralFusionController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PersonaViralFusionController 集成测试")
class PersonaViralFusionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonaViralFusionService fusionService;

    @Test
    @DisplayName("为爆款匹配人设 - 应返回 200")
    void matchPersonas_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);

        Map<String, Object> persona = new HashMap<>();
        persona.put("personaId", 1L);
        persona.put("personaName", "美妆达人");
        persona.put("matchScore", 0.95);

        when(fusionService.matchPersonas(eq(1L), eq(1L))).thenReturn(List.of(persona));

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/match-personas")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].personaId").value(1));
    }

    @Test
    @DisplayName("为爆款匹配人设（缺少 viralVideoId）- 应返回 1001")
    void matchPersonas_missingViralVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/match-personas")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("爆款×人设融合生成脚本 - 应返回 200")
    void generateFusedScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);
        body.put("personaId", 1L);
        body.put("remakeType", "form_imitation");

        Map<String, Object> result = new HashMap<>();
        result.put("scriptId", 1L);
        result.put("script", "融合后的脚本内容...");
        result.put("fusionScore", 0.92);

        when(fusionService.generatePersonaFusedScript(eq(1L), eq(1L), eq("form_imitation"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/generate-fused-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(1))
                .andExpect(jsonPath("$.data.fusionScore").value(0.92));
    }

    @Test
    @DisplayName("爆款×人设融合生成脚本（缺少参数）- 应返回 1001")
    void generateFusedScript_missingParams_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/generate-fused-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("爆款×人设融合生成脚本（默认 remakeType）- 应返回 200")
    void generateFusedScript_defaultRemakeType_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);
        body.put("personaId", 1L);

        Map<String, Object> result = new HashMap<>();
        result.put("scriptId", 1L);
        result.put("script", "融合后的脚本内容...");

        when(fusionService.generatePersonaFusedScript(eq(1L), eq(1L), eq("form_imitation"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/generate-fused-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(1));
    }

    @Test
    @DisplayName("热点×人设×产品融合 - 应返回 200")
    void generateHotspotFused_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("hotTopicId", 1L);
        body.put("personaId", 1L);
        body.put("productId", 1L);

        Map<String, Object> result = new HashMap<>();
        result.put("scriptId", 1L);
        result.put("script", "热点融合脚本内容...");
        result.put("hotspotRelevance", 0.88);

        when(fusionService.generateHotspotFusedScript(eq(1L), eq(1L), eq(1L), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/generate-hotspot-fused")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(1))
                .andExpect(jsonPath("$.data.hotspotRelevance").value(0.88));
    }

    @Test
    @DisplayName("热点×人设×产品融合（无 productId）- 应返回 200")
    void generateHotspotFused_noProductId_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("hotTopicId", 1L);
        body.put("personaId", 1L);

        Map<String, Object> result = new HashMap<>();
        result.put("scriptId", 1L);
        result.put("script", "热点融合脚本内容...");

        when(fusionService.generateHotspotFusedScript(eq(1L), eq(1L), isNull(), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/generate-hotspot-fused")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(1));
    }

    @Test
    @DisplayName("热点×人设×产品融合（缺少必需参数）- 应返回 1001")
    void generateHotspotFused_missingParams_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("hotTopicId", 1L);

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/generate-hotspot-fused")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("为爆款匹配人设（未登录）- 应返回 2001")
    void matchPersonas_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);

        mockMvc.perform(post("/api/v1/short-video/persona-fusion/match-personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
