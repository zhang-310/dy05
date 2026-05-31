package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyShootService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotListVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoShotListController 集成测试")
class ShortVideoShotListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvShotListService shotListService;

    @MockBean
    private DailyShootService dailyShootService;

    @Test
    @DisplayName("分镜列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 20);

        SvShotListVO shotList = new SvShotListVO();
        shotList.setId(1L);
        shotList.setScriptId(1L);

        PageResultVO<SvShotListVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(shotList));

        when(shotListService.list(eq(1L), eq(0), eq(20))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/shot-list/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("分镜列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/shot-list/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("分镜详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        SvShotListVO shotList = new SvShotListVO();
        shotList.setId(1L);
        shotList.setScriptId(1L);

        when(shotListService.get(eq(1L), eq(1L))).thenReturn(shotList);

        mockMvc.perform(post("/api/v1/short-video/shot-list/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("分镜详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/shot-list/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("根据脚本获取最新分镜 - 应返回 200")
    void getByScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);

        SvShotListVO shotList = new SvShotListVO();
        shotList.setId(1L);
        shotList.setScriptId(1L);

        when(shotListService.getLatestByScriptId(eq(1L), eq(1L))).thenReturn(shotList);

        mockMvc.perform(post("/api/v1/short-video/shot-list/get-by-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(1));
    }

    @Test
    @DisplayName("根据脚本获取最新分镜（缺少 scriptId）- 应返回 1001")
    void getByScript_missingScriptId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/shot-list/get-by-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("保存分镜 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SvShotSaveVO vo = new SvShotSaveVO();
        vo.setShotListId(1L);

        when(shotListService.save(any(SvShotSaveVO.class), eq(1L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/shot-list/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存分镜（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        SvShotSaveVO vo = new SvShotSaveVO();
        vo.setShotListId(1L);

        mockMvc.perform(post("/api/v1/short-video/shot-list/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除单条分镜 - 应返回 200")
    void deleteShot_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("shotId", 9L);

        mockMvc.perform(post("/api/v1/short-video/shot-list/delete-shot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        verify(shotListService).deleteShot(9L, 1L);
    }

    @Test
    @DisplayName("删除单条分镜（缺少 shotId）- 应返回 1001")
    void deleteShot_missingShotId_shouldReturn1001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/shot-list/delete-shot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("AI 生成分镜 - 应返回 200")
    void generate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);
        body.put("scriptContent", "测试脚本内容");
        body.put("shotCount", 5);
        body.put("style", "cinematic");

        SvShotVO shot = new SvShotVO();
        shot.setShotNumber(1);
        shot.setSceneDescription("第一镜");

        SvShotListService.GenerateResult result = new SvShotListService.GenerateResult(
                List.of(shot),
                1L
        );

        when(shotListService.generateWithResult(eq(1L), eq("测试脚本内容"), eq(5), eq("cinematic"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/shot-list/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.shots").isArray())
                .andExpect(jsonPath("$.data.shotListId").value(1));
    }

    @Test
    @DisplayName("AI 生成分镜（缺少 scriptContent）- 应返回 1001")
    void generate_missingScriptContent_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);

        mockMvc.perform(post("/api/v1/short-video/shot-list/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("分镜审核 - 应返回 200")
    void review_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sceneId", 1L);
        body.put("reviewStatus", "approved");
        body.put("reviewerNote", "通过");

        Map<String, Object> reviewResult = new HashMap<>();
        reviewResult.put("success", true);

        when(dailyShootService.reviewShot(eq(1L), eq(1L), eq("approved"), eq("通过")))
                .thenReturn(reviewResult);

        mockMvc.perform(post("/api/v1/short-video/shot-list/review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("分镜审核（缺少 sceneId）- 应返回 1001")
    void review_missingSceneId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("reviewStatus", "approved");

        mockMvc.perform(post("/api/v1/short-video/shot-list/review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("分镜审核（无效 reviewStatus）- 应返回 1001")
    void review_invalidReviewStatus_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sceneId", 1L);
        body.put("reviewStatus", "invalid");

        mockMvc.perform(post("/api/v1/short-video/shot-list/review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
