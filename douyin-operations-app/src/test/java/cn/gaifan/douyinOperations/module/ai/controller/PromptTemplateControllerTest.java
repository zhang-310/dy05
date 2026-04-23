package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import cn.gaifan.douyinOperations.module.ai.service.PromptTemplateService;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSearchVO;
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

/**
 * PromptTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PromptTemplateController 集成测试")
class PromptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PromptTemplateService promptTemplateService;

    @Test
    @DisplayName("分页查询提示词模板 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        AiPromptTemplateSearchVO searchVO = new AiPromptTemplateSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setTemplateCode("live_opening");

        AiPromptTemplate template = new AiPromptTemplate();
        template.setId(1L);
        template.setUserId(1L);
        template.setOwnerId(1L);
        template.setTemplateCode("live_opening");
        template.setTemplateName("直播开场模板");
        template.setVariantName("default");
        template.setIsActive(1);

        PageResultVO<AiPromptTemplate> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(template));

        when(promptTemplateService.search(any(AiPromptTemplateSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/prompt-template/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].templateName").value("直播开场模板"));
    }

    @Test
    @DisplayName("获取模板详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        AiPromptTemplate template = new AiPromptTemplate();
        template.setId(1L);
        template.setUserId(1L);
        template.setTemplateName("直播开场模板");
        template.setTemplateCode("live_opening");
        template.setSystemPrompt("你是一个专业的直播话术生成助手");
        template.setUserPromptTpl("生成一段{{style}}风格的开场白");

        when(promptTemplateService.getById(1L)).thenReturn(template);

        mockMvc.perform(post("/api/v1/ai/prompt-template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.templateName").value("直播开场模板"))
                .andExpect(jsonPath("$.data.templateCode").value("live_opening"));
    }

    @Test
    @DisplayName("保存模板（新增）- 应返回 200")
    void save_create_shouldReturn200() throws Exception {
        AiPromptTemplateSaveVO saveVO = new AiPromptTemplateSaveVO();
        saveVO.setTemplateName("新模板");
        saveVO.setTemplateCode("test_template");
        saveVO.setSystemPrompt("系统提示词");
        saveVO.setUserPromptTpl("用户提示词");

        AiPromptTemplate saved = new AiPromptTemplate();
        saved.setId(1L);
        saved.setUserId(1L);
        saved.setTemplateName("新模板");

        when(promptTemplateService.save(any(AiPromptTemplateSaveVO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/ai/prompt-template/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("保存模板（更新）- 应返回 200")
    void save_update_shouldReturn200() throws Exception {
        AiPromptTemplateSaveVO saveVO = new AiPromptTemplateSaveVO();
        saveVO.setId(1L);
        saveVO.setTemplateName("更新模板");
        saveVO.setTemplateCode("test_template");

        AiPromptTemplate saved = new AiPromptTemplate();
        saved.setId(1L);
        saved.setUserId(1L);
        saved.setTemplateName("更新模板");

        when(promptTemplateService.save(any(AiPromptTemplateSaveVO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/ai/prompt-template/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除模板 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        AiPromptTemplate existing = new AiPromptTemplate();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setOwnerId(1L);

        when(promptTemplateService.getById(1L)).thenReturn(existing);
        doNothing().when(promptTemplateService).delete(1L);

        mockMvc.perform(post("/api/v1/ai/prompt-template/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除他人模板 - 应返回 2002")
    void delete_notOwner_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        AiPromptTemplate existing = new AiPromptTemplate();
        existing.setId(1L);
        existing.setUserId(2L);
        existing.setOwnerId(2L);

        when(promptTemplateService.getById(1L)).thenReturn(existing);

        mockMvc.perform(post("/api/v1/ai/prompt-template/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("无权删除此模板"));
    }

    @Test
    @DisplayName("获取当前激活模板 - 应返回 200")
    void getActive_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateCode", "live_opening");
        body.put("variantName", "default");

        AiPromptTemplate template = new AiPromptTemplate();
        template.setId(1L);
        template.setTemplateCode("live_opening");
        template.setVariantName("default");
        template.setIsActive(1);

        when(promptTemplateService.getActiveTemplate(eq("live_opening"), eq("default"), eq(1L)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/ai/prompt-template/get-active")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.templateCode").value("live_opening"));
    }

    @Test
    @DisplayName("测试渲染模板 - 应返回 200")
    void testRender_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateId", 1L);
        Map<String, String> variables = new HashMap<>();
        variables.put("style", "专业");
        variables.put("product", "护肤品");
        body.put("variables", variables);

        AiPromptTemplate template = new AiPromptTemplate();
        template.setId(1L);
        template.setSystemPrompt("你是一个{{style}}的助手");
        template.setUserPromptTpl("生成关于{{product}}的介绍");

        when(promptTemplateService.getById(1L)).thenReturn(template);

        mockMvc.perform(post("/api/v1/ai/prompt-template/test-render")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.systemPrompt").value("你是一个专业的助手"))
                .andExpect(jsonPath("$.data.userPrompt").value("生成关于护肤品的介绍"));
    }

    @Test
    @DisplayName("测试渲染不存在的模板 - 应返回 1005")
    void testRender_notFound_shouldReturn1005() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateId", 999L);
        body.put("variables", new HashMap<>());

        when(promptTemplateService.getById(999L)).thenReturn(null);

        mockMvc.perform(post("/api/v1/ai/prompt-template/test-render")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1005))
                .andExpect(jsonPath("$.message").value("模板不存在"));
    }
}
