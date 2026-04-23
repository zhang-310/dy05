package cn.gaifan.douyinOperations.module.log.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.service.OperationLogService;
import cn.gaifan.douyinOperations.module.log.service.SystemLogService;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogVO;
import cn.gaifan.douyinOperations.module.log.vo.SystemLogVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LogController 集成测试")
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OperationLogService operationLogService;

    @MockBean
    private SystemLogService systemLogService;

    @Test
    @DisplayName("查询操作日志 - 应返回 200")
    void operationPage_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<OperationLogVO> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(operationLogService.search(any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/log/operation/page")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("查询操作日志（未登录）- 应返回 2001")
    void operationPage_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        mockMvc.perform(post("/api/v1/log/operation/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("查询系统日志 - 应返回 200")
    void systemPage_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<SystemLogVO> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(systemLogService.search(any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/log/system/page")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("查询系统日志（未登录）- 应返回 2001")
    void systemPage_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        mockMvc.perform(post("/api/v1/log/system/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
