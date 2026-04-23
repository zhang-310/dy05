package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.service.ScriptTemplateService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptTemplateVO;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ScriptTemplateController 集成测试")
class ScriptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScriptTemplateService scriptTemplateService;

    @Test
    @DisplayName("查询模板 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<ScriptTemplateVO> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(scriptTemplateService.search(any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/script/template/search")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("查询模板（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        mockMvc.perform(post("/api/v1/script/template/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取模板详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        ScriptTemplateVO template = new ScriptTemplateVO();
        template.setId(1L);
        template.setTemplateName("测试模板");

        when(scriptTemplateService.getById(anyLong()))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/script/template/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("保存模板 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateName", "测试模板");
        body.put("content", "模板内容");
        body.put("scene", "live");

        when(scriptTemplateService.save(any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/script/template/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除模板 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/v1/script/template/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("增加使用次数 - 应返回 204")
    void incrementUseCount_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/v1/script/template/use-count")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("按场景获取模板 - 应返回 200")
    void listByScene_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("scene", "live");

        ScriptTemplateVO template = new ScriptTemplateVO();
        template.setId(1L);

        when(scriptTemplateService.listByScene(anyString()))
                .thenReturn(List.of(template));

        mockMvc.perform(post("/api/v1/script/template/by-scene")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
