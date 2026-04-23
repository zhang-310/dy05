package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationPreset;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationPresetService;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationPresetSaveVO;
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveGenerationPresetController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveGenerationPresetController 集成测试")
class LiveGenerationPresetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveGenerationPresetService presetService;

    @Test
    @DisplayName("获取预设列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        LiveGenerationPreset preset = new LiveGenerationPreset();
        preset.setId(1L);
        preset.setName("默认预设");
        preset.setDescription("标准话术生成配置");
        preset.setStyle("专业");
        preset.setModelId(1L);
        preset.setUseKbRef(true);
        preset.setIsDefault(true);
        preset.setOwnerId(1L);
        preset.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(presetService.list(eq(1L))).thenReturn(List.of(preset));

        mockMvc.perform(post("/api/v1/live/generation-preset/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].name").value("默认预设"))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    @Test
    @DisplayName("保存预设 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveGenerationPresetSaveVO saveVO = new LiveGenerationPresetSaveVO();
        saveVO.setName("新预设");
        saveVO.setDescription("自定义配置");
        saveVO.setStyle("活泼");
        saveVO.setModelId(2L);
        saveVO.setUseKbRef(false);
        saveVO.setDurationMode("short");
        saveVO.setHotKeywords("护肤,美白");

        LiveGenerationPreset preset = new LiveGenerationPreset();
        preset.setId(1L);
        preset.setName("新预设");
        preset.setStyle("活泼");
        preset.setOwnerId(1L);
        preset.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(presetService.save(any(LiveGenerationPresetSaveVO.class), eq(1L)))
                .thenReturn(preset);

        mockMvc.perform(post("/api/v1/live/generation-preset/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("新预设"));
    }

    @Test
    @DisplayName("删除预设 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(presetService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/generation-preset/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除预设（缺少 id）- 应返回 1001")
    void delete_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/generation-preset/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("获取默认预设 - 应返回 200")
    void getDefault_shouldReturn200() throws Exception {
        LiveGenerationPreset preset = new LiveGenerationPreset();
        preset.setId(1L);
        preset.setName("默认预设");
        preset.setIsDefault(true);
        preset.setOwnerId(1L);

        when(presetService.getDefault(eq(1L))).thenReturn(preset);

        mockMvc.perform(post("/api/v1/live/generation-preset/getDefault")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("默认预设"))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    @DisplayName("设为默认预设 - 应返回 204")
    void setDefault_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(presetService).setDefault(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/generation-preset/set-default")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("设为默认预设（缺少 id）- 应返回 1001")
    void setDefault_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/generation-preset/set-default")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/live/generation-preset/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
