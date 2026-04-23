package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.LiveCompetitorScriptService;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptVO;
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
 * LiveCompetitorScriptController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveCompetitorScriptController 集成测试")
class LiveCompetitorScriptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveCompetitorScriptService competitorScriptService;

    @Test
    @DisplayName("分页查询竞品话术 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        LiveCompetitorScriptSearchVO searchVO = new LiveCompetitorScriptSearchVO();

        LiveCompetitorScriptVO scriptVO = new LiveCompetitorScriptVO();
        scriptVO.setId(1L);
        scriptVO.setOwnerId(1L);
        scriptVO.setTitle("竞品A开场话术");
        scriptVO.setCompetitorName("竞品A");
        scriptVO.setPlatform("抖音");
        scriptVO.setScriptContent("欢迎来到直播间，今天给大家带来超值好物...");
        scriptVO.setSourceUrl("https://example.com/live/123");
        scriptVO.setTags("开场,促销");
        scriptVO.setCreateTime(new Timestamp(System.currentTimeMillis()));

        PageResultVO<LiveCompetitorScriptVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(scriptVO));

        when(competitorScriptService.search(any(LiveCompetitorScriptSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/competitor-script/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("竞品A开场话术"));
    }

    @Test
    @DisplayName("分页查询竞品话术（空 body）- 应返回 200")
    void search_withEmptyBody_shouldReturn200() throws Exception {
        PageResultVO<LiveCompetitorScriptVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(competitorScriptService.search(any(LiveCompetitorScriptSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/competitor-script/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("获取竞品话术详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        LiveCompetitorScriptVO scriptVO = new LiveCompetitorScriptVO();
        scriptVO.setId(1L);
        scriptVO.setOwnerId(1L);
        scriptVO.setTitle("竞品A开场话术");
        scriptVO.setCompetitorName("竞品A");
        scriptVO.setScriptContent("欢迎来到直播间，今天给大家带来超值好物...");

        when(competitorScriptService.getById(eq(1L), eq(1L))).thenReturn(scriptVO);

        mockMvc.perform(post("/api/v1/live/competitor-script/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("竞品A开场话术"));
    }

    @Test
    @DisplayName("获取竞品话术详情（缺少 id）- 应返回 1001")
    void get_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/competitor-script/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("保存竞品话术 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveCompetitorScriptSaveVO saveVO = new LiveCompetitorScriptSaveVO();
        saveVO.setTitle("竞品B产品介绍");
        saveVO.setCompetitorName("竞品B");
        saveVO.setPlatform("抖音");
        saveVO.setScriptContent("这款产品采用进口原料，品质保证...");
        saveVO.setSourceUrl("https://example.com/live/456");
        saveVO.setTags("产品介绍,卖点");
        saveVO.setNotes("值得参考的产品介绍方式");

        when(competitorScriptService.save(any(LiveCompetitorScriptSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/competitor-script/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除竞品话术 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(competitorScriptService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/competitor-script/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除竞品话术（缺少 id）- 应返回 1001")
    void delete_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/competitor-script/delete")
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
        mockMvc.perform(post("/api/v1/live/competitor-script/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
