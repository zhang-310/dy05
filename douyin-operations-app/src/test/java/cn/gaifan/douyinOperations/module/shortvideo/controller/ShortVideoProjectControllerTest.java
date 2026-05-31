package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyShootService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShortVideoProjectController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoProjectController 集成测试")
class ShortVideoProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvProjectService projectService;

    @MockBean
    private DailyShootService dailyShootService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @Test
    @DisplayName("项目列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        SvProjectSearchVO vo = new SvProjectSearchVO();
        vo.setPage(0);
        vo.setRows(30);

        PageResultVO<SvProjectVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(projectService.search(any(), eq(1L), anyList())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/project/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("普通用户项目列表 - 应只传入当前用户可见 ownerId")
    void list_userScope_shouldUseVisibleOwnerIds() throws Exception {
        SvProjectSearchVO vo = new SvProjectSearchVO();
        vo.setPage(0);
        vo.setRows(8);

        PageResultVO<SvProjectVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of());

        when(dataScopeService.getVisibleUserIds(eq(9L), eq("user"))).thenReturn(List.of(9L));
        when(projectService.search(any(), eq(9L), eq(List.of(9L)))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/project/list")
                        .requestAttr("userId", 9L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(dataScopeService).getVisibleUserIds(9L, "user");
        verify(projectService).search(any(SvProjectSearchVO.class), eq(9L), eq(List.of(9L)));
    }

    @Test
    @DisplayName("项目详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        SvProjectVO project = new SvProjectVO();
        project.setId(1L);
        project.setTitle("测试项目");

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(projectService.get(eq(1L), eq(1L), anyList())).thenReturn(project);

        mockMvc.perform(post("/api/v1/short-video/project/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("测试项目"));
    }

    @Test
    @DisplayName("保存项目 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SvProjectSaveVO vo = new SvProjectSaveVO();
        vo.setTitle("新项目");
        vo.setProjectType("daily");

        when(projectService.save(any(), eq(1L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/project/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除项目 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(projectService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/project/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("AI 生成每日拍摄脚本 - 应返回 200")
    void generateDaily_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("personaId", 1L);
        body.put("scheduleDate", "2026-04-07");
        body.put("count", 3);

        Map<String, Object> result = Map.of("generated", 3);

        when(dailyShootService.generateDaily(eq(1L), eq(1L), eq("2026-04-07"), eq(3), isNull(), isNull(), isNull()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/project/generate-daily")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.generated").value(3));
    }

    @Test
    @DisplayName("每日脚本列表 - 应返回 200")
    void dailyList_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scheduleDate", "2026-04-07");
        body.put("personaId", 1L);

        when(dailyShootService.dailyList(eq(1L), eq("2026-04-07"), eq(1L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/short-video/project/daily-list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("更新拍摄状态 - 应返回 200")
    void updateShootStatus_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        body.put("shootStatus", "shooting");

        doNothing().when(dailyShootService).updateShootStatus(eq(1L), eq(1L), eq("shooting"));

        mockMvc.perform(post("/api/v1/short-video/project/update-shoot-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("导出拍摄脚本 - 应返回 200")
    void exportScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);

        when(dailyShootService.exportScript(eq(1L), eq(1L))).thenReturn("脚本内容");

        mockMvc.perform(post("/api/v1/short-video/project/export-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("脚本内容"));
    }

    @Test
    @DisplayName("项目列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        SvProjectSearchVO vo = new SvProjectSearchVO();

        mockMvc.perform(post("/api/v1/short-video/project/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取项目详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/project/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
