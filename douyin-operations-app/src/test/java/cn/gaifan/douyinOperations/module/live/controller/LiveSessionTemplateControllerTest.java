package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionTemplateService;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateVO;
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

import java.sql.Timestamp;
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
 * LiveSessionTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveSessionTemplateController 集成测试")
class LiveSessionTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveSessionTemplateService templateService;

    @Test
    @DisplayName("分页查询模板 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        LiveSessionTemplateSearchVO searchVO = new LiveSessionTemplateSearchVO();

        LiveSessionTemplateVO templateVO = new LiveSessionTemplateVO();
        templateVO.setId(1L);
        templateVO.setOwnerId(1L);
        templateVO.setName("标准模板");
        templateVO.setCode("STANDARD");
        templateVO.setDescription("标准直播场次模板");
        templateVO.setStructureJson("[{\"scriptType\":\"opening\"}]");
        templateVO.setCreateTime(new Timestamp(System.currentTimeMillis()));

        PageResultVO<LiveSessionTemplateVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(templateVO));

        when(templateService.search(any(LiveSessionTemplateSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/session-template/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("标准模板"))
                .andExpect(jsonPath("$.data.list[0].code").value("STANDARD"));
    }

    @Test
    @DisplayName("分页查询模板（空 body）- 应返回 200")
    void search_withEmptyBody_shouldReturn200() throws Exception {
        PageResultVO<LiveSessionTemplateVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(templateService.search(any(LiveSessionTemplateSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/session-template/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("获取模板详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        LiveSessionTemplateVO templateVO = new LiveSessionTemplateVO();
        templateVO.setId(1L);
        templateVO.setOwnerId(1L);
        templateVO.setName("标准模板");
        templateVO.setCode("STANDARD");
        templateVO.setStructureJson("[{\"scriptType\":\"opening\"}]");

        when(templateService.getById(eq(1L), eq(1L))).thenReturn(templateVO);

        mockMvc.perform(post("/api/v1/live/session-template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("标准模板"));
    }

    @Test
    @DisplayName("获取模板详情（缺少 id）- 应返回 1001")
    void get_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/session-template/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("保存模板 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveSessionTemplateSaveVO saveVO = new LiveSessionTemplateSaveVO();
        saveVO.setName("新模板");
        saveVO.setCode("NEW_TEMPLATE");
        saveVO.setDescription("新建模板");
        saveVO.setStructureJson("[{\"scriptType\":\"opening\"},{\"scriptType\":\"product\"}]");

        when(templateService.save(any(LiveSessionTemplateSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/session-template/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除模板 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(templateService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/session-template/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除模板（缺少 id）- 应返回 1001")
    void delete_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/session-template/delete")
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
        mockMvc.perform(post("/api/v1/live/session-template/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
