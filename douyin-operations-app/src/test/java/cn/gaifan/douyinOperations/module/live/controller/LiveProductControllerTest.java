package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveProductService;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveProductController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveProductController 集成测试")
class LiveProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveProductService liveProductService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @MockBean
    private LiveSessionRepository liveSessionRepository;

    @BeforeEach
    void setUp() {
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
        when(liveSessionRepository.findIdsByUserIdIn(anyList())).thenReturn(List.of(1L, 2L));
    }

    @Test
    @DisplayName("查询直播产品 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        LiveProductSearchVO searchVO = new LiveProductSearchVO();
        searchVO.setSessionId(1L);
        searchVO.setPage(0);
        searchVO.setRows(10);

        LiveProductVO productVO = new LiveProductVO();
        productVO.setId(1L);
        productVO.setSessionId(1L);
        productVO.setProductId(100L);
        productVO.setProductName("测试产品");
        productVO.setSaleQuantity(50);
        productVO.setRevenue(new BigDecimal("5000.00"));
        productVO.setPosition(1);

        PageResultVO<LiveProductVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(productVO));

        when(liveProductService.search(any(LiveProductSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/product/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(1))
                .andExpect(jsonPath("$.data.list[0].productName").value("测试产品"));
    }

    @Test
    @DisplayName("获取产品详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        LiveProductVO productVO = new LiveProductVO();
        productVO.setId(1L);
        productVO.setSessionId(1L);
        productVO.setProductId(100L);
        productVO.setProductName("测试产品");
        productVO.setSaleQuantity(50);
        productVO.setRevenue(new BigDecimal("5000.00"));

        when(liveProductService.getById(1L)).thenReturn(productVO);

        mockMvc.perform(post("/api/v1/live/product/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productName").value("测试产品"));
    }

    @Test
    @DisplayName("保存产品 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveProductSaveVO saveVO = new LiveProductSaveVO();
        saveVO.setSessionId(1L);
        saveVO.setProductId(100L);
        saveVO.setProductName("新产品");
        saveVO.setSaleQuantity(0);
        saveVO.setPosition(1);

        when(liveProductService.save(any(LiveProductSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/product/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除产品 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(liveProductService).delete(1L);

        mockMvc.perform(post("/api/v1/live/product/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取场次的产品列表 - 应返回 200")
    void getBySessionId_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        LiveProductVO productVO1 = new LiveProductVO();
        productVO1.setId(1L);
        productVO1.setSessionId(1L);
        productVO1.setProductId(100L);
        productVO1.setProductName("产品1");

        LiveProductVO productVO2 = new LiveProductVO();
        productVO2.setId(2L);
        productVO2.setSessionId(1L);
        productVO2.setProductId(101L);
        productVO2.setProductName("产品2");

        when(liveProductService.getBySessionId(1L)).thenReturn(List.of(productVO1, productVO2));

        mockMvc.perform(post("/api/v1/live/product/by-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productName").value("产品1"))
                .andExpect(jsonPath("$.data[1].productName").value("产品2"));
    }

    @Test
    @DisplayName("批量排序产品 - 应返回 200")
    void batchSort_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);
        body.put("productIds", List.of(3L, 1L, 2L));

        doNothing().when(liveProductService).batchSort(eq(1L), anyList());

        mockMvc.perform(post("/api/v1/live/product/batch-sort")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("排序成功"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        LiveProductSearchVO searchVO = new LiveProductSearchVO();

        mockMvc.perform(post("/api/v1/live/product/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
