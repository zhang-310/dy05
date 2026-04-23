package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.service.WorkflowTemplateService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WorkflowTemplateController 集成测试")
class WorkflowTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowTemplateService workflowTemplateService;

    @Test
    @DisplayName("查询可用工作流模板 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> template1 = new HashMap<>();
        template1.put("id", 1L);
        template1.put("name", "标准短视频工作流");

        Map<String, Object> template2 = new HashMap<>();
        template2.put("id", 2L);
        template2.put("name", "爆款二创工作流");

        when(workflowTemplateService.listForOwner(eq(1L)))
                .thenReturn(List.of(template1, template2));

        mockMvc.perform(post("/api/v1/short-video/workflow-template/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    @DisplayName("根据ID获取模板详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        SvWorkflowTemplate template = new SvWorkflowTemplate();
        template.setId(1L);
        template.setTemplateName("标准短视频工作流");

        when(workflowTemplateService.getById(eq(1L), eq(1L)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/short-video/workflow-template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.templateName").value("标准短视频工作流"));
    }

    @Test
    @DisplayName("根据ID获取模板详情（空 body）- 应返回 200")
    void get_emptyBody_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();

        SvWorkflowTemplate template = new SvWorkflowTemplate();
        template.setId(null);

        when(workflowTemplateService.getById(eq(null), eq(1L)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/short-video/workflow-template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
