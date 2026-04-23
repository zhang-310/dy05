package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.*;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LiveScriptController 集成测试
 * 测试直播话术管理的核心功能：搜索、详情、保存、删除、执行状态
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptController 集成测试")
class LiveScriptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptService liveScriptService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @MockBean
    private LiveSessionRepository liveSessionRepository;

    @BeforeEach
    void setUp() {
        // Mock 数据权限服务
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
        when(liveSessionRepository.findIdsByUserIdIn(anyList())).thenReturn(List.of(1L, 2L));
    }

    @Test
    @DisplayName("查询直播话术 - 200")
    void search_shouldReturn200() throws Exception {
        LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
        searchVO.setSessionId(1L);

        LiveScriptVO scriptVO = new LiveScriptVO();
        scriptVO.setId(1L);
        scriptVO.setSessionId(1L);
        scriptVO.setScriptContent("欢迎来到直播间");
        scriptVO.setSequenceNo(1);
        scriptVO.setExecuted(0);

        PageResultVO<LiveScriptVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(scriptVO));

        when(liveScriptService.search(any(LiveScriptSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/script/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].scriptContent").value("欢迎来到直播间"));
    }

    @Test
    @DisplayName("获取话术详情 - 200")
    void get_shouldReturn200() throws Exception {
        LiveScriptVO scriptVO = new LiveScriptVO();
        scriptVO.setId(1L);
        scriptVO.setSessionId(1L);
        scriptVO.setScriptContent("欢迎来到直播间");
        scriptVO.setSequenceNo(1);

        when(liveScriptService.getById(1L)).thenReturn(scriptVO);

        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/live/script/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.scriptContent").value("欢迎来到直播间"));
    }

    @Test
    @DisplayName("保存话术 - 200")
    void save_shouldReturn200() throws Exception {
        LiveScriptSaveVO saveVO = new LiveScriptSaveVO();
        saveVO.setSessionId(1L);
        saveVO.setScriptContent("新话术内容");
        saveVO.setSequenceNo(1);
        saveVO.setExecuted(0);

        when(liveScriptService.save(any(LiveScriptSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/script/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除话术 - 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/live/script/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取场次的话术列表 - 200")
    void getBySessionId_shouldReturn200() throws Exception {
        LiveScriptVO scriptVO1 = new LiveScriptVO();
        scriptVO1.setId(1L);
        scriptVO1.setScriptContent("开场白");

        LiveScriptVO scriptVO2 = new LiveScriptVO();
        scriptVO2.setId(2L);
        scriptVO2.setScriptContent("互动话术");

        when(liveScriptService.getBySessionId(1L)).thenReturn(List.of(scriptVO1, scriptVO2));

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        mockMvc.perform(post("/api/v1/live/script/by-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].scriptContent").value("开场白"));
    }

    @Test
    @DisplayName("话术效果排行 - 200")
    void getEffectiveness_shouldReturn200() throws Exception {
        LiveScriptVO scriptVO = new LiveScriptVO();
        scriptVO.setId(1L);
        scriptVO.setScriptContent("高效话术");

        when(liveScriptService.getEffectiveness(1L)).thenReturn(List.of(scriptVO));

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        mockMvc.perform(post("/api/v1/live/script/effectiveness")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("更新话术执行状态 - 204")
    void updateExecuted_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("executed", 1);

        mockMvc.perform(post("/api/v1/live/script/executed")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("保存话术到话术库 - 200")
    void saveToLibrary_shouldReturn200() throws Exception {
        when(liveScriptService.saveToLibrary(eq(1L), eq(1L))).thenReturn(100L);

        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);

        mockMvc.perform(post("/api/v1/live/script/save-to-library")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("批量保存话术到话术库 - 200")
    void saveBatchToLibrary_shouldReturn200() throws Exception {
        when(liveScriptService.saveBatchToLibrary(eq(1L), anyList(), eq(1L))).thenReturn(5);

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);
        body.put("scriptIds", List.of(1L, 2L, 3L, 4L, 5L));

        mockMvc.perform(post("/api/v1/live/script/save-batch-to-library")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("导出场次话术 - 200")
    void exportScripts_shouldReturn200() throws Exception {
        String exportedText = "话术1\n话术2\n话术3";
        when(liveScriptService.exportScripts(1L)).thenReturn(exportedText);

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        mockMvc.perform(post("/api/v1/live/script/export")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(exportedText));
    }

    @Test
    @DisplayName("未登录访问 - 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/live/script/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
