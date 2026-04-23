package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.module.system.service.TaxonomyService;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("TaxonomyController 集成测试")
class TaxonomyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaxonomyService taxonomyService;

    @Test
    @DisplayName("列出分类 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("moduleScope", "product");
        request.put("parentId", null);

        Map<String, Object> taxonomy = new HashMap<>();
        taxonomy.put("id", 1L);
        taxonomy.put("name", "商品分类");

        when(taxonomyService.list(eq(1L), eq("product"), isNull()))
                .thenReturn(List.of(taxonomy));

        mockMvc.perform(post("/api/v1/system/taxonomy/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("列出分类（空 body）- 应返回 200")
    void list_emptyBody_shouldReturn200() throws Exception {
        when(taxonomyService.list(eq(1L), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/system/taxonomy/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("列出分类（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/taxonomy/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存分类 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "新分类");
        request.put("moduleScope", "product");

        when(taxonomyService.save(eq(1L), eq(true), any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/system/taxonomy/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存分类（非 admin）- 应返回 200")
    void save_nonAdmin_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "新分类");

        when(taxonomyService.save(eq(1L), eq(false), any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/system/taxonomy/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("保存分类（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "新分类");

        mockMvc.perform(post("/api/v1/system/taxonomy/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除分类 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("id", 1L);

        doNothing().when(taxonomyService).delete(eq(1L), eq(true), eq(1L));

        mockMvc.perform(post("/api/v1/system/taxonomy/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除分类（非 admin）- 应返回 204")
    void delete_nonAdmin_shouldReturn204() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("id", 1L);

        doNothing().when(taxonomyService).delete(eq(1L), eq(false), eq(1L));

        mockMvc.perform(post("/api/v1/system/taxonomy/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除分类（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("id", 1L);

        mockMvc.perform(post("/api/v1/system/taxonomy/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
