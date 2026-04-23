package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.KbImportReport;
import cn.gaifan.douyinOperations.module.ai.config.ImportJobStore;
import cn.gaifan.douyinOperations.module.ai.service.ImportProgress;
import cn.gaifan.douyinOperations.module.ai.service.ImportRequirementsService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseImportService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.vo.DedupPreviewVO;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("KnowledgeBaseController 集成测试")
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeBaseService knowledgeBaseService;

    @MockBean
    private KnowledgeBaseImportService knowledgeBaseImportService;

    @MockBean
    private ImportRequirementsService importRequirementsService;

    @MockBean
    private ImportJobStore importJobStore;

    @Test
    @DisplayName("创建知识库 - 应返回 200")
    void createKnowledgeBase_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("name", "测试知识库");
        body.put("description", "测试描述");

        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setKbName("测试知识库");

        when(knowledgeBaseService.createKnowledgeBase(anyString(), anyString(), anyLong()))
                .thenReturn(kb);

        mockMvc.perform(post("/api/v1/ai/knowledge-base/create")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("创建知识库（未登录）- 应返回 2001")
    void createKnowledgeBase_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("name", "测试知识库");

        mockMvc.perform(post("/api/v1/ai/knowledge-base/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取知识库列表 - 应返回 200")
    void listKnowledgeBases_shouldReturn200() throws Exception {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setKbName("测试知识库");

        when(knowledgeBaseService.listKnowledgeBases(anyLong()))
                .thenReturn(List.of(kb));

        mockMvc.perform(post("/api/v1/ai/knowledge-base/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("上传文档 - 应返回 200")
    void uploadDocument_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("title", "测试文档");
        body.put("content", "测试内容");
        body.put("fileType", "text");
        body.put("contentType", "auto");

        AiKbDocument doc = new AiKbDocument();
        doc.setId(1L);
        doc.setTitle("测试文档");

        when(knowledgeBaseService.uploadDocument(anyLong(), anyString(), anyString(), anyString(), anyLong(), any(), anyString()))
                .thenReturn(doc);

        mockMvc.perform(post("/api/v1/ai/knowledge-base/1/document")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取文档列表 - 应返回 200")
    void listDocuments_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<AiKbDocument> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(knowledgeBaseService.pageDocuments(anyLong(), anyLong(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/knowledge-base/1/documents")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("混合搜索 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("query", "测试查询");
        body.put("topK", 10);

        when(knowledgeBaseService.hybridSearch(anyLong(), anyString(), anyInt(), anyLong(),
                nullable(String.class), eq(false), eq(true)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/knowledge-base/1/search")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("检查导入前置依赖 - 应返回 200")
    void checkImportRequirements_shouldReturn200() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("milvus", "available");

        when(importRequirementsService.checkImportRequirements())
                .thenReturn(requirements);

        mockMvc.perform(post("/api/v1/ai/knowledge-base/import-requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("异步导入 - 应返回 200")
    void importFromPathAsync_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sourcePath", "/test/path");
        body.put("kbId", 1);
        body.put("autoClassify", true);

        String jobId = "test-job-123";
        ImportProgress progress = new ImportProgress();

        when(importJobStore.createJob()).thenReturn(jobId);
        when(importJobStore.get(jobId)).thenReturn(progress);

        mockMvc.perform(post("/api/v1/ai/knowledge-base/import-from-path-async")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.jobId").value(jobId));
    }

    @Test
    @DisplayName("获取导入进度 - 应返回 200")
    void getImportStatus_shouldReturn200() throws Exception {
        String jobId = "test-job-123";
        ImportProgress progress = new ImportProgress();
        progress.setTotal(100);

        when(importJobStore.get(jobId)).thenReturn(progress);

        mockMvc.perform(post("/api/v1/ai/knowledge-base/import-status/" + jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.phase").value("running"));
    }

    @Test
    @DisplayName("提交知识反馈 - 应返回 200")
    void submitFeedback_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("docId", 1L);
        body.put("query", "测试查询");
        body.put("rating", 1);
        body.put("comment", "很好");
        body.put("searchMode", "hybrid");

        mockMvc.perform(post("/api/v1/ai/knowledge-base/feedback")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("去重预览 - 应返回 200")
    void dedupPreview_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("content", "测试内容");
        body.put("contentType", "auto");

        DedupPreviewVO preview = new DedupPreviewVO();

        when(knowledgeBaseService.dedupPreview(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(preview);

        mockMvc.perform(post("/api/v1/ai/knowledge-base/1/dedup-preview")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取文档分块列表 - 应返回 200")
    void getDocumentChunks_shouldReturn200() throws Exception {
        when(knowledgeBaseService.getDocumentChunks(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/knowledge-base/1/documents/1/chunks")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("导入报告列表 - 应返回 200")
    void listImportReports_shouldReturn200() throws Exception {
        KbImportReport report = new KbImportReport();
        report.setId(1L);

        when(knowledgeBaseService.listImportReports(anyLong(), anyLong()))
                .thenReturn(List.of(report));

        mockMvc.perform(post("/api/v1/ai/knowledge-base/1/import-reports")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除知识库 - 应返回 200")
    void deleteKnowledgeBase_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/v1/ai/knowledge-base/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除文档 - 应返回 200")
    void deleteDocument_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/v1/ai/knowledge-base/document/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
