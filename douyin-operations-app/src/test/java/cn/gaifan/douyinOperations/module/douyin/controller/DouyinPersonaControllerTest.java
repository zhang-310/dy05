package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinPersonaService;
import cn.gaifan.douyinOperations.module.douyin.vo.PersonaSaveVO;
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
@DisplayName("DouyinPersonaController 集成测试")
class DouyinPersonaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DouyinPersonaService personaService;

    @Test
    @DisplayName("保存人设 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        PersonaSaveVO saveVO = new PersonaSaveVO();
        saveVO.setPersonaName("测试人设");
        saveVO.setPersonaType("beauty");

        when(personaService.savePersona(any(PersonaSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/douyin/persona/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存人设（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        PersonaSaveVO saveVO = new PersonaSaveVO();
        saveVO.setPersonaName("测试人设");

        mockMvc.perform(post("/api/v1/douyin/persona/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取人设列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        DyPersona persona = new DyPersona();
        persona.setId(1L);
        persona.setPersonaName("测试人设");

        when(personaService.listPersonas(eq(1L), isNull()))
                .thenReturn(List.of(persona));

        mockMvc.perform(post("/api/v1/douyin/persona/list")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("获取人设列表（按类型）- 应返回 200")
    void list_withType_shouldReturn200() throws Exception {
        DyPersona persona = new DyPersona();
        persona.setId(1L);
        persona.setPersonaType("beauty");

        when(personaService.listPersonas(eq(1L), eq("beauty")))
                .thenReturn(List.of(persona));

        mockMvc.perform(post("/api/v1/douyin/persona/list")
                        .requestAttr("userId", 1L)
                        .param("personaType", "beauty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取人设列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/persona/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取人设详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        DyPersona persona = new DyPersona();
        persona.setId(1L);
        persona.setPersonaName("测试人设");

        when(personaService.getPersona(eq(1L), eq(1L)))
                .thenReturn(persona);

        mockMvc.perform(post("/api/v1/douyin/persona/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取人设详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/persona/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除人设 - 应返回 204")
    void delete_shouldReturn200() throws Exception {
        doNothing().when(personaService).deletePersona(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/douyin/persona/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除人设（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/persona/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("设置默认人设 - 应返回 204")
    void setDefault_shouldReturn200() throws Exception {
        doNothing().when(personaService).setDefaultPersona(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/douyin/persona/set-default")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("设置默认人设（未登录）- 应返回 2001")
    void setDefault_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/persona/set-default")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取默认人设 - 应返回 200")
    void getDefault_shouldReturn200() throws Exception {
        DyPersona persona = new DyPersona();
        persona.setId(1L);
        persona.setIsDefault(1);

        when(personaService.getDefaultPersona(eq(1L)))
                .thenReturn(persona);

        mockMvc.perform(post("/api/v1/douyin/persona/get-default")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取默认人设（未登录）- 应返回 2001")
    void getDefault_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/persona/get-default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取系统模板 - 应返回 200")
    void templates_shouldReturn200() throws Exception {
        DyPersona template = new DyPersona();
        template.setId(1L);
        template.setPersonaName("系统模板");

        when(personaService.getSystemTemplates())
                .thenReturn(List.of(template));

        mockMvc.perform(post("/api/v1/douyin/persona/templates")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("获取系统模板（未登录）- 应返回 2001")
    void templates_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/persona/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("按账号获取人设 - 应返回 200")
    void getByAccount_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("accountId", 1L);

        DyPersona persona = new DyPersona();
        persona.setId(1L);
        persona.setAccountId(1L);

        when(personaService.getPersonaByAccountId(eq(1L), eq(1L)))
                .thenReturn(persona);

        mockMvc.perform(post("/api/v1/douyin/persona/get-by-account")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accountId").value(1));
    }

    @Test
    @DisplayName("按账号获取人设（缺少 accountId）- 应返回 1001")
    void getByAccount_missingAccountId_shouldReturn1001() throws Exception {
        Map<String, Long> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/douyin/persona/get-by-account")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("按账号获取人设（未登录）- 应返回 2001")
    void getByAccount_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("accountId", 1L);

        mockMvc.perform(post("/api/v1/douyin/persona/get-by-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
