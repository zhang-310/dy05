package cn.gaifan.douyinOperations.module.config.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSaveVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSearchVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigVO;
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
@DisplayName("ConfigController 集成测试")
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigService configService;

    @Test
    @DisplayName("查询配置列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        ConfigSearchVO searchVO = new ConfigSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        ConfigVO configVO = new ConfigVO();
        configVO.setId(1L);
        configVO.setConfigKey("test.key");
        configVO.setConfigValue("test value");

        PageResultVO<ConfigVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(configVO));

        when(configService.search(any(ConfigSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/config/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("查询配置列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        ConfigSearchVO searchVO = new ConfigSearchVO();

        mockMvc.perform(post("/api/v1/config/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("查询配置列表（非管理员）- 应返回 2002")
    void list_forbidden_shouldReturn2002() throws Exception {
        ConfigSearchVO searchVO = new ConfigSearchVO();

        mockMvc.perform(post("/api/v1/config/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("按 Key 获取配置 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        ConfigVO configVO = new ConfigVO();
        configVO.setId(1L);
        configVO.setConfigKey("test.key");
        configVO.setConfigValue("test value");

        when(configService.getByKey(eq("test.key")))
                .thenReturn(configVO);

        Map<String, String> body = new HashMap<>();
        body.put("key", "test.key");

        mockMvc.perform(post("/api/v1/config/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.configKey").value("test.key"));
    }

    @Test
    @DisplayName("按 Key 获取配置（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("key", "test.key");

        mockMvc.perform(post("/api/v1/config/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("按 Key 获取配置（非管理员）- 应返回 2002")
    void get_forbidden_shouldReturn2002() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("key", "test.key");

        mockMvc.perform(post("/api/v1/config/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("保存配置 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        ConfigSaveVO saveVO = new ConfigSaveVO();
        saveVO.setConfigKey("new.key");
        saveVO.setConfigValue("new value");
        saveVO.setRemark("测试配置");

        when(configService.save(any(ConfigSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/config/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存配置（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        ConfigSaveVO saveVO = new ConfigSaveVO();
        saveVO.setConfigKey("new.key");
        saveVO.setConfigValue("new value");

        mockMvc.perform(post("/api/v1/config/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存配置（非管理员）- 应返回 2002")
    void save_forbidden_shouldReturn2002() throws Exception {
        ConfigSaveVO saveVO = new ConfigSaveVO();
        saveVO.setConfigKey("new.key");
        saveVO.setConfigValue("new value");

        mockMvc.perform(post("/api/v1/config/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("删除配置 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(configService).deleteById(eq(1L));

        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/config/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除配置（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/config/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除配置（非管理员）- 应返回 2002")
    void delete_forbidden_shouldReturn2002() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/config/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }
}
