package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvRemakeTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.service.RemakeTemplateService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSaveVO;
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
 * RemakeTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("RemakeTemplateController 集成测试")
class RemakeTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RemakeTemplateService remakeTemplateService;

    @Test
    @DisplayName("分页查询二创模板 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        RemakeTemplateSearchVO vo = new RemakeTemplateSearchVO();
        vo.setPage(0);
        vo.setRows(10);

        SvRemakeTemplate template = new SvRemakeTemplate();
        template.setId(1L);
        template.setTemplateName("爆款模板1");

        PageResultVO<SvRemakeTemplate> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(template));

        when(remakeTemplateService.search(any(RemakeTemplateSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/remake-template/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(1));
    }

    @Test
    @DisplayName("分页查询二创模板（无参数）- 应返回 200")
    void list_noParams_shouldReturn200() throws Exception {
        PageResultVO<SvRemakeTemplate> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(remakeTemplateService.search(any(RemakeTemplateSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/remake-template/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("新增二创模板 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        RemakeTemplateSaveVO vo = new RemakeTemplateSaveVO();
        vo.setTemplateName("新模板");
        vo.setRemakeType("form_copy");

        SvRemakeTemplate template = new SvRemakeTemplate();
        template.setId(1L);
        template.setTemplateName("新模板");

        when(remakeTemplateService.save(any(RemakeTemplateSaveVO.class), eq(1L)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/short-video/remake-template/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("删除二创模板 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(remakeTemplateService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/remake-template/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除二创模板（缺少 id）- 应返回 400")
    void delete_missingId_shouldReturn400() throws Exception {
        Map<String, Long> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/remake-template/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("从爆款分析结果生成模板 - 应返回 200")
    void createFromViral_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);
        body.put("remakeType", "form_imitation");

        SvRemakeTemplate template = new SvRemakeTemplate();
        template.setId(1L);
        template.setTemplateName("爆款模板");

        when(remakeTemplateService.createFromViralAnalysis(eq(1L), eq("form_imitation"), eq(1L)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/short-video/remake-template/create-from-viral")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("从爆款分析结果生成模板（默认 remakeType）- 应返回 200")
    void createFromViral_defaultRemakeType_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);

        SvRemakeTemplate template = new SvRemakeTemplate();
        template.setId(1L);
        template.setTemplateName("爆款模板");

        when(remakeTemplateService.createFromViralAnalysis(eq(1L), eq("form_copy"), eq(1L)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/short-video/remake-template/create-from-viral")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("从爆款分析结果生成模板（缺少 viralVideoId）- 应返回 400")
    void createFromViral_missingViralVideoId_shouldReturn400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("remakeType", "form_copy");

        mockMvc.perform(post("/api/v1/short-video/remake-template/create-from-viral")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("基于模板生成脚本 - 应返回 200")
    void generate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateId", 1L);
        Map<String, String> variables = new HashMap<>();
        variables.put("product", "护肤霜");
        variables.put("benefit", "保湿");
        body.put("variables", variables);

        String generatedScript = "这款护肤霜具有保湿功效...";

        when(remakeTemplateService.generateFromTemplate(eq(1L), eq(variables), eq(1L)))
                .thenReturn(generatedScript);

        mockMvc.perform(post("/api/v1/short-video/remake-template/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(generatedScript));
    }

    @Test
    @DisplayName("基于模板生成脚本（无变量）- 应返回 200")
    void generate_noVariables_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateId", 1L);

        String generatedScript = "默认脚本内容...";

        when(remakeTemplateService.generateFromTemplate(eq(1L), eq(Map.of()), eq(1L)))
                .thenReturn(generatedScript);

        mockMvc.perform(post("/api/v1/short-video/remake-template/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(generatedScript));
    }

    @Test
    @DisplayName("基于模板生成脚本（缺少 templateId）- 应返回 400")
    void generate_missingTemplateId_shouldReturn400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("variables", Map.of("product", "护肤霜"));

        mockMvc.perform(post("/api/v1/short-video/remake-template/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("分页查询二创模板（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/remake-template/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
