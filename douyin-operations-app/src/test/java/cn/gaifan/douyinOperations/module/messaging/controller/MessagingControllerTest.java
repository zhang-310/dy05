package cn.gaifan.douyinOperations.module.messaging.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingPlatformService;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSearchVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSaveVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("MessagingController 集成测试")
class MessagingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessagingPlatformService messagingPlatformService;

    @Test
    @DisplayName("配置列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        MsgPlatformConfigSearchVO searchVO = new MsgPlatformConfigSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        MsgPlatformConfigVO config = new MsgPlatformConfigVO();
        config.setId(1L);
        config.setPlatform("feishu");
        config.setAppId("test-app-id");

        PageResultVO<MsgPlatformConfigVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(config));

        when(messagingPlatformService.search(any(MsgPlatformConfigSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/messaging/config/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("配置列表（空 body）- 应返回 200")
    void list_emptyBody_shouldReturn200() throws Exception {
        PageResultVO<MsgPlatformConfigVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(messagingPlatformService.search(any(MsgPlatformConfigSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/messaging/config/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("配置列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        MsgPlatformConfigSearchVO searchVO = new MsgPlatformConfigSearchVO();

        mockMvc.perform(post("/api/v1/messaging/config/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("配置详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        MsgPlatformConfigVO config = new MsgPlatformConfigVO();
        config.setId(1L);
        config.setPlatform("feishu");
        config.setAppId("test-app-id");

        when(messagingPlatformService.getById(eq(1L))).thenReturn(config);

        mockMvc.perform(post("/api/v1/messaging/config/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("配置详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/messaging/config/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存配置 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        MsgPlatformConfigSaveVO saveVO = new MsgPlatformConfigSaveVO();
        saveVO.setPlatform("feishu");
        saveVO.setAppId("test-app-id");
        saveVO.setSecret("test-secret");

        when(messagingPlatformService.save(any(MsgPlatformConfigSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/messaging/config/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存配置（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        MsgPlatformConfigSaveVO saveVO = new MsgPlatformConfigSaveVO();
        saveVO.setPlatform("feishu");

        mockMvc.perform(post("/api/v1/messaging/config/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除配置 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(messagingPlatformService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/messaging/config/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除配置（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/messaging/config/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
