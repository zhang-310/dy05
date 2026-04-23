package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import cn.gaifan.douyinOperations.module.product.vo.ProductVO;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ProductController 集成测试")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private cn.gaifan.douyinOperations.module.product.service.impl.ProductServiceImpl productService;

    @MockBean
    private cn.gaifan.douyinOperations.module.product.service.impl.SalesHistoryServiceImpl salesHistoryService;

    @MockBean
    private cn.gaifan.douyinOperations.module.product.service.PaipingImportService paipingImportService;

    @MockBean
    private cn.gaifan.douyinOperations.module.product.service.ProductLinkExtractService productLinkExtractService;

    @MockBean
    private cn.gaifan.douyinOperations.contract.auth.DataScopeResolver dataScopeService;

    @Test
    @DisplayName("搜索商品 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 30);

        PageResultVO<ProductVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(productService.search(any())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/product/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("搜索商品（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/product/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取商品详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        ProductVO product = new ProductVO();
        product.setId(1L);
        product.setProductName("测试商品");

        when(productService.getById(eq(1L))).thenReturn(product);

        mockMvc.perform(post("/api/v1/product/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.productName").value("测试商品"));
    }

    @Test
    @DisplayName("推断商品类型 - 应返回 200")
    void inferProductType_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        when(productService.inferProductType(eq(1L))).thenReturn("hot,profit");

        mockMvc.perform(post("/api/v1/product/infer-product-type")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("hot,profit"));
    }

    @Test
    @DisplayName("保存商品 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        body.put("productName", "新商品");
        body.put("price", 99.99);

        when(productService.save(any())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/product/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除商品 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(productService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/product/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("批量删除商品 - 应返回 200")
    void batchDelete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", List.of(1L, 2L, 3L));

        doNothing().when(productService).batchDelete(anyList());

        mockMvc.perform(post("/api/v1/product/batch-delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("更新库存 - 应返回 204")
    void updateInventory_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("quantity", 100L);

        doNothing().when(productService).updateInventory(eq(1L), eq(100L));

        mockMvc.perform(post("/api/v1/product/update-inventory")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("发布商品 - 应返回 204")
    void publish_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(productService).publish(eq(1L));

        mockMvc.perform(post("/api/v1/product/publish")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("下架商品 - 应返回 204")
    void unpublish_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(productService).unpublish(eq(1L));

        mockMvc.perform(post("/api/v1/product/unpublish")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("设置精选商品 - 应返回 204")
    void setFeatured_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("featured", 1);

        doNothing().when(productService).setFeatured(eq(1L), eq(1));

        mockMvc.perform(post("/api/v1/product/set-featured")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }
}
