package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShootingTaskService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskVO;
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
@DisplayName("ShortVideoShootingTaskController 集成测试")
class ShortVideoShootingTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvShootingTaskService shootingTaskService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @Test
    @DisplayName("拍摄任务分页列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        SvShootingTaskSearchVO vo = new SvShootingTaskSearchVO();
        vo.setPage(0);
        vo.setRows(10);

        SvShootingTaskVO task = new SvShootingTaskVO();
        task.setId(1L);
        task.setTitle("拍摄任务1");

        PageResultVO<SvShootingTaskVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(task));

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(shootingTaskService.search(any(SvShootingTaskSearchVO.class), eq(1L), anyList()))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/shooting-task/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("拍摄任务分页列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/shooting-task/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("拍摄任务详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        SvShootingTaskVO task = new SvShootingTaskVO();
        task.setId(1L);
        task.setTitle("拍摄任务1");

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(shootingTaskService.get(eq(1L), eq(1L), anyList())).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/shooting-task/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("拍摄任务详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/shooting-task/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("保存拍摄任务 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SvShootingTaskSaveVO vo = new SvShootingTaskSaveVO();
        vo.setTitle("新拍摄任务");
        vo.setShootDate("2026-04-10");
        vo.setAnchorUserId(2L);

        when(shootingTaskService.save(any(SvShootingTaskSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/shooting-task/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存拍摄任务（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        SvShootingTaskSaveVO vo = new SvShootingTaskSaveVO();
        vo.setTitle("新拍摄任务");
        vo.setShootDate("2026-04-10");

        mockMvc.perform(post("/api/v1/short-video/shooting-task/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除拍摄任务 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(shootingTaskService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/shooting-task/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除拍摄任务（缺少 id）- 应返回 1001")
    void delete_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/shooting-task/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
