package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptTemplateService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptTemplateSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptTemplateVO;
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
@DisplayName("SvScriptTemplateController 集成测试")
class SvScriptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvScriptTemplateService svScriptTemplateService;

    @Test
    @DisplayName("查询脚本模板 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        SvScriptTemplateSearchVO searchVO = new SvScriptTemplateSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        SvScriptTemplateVO template = new SvScriptTemplateVO();
        template.setId(1L);
        template.setTemplateName("测试模板");

        PageResultVO<SvScriptTemplateVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(template));

        when(svScriptTemplateService.search(any(SvScriptTemplateSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/script-template/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("查询脚本模板（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/script-template/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取模板详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        SvScriptTemplateVO template = new SvScriptTemplateVO();
        template.setId(1L);
        template.setTemplateName("测试模板");

        when(svScriptTemplateService.getById(eq(1L), eq(1L))).thenReturn(template);

        mockMvc.perform(post("/api/v1/short-video/script-template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取模板详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/script-template/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存脚本模板 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SvScriptTemplateSaveVO saveVO = new SvScriptTemplateSaveVO();
        saveVO.setTemplateName("新模板");
        saveVO.setContent("模板内容");
        saveVO.setScene("product_intro");

        when(svScriptTemplateService.save(any(SvScriptTemplateSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/script-template/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存脚本模板（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        SvScriptTemplateSaveVO saveVO = new SvScriptTemplateSaveVO();
        saveVO.setTemplateName("新模板");
        saveVO.setContent("模板内容");

        mockMvc.perform(post("/api/v1/short-video/script-template/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除脚本模板 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(svScriptTemplateService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/script-template/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除脚本模板（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/script-template/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("增加使用次数 - 应返回 204")
    void incrementUseCount_shouldReturn204() throws Exception {
        doNothing().when(svScriptTemplateService).incrementUseCount(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/script-template/use-count")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("增加使用次数（未登录）- 应返回 2001")
    void incrementUseCount_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/script-template/use-count")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("按场景获取模板 - 应返回 200")
    void listByScene_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("scene", "product_intro");

        SvScriptTemplateVO template = new SvScriptTemplateVO();
        template.setId(1L);
        template.setScene("product_intro");

        when(svScriptTemplateService.listByScene(eq("product_intro"), eq(1L)))
                .thenReturn(List.of(template));

        mockMvc.perform(post("/api/v1/short-video/script-template/by-scene")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("按场景获取模板（缺少 scene）- 应返回 1001")
    void listByScene_missingScene_shouldReturn1001() throws Exception {
        Map<String, String> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/script-template/by-scene")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("按场景获取模板（未登录）- 应返回 2001")
    void listByScene_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("scene", "product_intro");

        mockMvc.perform(post("/api/v1/short-video/script-template/by-scene")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
