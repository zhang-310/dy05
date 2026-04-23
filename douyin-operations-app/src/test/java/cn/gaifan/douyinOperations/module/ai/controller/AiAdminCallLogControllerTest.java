package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.vo.CallLogSearchVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiAdminCallLogController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiAdminCallLogController 集成测试")
class AiAdminCallLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiCallLogService aiCallLogService;

    @Test
    @DisplayName("分页查询调用日志 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        CallLogSearchVO vo = new CallLogSearchVO();
        vo.setPage(0);
        vo.setRows(20);

        AiCallLog log1 = new AiCallLog();
        log1.setId(1L);
        log1.setUserId(1L);
        log1.setCallType("text_generation");

        AiCallLog log2 = new AiCallLog();
        log2.setId(2L);
        log2.setUserId(2L);
        log2.setCallType("image_generation");

        PageResultVO<AiCallLog> pageResult = new PageResultVO<>(2L, List.of(log1, log2), 0, 20);

        when(aiCallLogService.search(any(CallLogSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/admin/call-log/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].callType").value("text_generation"))
                .andExpect(jsonPath("$.data.list[1].callType").value("image_generation"));
    }

    @Test
    @DisplayName("分页查询调用日志（无参数）- 应返回 200")
    void search_withoutParams_shouldReturn200() throws Exception {
        PageResultVO<AiCallLog> pageResult = new PageResultVO<>(0L, List.of(), 0, 20);

        when(aiCallLogService.search(any(CallLogSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/admin/call-log/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("非管理员访问 - 应返回 2002")
    void asUser_shouldReturn2002() throws Exception {
        CallLogSearchVO vo = new CallLogSearchVO();

        mockMvc.perform(post("/api/v1/ai/admin/call-log/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2002")
    void withoutAuth_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/call-log/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }
}
