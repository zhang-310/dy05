package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkAccountService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkAccountSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkAccountVO;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("BenchmarkAccountController 集成测试")
class BenchmarkAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BenchmarkAccountService benchmarkAccountService;

    @Test
    @DisplayName("已登录时应使用请求上下文 userId")
    void list_shouldUseRequestUserId() throws Exception {
        BenchmarkAccountSearchVO searchVO = new BenchmarkAccountSearchVO();
        BenchmarkAccountVO item = new BenchmarkAccountVO();
        item.setId(1L);
        item.setAccountName("对标账号");
        when(benchmarkAccountService.search(org.mockito.ArgumentMatchers.any(BenchmarkAccountSearchVO.class), org.mockito.ArgumentMatchers.eq(9L)))
                .thenReturn(PageResultVO.of(1L, List.of(item), 0, 20));

        mockMvc.perform(post("/api/v1/benchmark/account/list")
                        .requestAttr("userId", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("未登录时不再默认回退到 1L")
    void list_shouldReturnUnauthorizedWhenNoUserContext() throws Exception {
        BenchmarkAccountSearchVO searchVO = new BenchmarkAccountSearchVO();

        mockMvc.perform(post("/api/v1/benchmark/account/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
