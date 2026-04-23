package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeSource;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeSourceService;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSearchVO;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSaveVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KnowledgeSourceController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("KnowledgeSourceController 集成测试")
class KnowledgeSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeSourceService knowledgeSourceService;

    @Test
    @DisplayName("知识源分页查询 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        KnowledgeSourceSearchVO vo = new KnowledgeSourceSearchVO();
        vo.setPage(0);
        vo.setRows(10);

        AiKnowledgeSource source1 = new AiKnowledgeSource();
        source1.setId(1L);
        source1.setSourceName("产品知识库");
        source1.setSourceType("document");

        AiKnowledgeSource source2 = new AiKnowledgeSource();
        source2.setId(2L);
        source2.setSourceName("FAQ知识库");
        source2.setSourceType("qa");

        PageResultVO<AiKnowledgeSource> pageResult = new PageResultVO<>(2L, List.of(source1, source2), 0, 10);

        when(knowledgeSourceService.search(any(KnowledgeSourceSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge-source/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].sourceName").value("产品知识库"))
                .andExpect(jsonPath("$.data.list[1].sourceName").value("FAQ知识库"));
    }

    @Test
    @DisplayName("知识源详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        AiKnowledgeSource source = new AiKnowledgeSource();
        source.setId(1L);
        source.setSourceName("产品知识库");
        source.setSourceType("document");
        source.setSourcePath("/data/docs");

        when(knowledgeSourceService.getById(eq(1L)))
                .thenReturn(source);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge-source/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sourceName").value("产品知识库"))
                .andExpect(jsonPath("$.data.sourceType").value("document"));
    }

    @Test
    @DisplayName("新增知识源 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        KnowledgeSourceSaveVO vo = new KnowledgeSourceSaveVO();
        vo.setSourceName("新知识源");
        vo.setSourceType("document");
        vo.setSourcePath("/data/new");

        when(knowledgeSourceService.save(any(KnowledgeSourceSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge-source/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除知识源 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(knowledgeSourceService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/ai/admin/knowledge-source/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("非管理员访问 - 应返回 2002")
    void asUser_shouldReturn2002() throws Exception {
        KnowledgeSourceSearchVO vo = new KnowledgeSourceSearchVO();
        vo.setPage(0);
        vo.setRows(10);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge-source/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2002")
    void withoutAuth_shouldReturn2002() throws Exception {
        KnowledgeSourceSearchVO vo = new KnowledgeSourceSearchVO();
        vo.setPage(0);
        vo.setRows(10);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge-source/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }
}
