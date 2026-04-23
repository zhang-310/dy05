package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.AccountVideoCollectService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskVO;
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
 * AccountVideoCollectController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AccountVideoCollectController 集成测试")
class AccountVideoCollectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountVideoCollectService accountVideoCollectService;

    @MockBean(name = "accountCollectAsyncRunner")
    private Object accountCollectAsyncRunner;

    @Test
    @DisplayName("发起采集任务 - 应返回 200")
    void start_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("accountUrl", "https://www.douyin.com/user/123456");
        body.put("maxVideos", 100);

        AccountCollectTaskVO task = new AccountCollectTaskVO();
        task.setId(1L);
        task.setStatus("running");
        task.setAccountUrl("https://www.douyin.com/user/123456");

        when(accountVideoCollectService.startCollect(any(), eq(1L))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/account-collect/start")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("running"));
    }

    @Test
    @DisplayName("任务列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 20);

        AccountCollectTaskVO task = new AccountCollectTaskVO();
        task.setId(1L);
        task.setStatus("completed");

        PageResultVO<AccountCollectTaskVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(task));

        when(accountVideoCollectService.listTasks(any(), eq(1L))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/account-collect/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("任务状态 - 应返回 200")
    void status_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);

        AccountCollectTaskVO task = new AccountCollectTaskVO();
        task.setId(1L);
        task.setStatus("running");
        task.setCollectedVideos(50);

        when(accountVideoCollectService.getTaskStatus(eq(1L), eq(1L))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/account-collect/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.collectedVideos").value(50));
    }

    @Test
    @DisplayName("任务状态（缺少 taskId）- 应返回 1001")
    void status_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/account-collect/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("取消任务 - 应返回 200")
    void cancel_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);

        doNothing().when(accountVideoCollectService).cancelTask(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/account-collect/cancel")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("重试失败任务 - 应返回 200")
    void retry_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);

        AccountCollectTaskVO task = new AccountCollectTaskVO();
        task.setId(1L);
        task.setStatus("running");

        when(accountVideoCollectService.retryTask(eq(1L), eq(1L))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/account-collect/retry")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("running"));
    }

    @Test
    @DisplayName("对选中视频执行深度分析 - 应返回 200")
    void analyzeSelected_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);
        body.put("viralVideoIds", List.of(1L, 2L, 3L));

        AccountCollectTaskVO task = new AccountCollectTaskVO();
        task.setId(1L);
        task.setStatus("analyzing");

        when(accountVideoCollectService.analyzeSelected(eq(1L), anyList(), eq(1L))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/account-collect/analyze-selected")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("analyzing"));
    }

    @Test
    @DisplayName("对选中视频执行深度分析（未选择视频）- 应返回 1001")
    void analyzeSelected_emptyVideoIds_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);
        body.put("viralVideoIds", List.of());

        mockMvc.perform(post("/api/v1/short-video/account-collect/analyze-selected")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("任务下的视频列表 - 应返回 200")
    void videos_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);
        body.put("page", 0);
        body.put("rows", 20);

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of(
                Map.of("id", 1L, "title", "视频1", "views", 10000)
        ));

        when(accountVideoCollectService.listTaskVideos(eq(1L), eq(1L), eq(0), eq(20), isNull()))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/account-collect/videos")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("删除任务 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);

        doNothing().when(accountVideoCollectService).deleteTask(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/account-collect/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("任务列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/account-collect/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
