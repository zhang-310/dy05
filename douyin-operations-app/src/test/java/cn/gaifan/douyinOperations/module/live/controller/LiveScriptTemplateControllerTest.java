package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptTemplateController 集成测试")
class LiveScriptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptTemplateRepository templateRepository;

    @MockBean
    private LiveScriptTemplateService templateService;

    @Test
    @DisplayName("分页搜索模板 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptType", "product");
        body.put("page", 0);
        body.put("rows", 10);

        LiveScriptTemplate template1 = new LiveScriptTemplate();
        template1.setId(1L);
        template1.setTemplateName("产品介绍模板");
        template1.setScriptType("product");
        template1.setContent("这是一个产品介绍模板");

        LiveScriptTemplate template2 = new LiveScriptTemplate();
        template2.setId(2L);
        template2.setTemplateName("产品卖点模板");
        template2.setScriptType("product");
        template2.setContent("这是一个产品卖点模板");

        Page<LiveScriptTemplate> page = new PageImpl<>(List.of(template1, template2));

        when(templateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(post("/api/v1/live/template/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].templateName").value("产品介绍模板"))
                .andExpect(jsonPath("$.data.list[1].templateName").value("产品卖点模板"));
    }

    @Test
    @DisplayName("分页搜索模板（带关键词）- 应返回 200")
    void search_withKeyword_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("keyword", "卖点");
        body.put("page", 0);
        body.put("rows", 10);

        LiveScriptTemplate template = new LiveScriptTemplate();
        template.setId(1L);
        template.setTemplateName("产品卖点模板");

        Page<LiveScriptTemplate> page = new PageImpl<>(List.of(template));

        when(templateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(post("/api/v1/live/template/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].templateName").value("产品卖点模板"));
    }

    @Test
    @DisplayName("手动保存话术为模板 - 应返回 200")
    void saveFromScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);
        body.put("templateName", "我的自定义模板");
        body.put("category", "product");

        LiveScriptTemplate savedTemplate = new LiveScriptTemplate();
        savedTemplate.setId(1L);
        savedTemplate.setTemplateName("我的自定义模板");
        savedTemplate.setScriptType("product");

        when(templateService.saveFromScript(eq(1L), eq("我的自定义模板"), eq("product")))
                .thenReturn(savedTemplate);

        mockMvc.perform(post("/api/v1/live/template/save-from-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.templateName").value("我的自定义模板"));
    }

    @Test
    @DisplayName("应用模板到话术 - 应返回 200")
    void apply_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateId", 1L);

        LiveScriptTemplate template = new LiveScriptTemplate();
        template.setId(1L);
        template.setTemplateName("产品介绍模板");
        template.setScriptType("product");
        template.setContent("这是模板内容");
        template.setUsageCount(5);

        when(templateRepository.findById(eq(1L)))
                .thenReturn(Optional.of(template));
        when(templateRepository.save(any(LiveScriptTemplate.class)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/live/template/apply")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value("这是模板内容"))
                .andExpect(jsonPath("$.data.scriptType").value("product"))
                .andExpect(jsonPath("$.data.templateName").value("产品介绍模板"));
    }

    @Test
    @DisplayName("应用模板到话术（模板不存在）- 应返回 3405")
    void apply_templateNotFound_shouldReturn3405() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateId", 999L);

        when(templateRepository.findById(eq(999L)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/live/template/apply")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(3405))
                .andExpect(jsonPath("$.message").value("模板不存在"));
    }

    @Test
    @DisplayName("手动触发自动沉淀（管理员）- 应返回 200")
    void autoCollect_asAdmin_shouldReturn200() throws Exception {
        when(templateService.importFromHighEffectivenessScripts())
                .thenReturn(10);

        mockMvc.perform(post("/api/v1/live/template/auto-collect")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(10));
    }

    @Test
    @DisplayName("手动触发自动沉淀（非管理员）- 应返回 2002")
    void autoCollect_asUser_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/live/template/auto-collect")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可触发自动沉淀"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);

        mockMvc.perform(post("/api/v1/live/template/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
