package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfileStats;
import cn.gaifan.douyinOperations.module.douyin.service.FanProfileService;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileQueryVO;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("FanProfileController 集成测试")
class FanProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FanProfileService fanProfileService;

    @Test
    @DisplayName("获取粉丝画像 - 应返回 200")
    void getFanProfile_shouldReturn200() throws Exception {
        FanProfileQueryVO queryVO = new FanProfileQueryVO();
        queryVO.setAccountId(1L);

        FanProfileVO profileVO = new FanProfileVO();
        profileVO.setAccountId(1L);
        profileVO.setTotalFans(1000L);

        when(fanProfileService.getFanProfile(eq(1L), eq(1L)))
                .thenReturn(profileVO);

        mockMvc.perform(post("/api/v1/douyin/fan-profile/get")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accountId").value(1));
    }

    @Test
    @DisplayName("获取粉丝画像（未登录）- 应返回 2001")
    void getFanProfile_unauthorized_shouldReturn2001() throws Exception {
        FanProfileQueryVO queryVO = new FanProfileQueryVO();
        queryVO.setAccountId(1L);

        mockMvc.perform(post("/api/v1/douyin/fan-profile/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取统计数据 - 应返回 200")
    void getStats_shouldReturn200() throws Exception {
        FanProfileQueryVO queryVO = new FanProfileQueryVO();
        queryVO.setAccountId(1L);
        queryVO.setStatType("gender");

        DyFanProfileStats stats = new DyFanProfileStats();
        stats.setAccountId(1L);
        stats.setStatType("gender");

        when(fanProfileService.getStats(eq(1L), eq("gender"), eq(1L)))
                .thenReturn(List.of(stats));

        mockMvc.perform(post("/api/v1/douyin/fan-profile/stats")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].accountId").value(1));
    }

    @Test
    @DisplayName("获取统计数据（未登录）- 应返回 2001")
    void getStats_unauthorized_shouldReturn2001() throws Exception {
        FanProfileQueryVO queryVO = new FanProfileQueryVO();
        queryVO.setAccountId(1L);

        mockMvc.perform(post("/api/v1/douyin/fan-profile/stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("手动同步粉丝画像 - 应返回 204")
    void manualSync_shouldReturn204() throws Exception {
        doNothing().when(fanProfileService).manualSync(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/douyin/fan-profile/sync/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("手动同步粉丝画像（未登录）- 应返回 2001")
    void manualSync_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/fan-profile/sync/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
