package cn.gaifan.douyinOperations.module.abtest.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.abtest.service.AbTestService;
import cn.gaifan.douyinOperations.module.abtest.service.ScriptStyleAbService;
import cn.gaifan.douyinOperations.module.abtest.vo.*;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AbTestController 集成测试")
class AbTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AbTestService abTestService;

    @MockBean
    private ScriptStyleAbService scriptStyleAbService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @Test
    @DisplayName("实验列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        AbExperimentSearchVO searchVO = new AbExperimentSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        AbExperimentVO experimentVO = new AbExperimentVO();
        experimentVO.setId(1L);
        experimentVO.setName("测试实验");

        PageResultVO<AbExperimentVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(experimentVO));

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user")))
                .thenReturn(List.of(1L));
        when(abTestService.search(any(AbExperimentSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/abtest/experiment/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("实验列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        AbExperimentSearchVO searchVO = new AbExperimentSearchVO();

        mockMvc.perform(post("/api/v1/abtest/experiment/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取实验详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        AbExperimentVO experimentVO = new AbExperimentVO();
        experimentVO.setId(1L);
        experimentVO.setName("测试实验");

        when(abTestService.getById(eq(1L)))
                .thenReturn(experimentVO);

        mockMvc.perform(post("/api/v1/abtest/experiment/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取实验详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/abtest/experiment/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存实验 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        AbExperimentSaveVO saveVO = new AbExperimentSaveVO();
        saveVO.setName("新实验");
        saveVO.setDescription("测试描述");
        saveVO.setExperimentType("script_style");

        when(abTestService.save(any(AbExperimentSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/abtest/experiment/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));

        verify(abTestService).save(argThat(vo -> vo.getOwnerId() == null && "新实验".equals(vo.getName())), eq(1L));
    }

    @Test
    @DisplayName("保存实验（未登录）- 应返回 1001")
    void save_unauthorized_shouldReturn1001() throws Exception {
        AbExperimentSaveVO saveVO = new AbExperimentSaveVO();
        saveVO.setName("新实验");

        mockMvc.perform(post("/api/v1/abtest/experiment/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除实验 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(abTestService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/abtest/experiment/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除实验（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/abtest/experiment/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("更新实验状态 - 应返回 204")
    void updateStatus_shouldReturn204() throws Exception {
        doNothing().when(abTestService).updateStatus(eq(1L), eq(1));

        mockMvc.perform(post("/api/v1/abtest/experiment/update-status")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新实验状态（未登录）- 应返回 2001")
    void updateStatus_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/abtest/experiment/update-status")
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("设置获胜变体 - 应返回 204")
    void setWinner_shouldReturn204() throws Exception {
        AbSetWinnerVO winnerVO = new AbSetWinnerVO();
        winnerVO.setExperimentId(1L);
        winnerVO.setVariantId(2L);

        doNothing().when(abTestService).setWinner(any(AbSetWinnerVO.class));

        mockMvc.perform(post("/api/v1/abtest/experiment/set-winner")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(winnerVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("设置获胜变体（未登录）- 应返回 2001")
    void setWinner_unauthorized_shouldReturn2001() throws Exception {
        AbSetWinnerVO winnerVO = new AbSetWinnerVO();
        winnerVO.setExperimentId(1L);
        winnerVO.setVariantId(2L);

        mockMvc.perform(post("/api/v1/abtest/experiment/set-winner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(winnerVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存变体 - 应返回 200")
    void saveVariant_shouldReturn200() throws Exception {
        AbVariantSaveVO saveVO = new AbVariantSaveVO();
        saveVO.setExperimentId(1L);
        saveVO.setVariantName("变体A");
        saveVO.setVariantType("A");

        when(abTestService.saveVariant(any(AbVariantSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/abtest/variant/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存变体（未登录）- 应返回 2001")
    void saveVariant_unauthorized_shouldReturn2001() throws Exception {
        AbVariantSaveVO saveVO = new AbVariantSaveVO();
        saveVO.setExperimentId(1L);
        saveVO.setVariantName("变体A");
        saveVO.setVariantType("A");

        mockMvc.perform(post("/api/v1/abtest/variant/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除变体 - 应返回 204")
    void deleteVariant_shouldReturn204() throws Exception {
        doNothing().when(abTestService).deleteVariant(eq(1L));

        mockMvc.perform(post("/api/v1/abtest/variant/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除变体（未登录）- 应返回 2001")
    void deleteVariant_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/abtest/variant/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("记录事件 - 应返回 204")
    void recordEvent_shouldReturn204() throws Exception {
        AbEventSaveVO eventVO = new AbEventSaveVO();
        eventVO.setExperimentId(1L);
        eventVO.setVariantId(2L);
        eventVO.setEventType("click");
        eventVO.setUserFingerprint("user123");

        doNothing().when(abTestService).recordEvent(any(AbEventSaveVO.class));

        mockMvc.perform(post("/api/v1/abtest/event/record")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("记录事件（未登录）- 应返回 1001")
    void recordEvent_unauthorized_shouldReturn1001() throws Exception {
        AbEventSaveVO eventVO = new AbEventSaveVO();
        eventVO.setExperimentId(1L);
        eventVO.setVariantId(2L);
        eventVO.setEventType("click");

        mockMvc.perform(post("/api/v1/abtest/event/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("获取实验统计结果 - 应返回 200")
    void getExperimentResult_shouldReturn200() throws Exception {
        AbExperimentStatisticsVO statisticsVO = new AbExperimentStatisticsVO();
        statisticsVO.setExperimentId(1L);

        when(abTestService.getExperimentStatistics(eq(1L)))
                .thenReturn(statisticsVO);

        mockMvc.perform(post("/api/v1/abtest/experiment/result")
                        .requestAttr("userId", 1L)
                        .param("experimentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.experimentId").value(1));
    }

    @Test
    @DisplayName("获取实验统计结果（未登录）- 应返回 2001")
    void getExperimentResult_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/abtest/experiment/result")
                        .param("experimentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取实验日趋势 - 应返回 200")
    void getDailyTrend_shouldReturn200() throws Exception {
        AbDailyTrendVO trendVO = new AbDailyTrendVO();
        trendVO.setDate(LocalDate.now());

        when(abTestService.getDailyTrend(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(trendVO));

        mockMvc.perform(post("/api/v1/abtest/experiment/daily-trend")
                        .requestAttr("userId", 1L)
                        .param("experimentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("获取实验日趋势（未登录）- 应返回 2001")
    void getDailyTrend_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/abtest/experiment/daily-trend")
                        .param("experimentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("分配话术风格 - 应返回 200")
    void assignStyle_shouldReturn200() throws Exception {
        ScriptStyleAssignRequest request = new ScriptStyleAssignRequest();
        request.setTargetEntityType("live");
        request.setTargetEntityId(1L);

        ScriptStyleAssignVO assignVO = new ScriptStyleAssignVO();
        assignVO.setExperimentId(1L);
        assignVO.setVariantId(2L);
        assignVO.setStyleCode("style_a");
        assignVO.setVariantType("A");

        when(scriptStyleAbService.assignStyle(eq(1L), eq("live"), eq(1L), anyString()))
                .thenReturn(assignVO);

        mockMvc.perform(post("/api/v1/abtest/script-style/assign")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.styleCode").value("style_a"));
    }

    @Test
    @DisplayName("分配话术风格（未登录）- 应返回 2001")
    void assignStyle_unauthorized_shouldReturn2001() throws Exception {
        ScriptStyleAssignRequest request = new ScriptStyleAssignRequest();
        request.setTargetEntityType("live");
        request.setTargetEntityId(1L);

        mockMvc.perform(post("/api/v1/abtest/script-style/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("记录话术风格转化 - 应返回 204")
    void recordConversion_shouldReturn204() throws Exception {
        ScriptStyleConversionRequest request = new ScriptStyleConversionRequest();
        request.setExperimentId(1L);
        request.setVariantId(2L);

        doNothing().when(scriptStyleAbService).recordConversion(eq(1L), eq(2L), anyString());

        mockMvc.perform(post("/api/v1/abtest/script-style/record-conversion")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("记录话术风格转化（未登录）- 应返回 2001")
    void recordConversion_unauthorized_shouldReturn2001() throws Exception {
        ScriptStyleConversionRequest request = new ScriptStyleConversionRequest();
        request.setExperimentId(1L);
        request.setVariantId(2L);

        mockMvc.perform(post("/api/v1/abtest/script-style/record-conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
