package cn.gaifan.douyinOperations.module.search.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.search.service.GlobalSearchService;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchRequestVO;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchResponseVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("GlobalSearchController 集成测试")
class GlobalSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GlobalSearchService globalSearchService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @Test
    @DisplayName("全局搜索 - 应返回 200")
    void globalSearch_shouldReturn200() throws Exception {
        GlobalSearchRequestVO requestVO = new GlobalSearchRequestVO();
        requestVO.setQ("测试");

        GlobalSearchResponseVO responseVO = new GlobalSearchResponseVO();

        when(dataScopeService.getVisibleUserIds(eq(1L), anyString()))
                .thenReturn(List.of(1L));
        when(globalSearchService.search(any(GlobalSearchRequestVO.class), anyList()))
                .thenReturn(responseVO);

        mockMvc.perform(post("/api/v1/search/global")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("全局搜索（未登录）- 应返回 2001")
    void globalSearch_unauthorized_shouldReturn2001() throws Exception {
        GlobalSearchRequestVO requestVO = new GlobalSearchRequestVO();
        requestVO.setQ("测试");

        mockMvc.perform(post("/api/v1/search/global")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
