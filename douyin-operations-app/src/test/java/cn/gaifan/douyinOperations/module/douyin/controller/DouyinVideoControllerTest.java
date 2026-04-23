package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinVideoService;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinVideoSaveVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinVideoSearchVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinVideoVO;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DouyinVideoController 集成测试")
class DouyinVideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DouyinVideoService douyinVideoService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @MockBean
    private DouyinAccountRepository douyinAccountRepository;

    @Test
    @DisplayName("查询视频 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        DouyinVideoSearchVO searchVO = new DouyinVideoSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        DouyinVideoVO videoVO = new DouyinVideoVO();
        videoVO.setId(1L);
        videoVO.setTitle("测试视频");

        PageResultVO<DouyinVideoVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(videoVO));

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user")))
                .thenReturn(List.of(1L));
        when(douyinAccountRepository.findIdsByUserIdIn(anyList()))
                .thenReturn(List.of(1L));
        when(douyinVideoService.search(any(DouyinVideoSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/douyin/video/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("查询视频（空 body）- 应返回 200")
    void search_emptyBody_shouldReturn200() throws Exception {
        PageResultVO<DouyinVideoVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user")))
                .thenReturn(List.of(1L));
        when(douyinAccountRepository.findIdsByUserIdIn(anyList()))
                .thenReturn(List.of(1L));
        when(douyinVideoService.search(any(DouyinVideoSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/douyin/video/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("查询视频（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        DouyinVideoSearchVO searchVO = new DouyinVideoSearchVO();

        mockMvc.perform(post("/api/v1/douyin/video/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取视频详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        DouyinVideoVO videoVO = new DouyinVideoVO();
        videoVO.setId(1L);
        videoVO.setTitle("测试视频");

        when(douyinVideoService.getVideo(eq(1L)))
                .thenReturn(videoVO);

        mockMvc.perform(post("/api/v1/douyin/video/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取视频详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/video/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存视频 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        DouyinVideoSaveVO saveVO = new DouyinVideoSaveVO();
        saveVO.setTitle("新视频");
        saveVO.setAccountId(1L);
        saveVO.setVideoId("vid123");

        when(douyinVideoService.saveVideo(any(DouyinVideoSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/douyin/video/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存视频（未登录）- 应返回 1001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        DouyinVideoSaveVO saveVO = new DouyinVideoSaveVO();
        saveVO.setTitle("新视频");

        mockMvc.perform(post("/api/v1/douyin/video/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("同步视频 - 应返回 204")
    void sync_shouldReturn200() throws Exception {
        DouyinAccount account = new DouyinAccount();
        account.setId(1L);
        account.setUserId(1L);

        when(douyinAccountRepository.findByIdAndDeleted(eq(1L), eq(0)))
                .thenReturn(Optional.of(account));
        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user")))
                .thenReturn(List.of(1L));
        doNothing().when(douyinVideoService).syncVideos(eq(1L));

        mockMvc.perform(post("/api/v1/douyin/video/sync")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("同步视频（无权限）- 应返回 2002")
    void sync_forbidden_shouldReturn2002() throws Exception {
        DouyinAccount account = new DouyinAccount();
        account.setId(1L);
        account.setUserId(2L);

        when(douyinAccountRepository.findByIdAndDeleted(eq(1L), eq(0)))
                .thenReturn(Optional.of(account));
        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user")))
                .thenReturn(List.of(1L));

        mockMvc.perform(post("/api/v1/douyin/video/sync")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("同步视频（未登录）- 应返回 2001")
    void sync_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/video/sync")
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
