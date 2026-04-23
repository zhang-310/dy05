package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveEffectivenessConfig;
import cn.gaifan.douyinOperations.module.live.service.LiveEffectivenessConfigService;
import cn.gaifan.douyinOperations.module.live.vo.LiveEffectivenessConfigSaveVO;
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

import java.math.BigDecimal;
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
 * LiveEffectivenessConfigController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveEffectivenessConfigController 集成测试")
class LiveEffectivenessConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveEffectivenessConfigService configService;

    @Test
    @DisplayName("查询评分配置列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        LiveEffectivenessConfig config1 = new LiveEffectivenessConfig();
        config1.setId(1L);
        config1.setConfigName("默认配置");
        config1.setConversionWeight(new BigDecimal("0.40"));
        config1.setInteractionWeight(new BigDecimal("0.30"));
        config1.setRetentionWeight(new BigDecimal("0.20"));
        config1.setGmvWeight(new BigDecimal("0.10"));
        config1.setIsDefault(1);

        LiveEffectivenessConfig config2 = new LiveEffectivenessConfig();
        config2.setId(2L);
        config2.setConfigName("自定义配置");
        config2.setConversionWeight(new BigDecimal("0.50"));
        config2.setInteractionWeight(new BigDecimal("0.25"));
        config2.setRetentionWeight(new BigDecimal("0.15"));
        config2.setGmvWeight(new BigDecimal("0.10"));
        config2.setIsDefault(0);

        when(configService.list(eq(1L))).thenReturn(List.of(config1, config2));

        mockMvc.perform(post("/api/v1/live/effectiveness-config/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].configName").value("默认配置"))
                .andExpect(jsonPath("$.data[0].isDefault").value(1))
                .andExpect(jsonPath("$.data[1].configName").value("自定义配置"));
    }

    @Test
    @DisplayName("保存评分配置 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveEffectivenessConfigSaveVO saveVO = new LiveEffectivenessConfigSaveVO();
        saveVO.setConfigName("新配置");
        saveVO.setConversionWeight(new BigDecimal("0.40"));
        saveVO.setInteractionWeight(new BigDecimal("0.30"));
        saveVO.setRetentionWeight(new BigDecimal("0.20"));
        saveVO.setGmvWeight(new BigDecimal("0.10"));

        LiveEffectivenessConfig saved = new LiveEffectivenessConfig();
        saved.setId(1L);
        saved.setConfigName("新配置");
        saved.setConversionWeight(new BigDecimal("0.40"));
        saved.setInteractionWeight(new BigDecimal("0.30"));
        saved.setRetentionWeight(new BigDecimal("0.20"));
        saved.setGmvWeight(new BigDecimal("0.10"));

        when(configService.save(any(LiveEffectivenessConfigSaveVO.class), eq(1L)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/live/effectiveness-config/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.configName").value("新配置"));
    }

    @Test
    @DisplayName("获取默认配置 - 应返回 200")
    void getDefault_shouldReturn200() throws Exception {
        LiveEffectivenessConfig config = new LiveEffectivenessConfig();
        config.setId(1L);
        config.setConfigName("默认配置");
        config.setIsDefault(1);
        config.setConversionWeight(new BigDecimal("0.40"));
        config.setInteractionWeight(new BigDecimal("0.30"));
        config.setRetentionWeight(new BigDecimal("0.20"));
        config.setGmvWeight(new BigDecimal("0.10"));

        when(configService.getDefaultConfig(eq(1L))).thenReturn(config);

        mockMvc.perform(post("/api/v1/live/effectiveness-config/default")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.configName").value("默认配置"))
                .andExpect(jsonPath("$.data.isDefault").value(1));
    }

    @Test
    @DisplayName("设置默认配置 - 应返回 200")
    void setDefault_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("configId", 1L);

        doNothing().when(configService).setDefault(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/effectiveness-config/set-default")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("设置默认配置（缺少 configId）- 应返回 1001")
    void setDefault_withoutConfigId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/effectiveness-config/set-default")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("configId 不能为空"));
    }

    @Test
    @DisplayName("删除配置 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("configId", 1L);

        doNothing().when(configService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/effectiveness-config/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除配置（缺少 configId）- 应返回 1001")
    void delete_withoutConfigId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/effectiveness-config/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("configId 不能为空"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/live/effectiveness-config/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
