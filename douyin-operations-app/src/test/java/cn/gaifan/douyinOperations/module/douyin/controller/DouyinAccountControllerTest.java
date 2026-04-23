package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinAccountService;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinAccountSaveVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinAccountSearchVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinAccountStatisticsVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinAccountVO;
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
@DisplayName("DouyinAccountController 集成测试")
class DouyinAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DouyinAccountService douyinAccountService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @Test
    @DisplayName("查询抖音账号 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        DouyinAccountSearchVO searchVO = new DouyinAccountSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(1L);
        accountVO.setAccountName("测试账号");

        PageResultVO<DouyinAccountVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(accountVO));

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user")))
                .thenReturn(List.of(1L));
        when(douyinAccountService.search(any(DouyinAccountSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/douyin/account/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("查询抖音账号（空 body）- 应返回 200")
    void search_emptyBody_shouldReturn200() throws Exception {
        PageResultVO<DouyinAccountVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user")))
                .thenReturn(List.of(1L));
        when(douyinAccountService.search(any(DouyinAccountSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/douyin/account/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("查询抖音账号（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        DouyinAccountSearchVO searchVO = new DouyinAccountSearchVO();

        mockMvc.perform(post("/api/v1/douyin/account/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取账号详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(1L);
        accountVO.setAccountName("测试账号");

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);

        mockMvc.perform(post("/api/v1/douyin/account/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取账号详情（管理员访问他人账号）- 应返回 200")
    void get_adminAccessOthers_shouldReturn200() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(2L);
        accountVO.setAccountName("他人账号");

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);

        mockMvc.perform(post("/api/v1/douyin/account/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取账号详情（非管理员访问他人账号）- 应返回 2002")
    void get_nonAdminAccessOthers_shouldReturn2002() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(2L);

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);

        mockMvc.perform(post("/api/v1/douyin/account/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("获取账号详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/account/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存账号 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        DouyinAccountSaveVO saveVO = new DouyinAccountSaveVO();
        saveVO.setAccountName("新账号");
        saveVO.setAccountId("dy123");

        when(douyinAccountService.saveAccount(any(DouyinAccountSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/douyin/account/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存账号（未登录）- 应返回 1001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        DouyinAccountSaveVO saveVO = new DouyinAccountSaveVO();
        saveVO.setAccountName("新账号");

        mockMvc.perform(post("/api/v1/douyin/account/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除账号 - 应返回 204")
    void delete_shouldReturn200() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(1L);

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);
        doNothing().when(douyinAccountService).deleteAccount(eq(1L));

        mockMvc.perform(post("/api/v1/douyin/account/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除账号（管理员删除他人账号）- 应返回 204")
    void delete_adminDeleteOthers_shouldReturn200() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(2L);

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);
        doNothing().when(douyinAccountService).deleteAccount(eq(1L));

        mockMvc.perform(post("/api/v1/douyin/account/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除账号（非管理员删除他人账号）- 应返回 2002")
    void delete_nonAdminDeleteOthers_shouldReturn2002() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(2L);

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);

        mockMvc.perform(post("/api/v1/douyin/account/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("删除账号（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/account/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取账号统计 - 应返回 200")
    void statistics_shouldReturn200() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(1L);

        DouyinAccountStatisticsVO statisticsVO = new DouyinAccountStatisticsVO();
        statisticsVO.setTotalVideos(50L);
        statisticsVO.setTotalLikes(1000L);

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);
        when(douyinAccountService.getAccountStatistics(eq(1L)))
                .thenReturn(statisticsVO);

        mockMvc.perform(post("/api/v1/douyin/account/statistics")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalVideos").value(50));
    }

    @Test
    @DisplayName("获取账号统计（管理员访问他人账号）- 应返回 200")
    void statistics_adminAccessOthers_shouldReturn200() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(2L);

        DouyinAccountStatisticsVO statisticsVO = new DouyinAccountStatisticsVO();
        statisticsVO.setTotalVideos(50L);

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);
        when(douyinAccountService.getAccountStatistics(eq(1L)))
                .thenReturn(statisticsVO);

        mockMvc.perform(post("/api/v1/douyin/account/statistics")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取账号统计（非管理员访问他人账号）- 应返回 2002")
    void statistics_nonAdminAccessOthers_shouldReturn2002() throws Exception {
        DouyinAccountVO accountVO = new DouyinAccountVO();
        accountVO.setId(1L);
        accountVO.setUserId(2L);

        when(douyinAccountService.getAccount(eq(1L)))
                .thenReturn(accountVO);

        mockMvc.perform(post("/api/v1/douyin/account/statistics")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("获取账号统计（未登录）- 应返回 2001")
    void statistics_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/account/statistics")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
