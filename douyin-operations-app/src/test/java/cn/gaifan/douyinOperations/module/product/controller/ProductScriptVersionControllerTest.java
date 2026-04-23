package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptVersionService;
import cn.gaifan.douyinOperations.module.product.vo.*;
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

/**
 * ProductScriptVersionController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ProductScriptVersionController 集成测试")
class ProductScriptVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductScriptVersionService versionService;

    @Test
    @DisplayName("保存话术版本 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        ProductScriptVersionSaveVO vo = new ProductScriptVersionSaveVO();
        vo.setProductId(1L);
        vo.setContent("测试话术内容");

        ProductScriptVersionVO result = new ProductScriptVersionVO();
        result.setId(1L);

        when(versionService.save(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/product/script-version/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("查询话术版本列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        ProductScriptVersionSearchVO vo = new ProductScriptVersionSearchVO();
        vo.setPage(0);
        vo.setRows(30);

        PageResultVO<ProductScriptVersionVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(versionService.list(any(), eq(1L))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/product/script-version/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("搜索话术版本 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        PageResultVO<ProductScriptVersionVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(5L);
        pageResult.setList(List.of());

        when(versionService.search(eq("测试"), eq("professional"), eq(80.0), eq(0), eq(30), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/product/script-version/search")
                        .param("keyword", "测试")
                        .param("style", "professional")
                        .param("minScore", "80.0")
                        .param("page", "0")
                        .param("rows", "30")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    @DisplayName("获取版本详情 - 应返回 200")
    void getDetail_shouldReturn200() throws Exception {
        ProductScriptVersionVO result = new ProductScriptVersionVO();
        result.setId(1L);
        result.setContent("v1.0");

        when(versionService.getDetail(eq(1L), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/product/script-version/detail/1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.content").value("v1.0"));
    }

    @Test
    @DisplayName("获取推荐版本 - 应返回 200")
    void recommend_shouldReturn200() throws Exception {
        ProductScriptRecommendVO result = new ProductScriptRecommendVO();

        when(versionService.recommend(eq(1L), eq(5), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/product/script-version/recommend")
                        .param("productId", "1")
                        .param("topN", "5")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("引用到直播场次 - 应返回 200")
    void applyFromLibrary_shouldReturn200() throws Exception {
        ProductScriptSnapshotVO result = new ProductScriptSnapshotVO();
        result.setId(1L);

        when(versionService.applyFromLibrary(eq(1L), eq(1L), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/product/script-version/apply-from-library")
                        .param("liveSessionId", "1")
                        .param("productScriptVersionId", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("删除版本 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        when(versionService.delete(eq(1L), eq(1L))).thenReturn(true);

        mockMvc.perform(post("/api/v1/product/script-version/delete/1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("更新版本状态 - 应返回 200")
    void updateStatus_shouldReturn200() throws Exception {
        ProductScriptVersionVO result = new ProductScriptVersionVO();
        result.setId(1L);
        result.setIsActive(true);

        when(versionService.updateStatus(eq(1L), eq(true), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/product/script-version/update-status")
                        .param("id", "1")
                        .param("isActive", "true")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取产品的所有版本 - 应返回 200")
    void listByProductId_shouldReturn200() throws Exception {
        when(versionService.listByProductId(eq(1L), eq(1L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/script-version/list-by-product")
                        .param("productId", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
