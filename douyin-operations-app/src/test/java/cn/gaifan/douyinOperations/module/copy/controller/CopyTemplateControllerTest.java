package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.service.CopyTemplateService;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTemplateSaveVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTemplateSearchVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTemplateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CopyTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CopyTemplateController 集成测试")
class CopyTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CopyTemplateService copyTemplateService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @BeforeEach
    void setUp() {
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("分页搜索模板 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        CopyTemplateSearchVO searchVO = new CopyTemplateSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setKeyword("开场白");

        CopyTemplateVO templateVO = new CopyTemplateVO();
        templateVO.setId(1L);
        templateVO.setUserId(1L);
        templateVO.setTemplateName("直播开场白模板");
        templateVO.setTemplateContent("欢迎来到直播间...");
        templateVO.setCategory("直播");
        templateVO.setDescription("标准开场白");
        templateVO.setStatus(1);
        templateVO.setCreateTime(new Timestamp(System.currentTimeMillis()));
        templateVO.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        PageResultVO<CopyTemplateVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(templateVO));

        when(copyTemplateService.search(any(CopyTemplateSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/copy/template/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].templateName").value("直播开场白模板"));
    }

    @Test
    @DisplayName("获取模板详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        CopyTemplateVO templateVO = new CopyTemplateVO();
        templateVO.setId(1L);
        templateVO.setTemplateName("直播开场白模板");
        templateVO.setTemplateContent("欢迎来到直播间...");

        when(copyTemplateService.getById(1L)).thenReturn(templateVO);

        mockMvc.perform(post("/api/v1/copy/template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.templateName").value("直播开场白模板"));
    }

    @Test
    @DisplayName("新建模板 - 应从登录态注入 userId 并返回 200")
    void save_shouldReturn200() throws Exception {
        CopyTemplateSaveVO saveVO = new CopyTemplateSaveVO();
        saveVO.setTemplateName("新模板");
        saveVO.setTemplateContent("模板内容");
        saveVO.setCategory("直播");
        saveVO.setDescription("测试模板");

        when(copyTemplateService.save(any(CopyTemplateSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/copy/template/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));

        verify(copyTemplateService).save(argThat(vo -> Long.valueOf(1L).equals(vo.getUserId())
                && "新模板".equals(vo.getTemplateName())
                && "模板内容".equals(vo.getTemplateContent())));
    }

    @Test
    @DisplayName("删除模板 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(copyTemplateService).delete(1L, 1L);

        mockMvc.perform(post("/api/v1/copy/template/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新模板状态 - 应返回 204")
    void updateStatus_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("status", 1);

        doNothing().when(copyTemplateService).updateStatus(1L, 1, 1L);

        mockMvc.perform(post("/api/v1/copy/template/update-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取模板详情缺少 id - 应返回 1001")
    void get_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/copy/template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        CopyTemplateSearchVO searchVO = new CopyTemplateSearchVO();

        mockMvc.perform(post("/api/v1/copy/template/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
