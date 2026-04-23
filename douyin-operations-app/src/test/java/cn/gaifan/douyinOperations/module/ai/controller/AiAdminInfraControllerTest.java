package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.AiAdminInfraService;
import cn.gaifan.douyinOperations.module.ai.service.AiAdminInfraService.InfraHealthItem;
import cn.gaifan.douyinOperations.module.ai.vo.InfraSearchVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiAdminInfraController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiAdminInfraController 集成测试")
class AiAdminInfraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAdminInfraService aiAdminInfraService;

    @Test
    @DisplayName("监控配置 - 应返回 200")
    void getMonitoringConfig_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/infra/monitoring-config")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("基础设施健康检查 - 应返回 200")
    void health_shouldReturn200() throws Exception {
        InfraHealthItem item1 = new InfraHealthItem("Milvus", true, "Connected");
        InfraHealthItem item2 = new InfraHealthItem("Elasticsearch", true, "Connected");

        when(aiAdminInfraService.checkHealth()).thenReturn(List.of(item1, item2));

        mockMvc.perform(post("/api/v1/ai/admin/infra/health")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].component").value("Milvus"))
                .andExpect(jsonPath("$.data[0].ok").value(true));
    }

    @Test
    @DisplayName("基础设施详情 - 应返回 200")
    void getInfraDetail_shouldReturn200() throws Exception {
        Map<String, Object> detail = new HashMap<>();
        detail.put("postgresql", Map.of("version", "15.3", "connections", 10));
        detail.put("milvus", Map.of("version", "2.6", "collections", 5));

        when(aiAdminInfraService.getInfraDetail()).thenReturn(detail);

        mockMvc.perform(post("/api/v1/ai/admin/infra/detail")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.postgresql").exists())
                .andExpect(jsonPath("$.data.milvus").exists());
    }

    @Test
    @DisplayName("PostgreSQL 文档分页 - 应返回 200")
    void pagePgDocuments_shouldReturn200() throws Exception {
        InfraSearchVO vo = new InfraSearchVO();
        vo.setKbId(1L);
        vo.setKeyword("test");
        vo.setPage(0);
        vo.setRows(30);

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", 1L);
        doc.put("content", "test document");

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(1L, List.of(doc), 0, 30);

        when(aiAdminInfraService.pagePgDocuments(eq(1L), eq("test"), eq(0), eq(30)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/admin/infra/documents/pg")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].content").value("test document"));
    }

    @Test
    @DisplayName("Elasticsearch 文档分页 - 应返回 200")
    void pageEsDocuments_shouldReturn200() throws Exception {
        InfraSearchVO vo = new InfraSearchVO();
        vo.setKbId(2L);
        vo.setKeyword("search");
        vo.setPage(0);
        vo.setRows(30);

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", 2L);
        doc.put("content", "search document");

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(1L, List.of(doc), 0, 30);

        when(aiAdminInfraService.pageEsDocuments(eq(2L), eq("search"), eq(0), eq(30)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/admin/infra/documents/es")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].content").value("search document"));
    }

    @Test
    @DisplayName("Milvus 向量统计 - 应返回 200")
    void getMilvusStats_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("kbId", 1L);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVectors", 10000);
        stats.put("collections", 5);

        when(aiAdminInfraService.getMilvusStats(eq(1L))).thenReturn(stats);

        mockMvc.perform(post("/api/v1/ai/admin/infra/milvus/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalVectors").value(10000))
                .andExpect(jsonPath("$.data.collections").value(5));
    }

    @Test
    @DisplayName("Milvus 向量统计（无 kbId）- 应返回 200")
    void getMilvusStats_withoutKbId_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVectors", 50000);

        when(aiAdminInfraService.getMilvusStats(isNull())).thenReturn(stats);

        mockMvc.perform(post("/api/v1/ai/admin/infra/milvus/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalVectors").value(50000));
    }

    @Test
    @DisplayName("缓存命中率统计 - 应返回 200")
    void getCacheStats_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.85);
        stats.put("totalRequests", 10000);

        when(aiAdminInfraService.getCacheStats()).thenReturn(stats);

        mockMvc.perform(post("/api/v1/ai/admin/infra/cache/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.hitRate").value(0.85))
                .andExpect(jsonPath("$.data.totalRequests").value(10000));
    }

    @Test
    @DisplayName("检索性能统计 - 应返回 200")
    void getSearchStats_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("p50", 50);
        stats.put("p99", 200);
        stats.put("qps", 100);

        when(aiAdminInfraService.getSearchStats()).thenReturn(stats);

        mockMvc.perform(post("/api/v1/ai/admin/infra/search/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.p50").value(50))
                .andExpect(jsonPath("$.data.p99").value(200))
                .andExpect(jsonPath("$.data.qps").value(100));
    }

    @Test
    @DisplayName("索引队列分页 - 应返回 200")
    void pageIndexQueue_shouldReturn200() throws Exception {
        InfraSearchVO vo = new InfraSearchVO();
        vo.setKbId(1L);
        vo.setStatus("pending");
        vo.setKeyword("doc");
        vo.setPage(0);
        vo.setRows(30);

        Map<String, Object> queueItem = new HashMap<>();
        queueItem.put("id", 1L);
        queueItem.put("status", "pending");
        queueItem.put("docName", "document.pdf");

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(1L, List.of(queueItem), 0, 30);

        when(aiAdminInfraService.pageIndexQueue(eq(1L), eq("pending"), eq("doc"), eq(0), eq(30)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/admin/infra/queue")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("pending"));
    }

    @Test
    @DisplayName("非管理员访问 - 应返回 2002")
    void asUser_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/infra/health")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2002")
    void withoutAuth_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/infra/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }
}
