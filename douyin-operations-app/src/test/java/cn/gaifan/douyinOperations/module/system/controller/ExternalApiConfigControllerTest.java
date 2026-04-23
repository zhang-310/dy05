package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.service.ExternalApiConfigService;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSaveVO;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSearchVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ExternalApiConfigController 集成测试")
class ExternalApiConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExternalApiConfigService externalApiConfigService;

    @Test
    @DisplayName("配置列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        ExternalApiConfigSearchVO searchVO = new ExternalApiConfigSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        ExternalApiConfig config = new ExternalApiConfig();
        config.setId(1L);
        config.setProviderCode("openai");

        PageResultVO<ExternalApiConfig> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(config));

        when(externalApiConfigService.search(any(ExternalApiConfigSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/system/external-api/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("配置列表（空 body）- 应返回 200")
    void list_emptyBody_shouldReturn200() throws Exception {
        PageResultVO<ExternalApiConfig> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(externalApiConfigService.search(any(ExternalApiConfigSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/system/external-api/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("配置列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        ExternalApiConfigSearchVO searchVO = new ExternalApiConfigSearchVO();

        mockMvc.perform(post("/api/v1/system/external-api/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("配置列表（非 admin）- 应返回 2002")
    void list_forbidden_shouldReturn2002() throws Exception {
        ExternalApiConfigSearchVO searchVO = new ExternalApiConfigSearchVO();

        mockMvc.perform(post("/api/v1/system/external-api/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("获取配置详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("providerCode", "openai");

        ExternalApiConfig config = new ExternalApiConfig();
        config.setId(1L);
        config.setProviderCode("openai");

        when(externalApiConfigService.getByProviderCode(eq("openai")))
                .thenReturn(config);

        mockMvc.perform(post("/api/v1/system/external-api/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.providerCode").value("openai"));
    }

    @Test
    @DisplayName("获取配置详情（空 providerCode）- 应返回 1001")
    void get_emptyProviderCode_shouldReturn1001() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/external-api/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("获取配置详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("providerCode", "openai");

        mockMvc.perform(post("/api/v1/system/external-api/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存配置 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        ExternalApiConfigSaveVO saveVO = new ExternalApiConfigSaveVO();
        saveVO.setProviderCode("openai");
        saveVO.setProviderName("OpenAI");

        ExternalApiConfig config = new ExternalApiConfig();
        config.setId(1L);
        config.setProviderCode("openai");

        when(externalApiConfigService.save(any(ExternalApiConfigSaveVO.class)))
                .thenReturn(config);

        mockMvc.perform(post("/api/v1/system/external-api/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.providerCode").value("openai"));
    }

    @Test
    @DisplayName("保存配置（未登录）- 应返回 1001")
    void save_unauthorized_shouldReturn1001() throws Exception {
        ExternalApiConfigSaveVO saveVO = new ExternalApiConfigSaveVO();

        mockMvc.perform(post("/api/v1/system/external-api/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除配置 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("id", 1L);

        doNothing().when(externalApiConfigService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/system/external-api/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除配置（空 id）- 应返回 1001")
    void delete_emptyId_shouldReturn1001() throws Exception {
        Map<String, Long> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/external-api/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("更新健康状态 - 应返回 200")
    void updateHealthStatus_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("providerCode", "openai");
        request.put("status", "healthy");
        request.put("latencyMs", 100);
        request.put("successRate", 0.99);

        doNothing().when(externalApiConfigService)
                .updateHealthStatus(eq("openai"), eq("healthy"), eq(100), eq(0.99f));

        mockMvc.perform(post("/api/v1/system/external-api/health-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("更新健康状态（空 providerCode）- 应返回 1001")
    void updateHealthStatus_emptyProviderCode_shouldReturn1001() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("status", "healthy");

        mockMvc.perform(post("/api/v1/system/external-api/health-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("按分类获取配置 - 应返回 200")
    void byCategory_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("category", "ai");

        ExternalApiConfig config = new ExternalApiConfig();
        config.setId(1L);
        config.setProviderCode("openai");

        when(externalApiConfigService.getEnabledByCategory(eq("ai")))
                .thenReturn(List.of(config));

        mockMvc.perform(post("/api/v1/system/external-api/by-category")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].providerCode").value("openai"));
    }

    @Test
    @DisplayName("按分类获取配置（空 category）- 应返回 1001")
    void byCategory_emptyCategory_shouldReturn1001() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/external-api/by-category")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("按分类获取配置（未登录）- 应返回 2001")
    void byCategory_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("category", "ai");

        mockMvc.perform(post("/api/v1/system/external-api/by-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
