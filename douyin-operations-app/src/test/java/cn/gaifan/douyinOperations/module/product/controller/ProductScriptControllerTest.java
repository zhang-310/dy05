package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptService;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptShortVideoExportService;
import cn.gaifan.douyinOperations.module.product.service.ScriptVersionHistoryService;
import cn.gaifan.douyinOperations.module.product.vo.MultiStyleGenerateResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptExportToShortVideoResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptSaveVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductScriptController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ProductScriptController 集成测试")
class ProductScriptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductScriptService productScriptService;

    @MockBean
    private ScriptVersionHistoryService scriptVersionHistoryService;

    @MockBean
    private ProductScriptShortVideoExportService productScriptShortVideoExportService;

    @Test
    @DisplayName("保存产品话术 - 应返回 200")
    void saveScript_shouldReturn200() throws Exception {
        ProductScriptSaveVO vo = new ProductScriptSaveVO();
        vo.setProductId(1L);
        vo.setScriptType("formal");
        vo.setStyle("professional");
        vo.setScriptContent("测试话术内容");

        DyProductScript script = new DyProductScript();
        script.setId(1L);
        script.setProductId(1L);

        when(productScriptService.saveScript(any(), eq(1L))).thenReturn(script);

        mockMvc.perform(post("/api/v1/product/script/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取产品所有话术 - 应返回 200")
    void listScripts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        when(productScriptService.listScripts(eq(1L), eq(1L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/script/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("搜索产品话术 - 应返回 200")
    void searchScripts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        when(productScriptService.listScripts(eq(1L), eq(1L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/script/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取产品指定类型的话术 - 应返回 200")
    void listScriptsByType_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);
        body.put("scriptType", "formal");

        when(productScriptService.listScriptsByType(eq(1L), eq("formal"), eq(1L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/script/list-by-type")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("按风格分组列出话术 - 应返回 200")
    void listScriptsByStyle_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);
        body.put("scriptType", "formal");

        when(productScriptService.listScriptsByStyle(eq(1L), eq("formal"), eq(1L))).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/product/script/list-by-style")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("按风格获取各风格激活话术 - 应返回 200")
    void getActiveScriptsByStyle_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);
        body.put("scriptType", "formal");

        when(productScriptService.getActiveScriptsByStyle(eq(1L), eq("formal"), eq(1L))).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/product/script/active-by-style")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取产品的激活话术 - 应返回 200")
    void listActiveScripts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        when(productScriptService.listActiveScripts(eq(1L), eq(1L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/script/active")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("激活产品话术 - Void 成功响应应返回 200")
    void activateScript_shouldReturn200ForVoidSuccess() throws Exception {
        doNothing().when(productScriptService).activateScript(eq(8L), eq(1L));

        mockMvc.perform(put("/api/v1/product/script/activate/8")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除产品话术 - Void 成功响应应返回 200")
    void deleteScript_shouldReturn200ForVoidSuccess() throws Exception {
        doNothing().when(productScriptService).deleteScript(eq(8L), eq(1L));

        mockMvc.perform(delete("/api/v1/product/script/8")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取产品所有话术（未登录）- 应返回 2001")
    void listScripts_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        mockMvc.perform(post("/api/v1/product/script/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取产品所有话术（缺少 productId）- 应返回 1001")
    void listScripts_missingProductId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/product/script/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("商品话术导出短视频 - 应返回 scriptId 和 projectId")
    void exportToShortVideo_shouldReturnProjectAndScriptIds() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);
        body.put("style", "seeding");
        body.put("duration", 60);

        when(productScriptShortVideoExportService.exportToShortVideoProject(any(), eq(1L)))
                .thenReturn(new ProductScriptExportToShortVideoResultVO(10L, 100L, "商品转短视频·面霜"));

        mockMvc.perform(post("/api/v1/product/script/export-to-shortvideo")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(10))
                .andExpect(jsonPath("$.data.projectId").value(100))
                .andExpect(jsonPath("$.data.projectName").value("商品转短视频·面霜"));
    }
}
