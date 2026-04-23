package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.module.product.entity.StylePreset;
import cn.gaifan.douyinOperations.module.product.service.StylePresetService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StylePresetController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("StylePresetController 集成测试")
class StylePresetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StylePresetService stylePresetService;

    @Test
    @DisplayName("获取启用的风格列表 - 应返回 200")
    void listEnabled_shouldReturn200() throws Exception {
        when(stylePresetService.listEnabled()).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/style-preset/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取全部风格 - 应返回 200")
    void listAll_shouldReturn200() throws Exception {
        when(stylePresetService.listAll()).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/style-preset/list-all")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取风格详情 - 应返回 200")
    void getById_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        StylePreset preset = new StylePreset();
        preset.setId(1L);
        preset.setPresetName("专业风格");

        when(stylePresetService.getById(eq(1L))).thenReturn(preset);

        mockMvc.perform(post("/api/v1/product/style-preset/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.presetName").value("专业风格"));
    }

    @Test
    @DisplayName("保存风格预设 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        StylePreset vo = new StylePreset();
        vo.setPresetName("新风格");
        vo.setPresetCode("new_style");

        StylePreset saved = new StylePreset();
        saved.setId(1L);
        saved.setPresetName("新风格");

        when(stylePresetService.save(any(), eq(1L))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/product/style-preset/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("删除风格预设 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(stylePresetService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/product/style-preset/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取启用的风格列表（未登录）- 应返回 2001")
    void listEnabled_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/product/style-preset/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取风格详情（缺少 id）- 应返回 1001")
    void getById_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/product/style-preset/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
