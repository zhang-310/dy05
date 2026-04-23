package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarVO;
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
@DisplayName("SvContentCalendarController 集成测试")
class SvContentCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvContentCalendarService calendarService;

    @Test
    @DisplayName("分页列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        SvContentCalendarSearchVO searchVO = new SvContentCalendarSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        SvContentCalendarVO calendar = new SvContentCalendarVO();
        calendar.setId(1L);
        calendar.setPlanDate("2026-04-10");

        PageResultVO<SvContentCalendarVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(calendar));

        when(calendarService.list(any(SvContentCalendarSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/content-calendar/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        SvContentCalendarVO calendar = new SvContentCalendarVO();
        calendar.setId(1L);
        calendar.setPlanDate("2026-04-10");

        when(calendarService.get(eq(1L), eq(1L))).thenReturn(calendar);

        mockMvc.perform(post("/api/v1/short-video/content-calendar/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        Map<String, Long> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/content-calendar/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("保存 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SvContentCalendarSaveVO saveVO = new SvContentCalendarSaveVO();
        saveVO.setPlanDate("2026-04-10");
        saveVO.setContentType("video");
        saveVO.setPersonaId(1L);

        when(calendarService.save(any(SvContentCalendarSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/content-calendar/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(calendarService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/content-calendar/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除（缺少 id）- 应返回 1001")
    void delete_missingId_shouldReturn1001() throws Exception {
        Map<String, Long> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/content-calendar/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("按日期范围 - 应返回 200")
    void dateRange_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("from", "2026-04-01");
        body.put("to", "2026-04-30");
        body.put("personaId", 1L);

        SvContentCalendarVO calendar = new SvContentCalendarVO();
        calendar.setId(1L);
        calendar.setPlanDate("2026-04-10");

        when(calendarService.listByDateRange(eq("2026-04-01"), eq("2026-04-30"), eq(1L), eq(1L)))
                .thenReturn(List.of(calendar));

        mockMvc.perform(post("/api/v1/short-video/content-calendar/date-range")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("按日期范围（缺少 from）- 应返回 1001")
    void dateRange_missingFrom_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("to", "2026-04-30");

        mockMvc.perform(post("/api/v1/short-video/content-calendar/date-range")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("AI 自动排期 - 应返回 200")
    void autoGenerate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("personaId", 1L);
        body.put("from", "2026-04-01");
        body.put("to", "2026-04-30");

        when(calendarService.autoGenerate(eq(1L), eq("2026-04-01"), eq("2026-04-30"), eq(1L)))
                .thenReturn(10);

        mockMvc.perform(post("/api/v1/short-video/content-calendar/auto-generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(10));
    }

    @Test
    @DisplayName("AI 自动排期（缺少 personaId）- 应返回 1001")
    void autoGenerate_missingPersonaId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("from", "2026-04-01");
        body.put("to", "2026-04-30");

        mockMvc.perform(post("/api/v1/short-video/content-calendar/auto-generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
