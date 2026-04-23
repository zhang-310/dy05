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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ScriptTemplateAdminController 集成测试")
class ScriptTemplateAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScriptTemplateService scriptTemplateService;

    @Test
    @DisplayName("系统模板列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<ScriptTemplateVO> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(scriptTemplateService.search(any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/script/admin/template/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("系统模板列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        mockMvc.perform(post("/api/v1/script/admin/template/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存系统模板 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateName", "测试模板");
        body.put("content", "模板内容");

        when(scriptTemplateService.saveSystemTemplate(any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/script/admin/template/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("保存系统模板（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateName", "测试模板");
        body.put("content", "模板内容");

        mockMvc.perform(post("/api/v1/script/admin/template/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除系统模板 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/v1/script/admin/template/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除系统模板（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/script/admin/template/delete")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
