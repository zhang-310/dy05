package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.service.ScriptLibraryService;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.*;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ScriptController 集成测试
 * 测试话术库管理的核心功能：搜索、详情、保存、删除、违规检测
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ScriptController 集成测试")
class ScriptControllerTest {
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScriptLibraryService scriptLibraryService;

    @MockBean
    private ViolationWordService violationWordService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @BeforeEach
    void setUp() {
        // Mock 数据权限服务
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("话术列表分页搜索 - 200")
    void list_shouldReturn200() throws Exception {
        ScriptSearchVO searchVO = new ScriptSearchVO();
        searchVO.setKeyword("开场白");

        ScriptVO scriptVO = new ScriptVO();
        scriptVO.setId(1L);
        scriptVO.setTitle("测试话术");
        scriptVO.setContent("欢迎来到直播间");
        scriptVO.setCategory("开场");

        PageResultVO<ScriptVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(scriptVO));

        when(scriptLibraryService.search(any(ScriptSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/script/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("测试话术"));
    }

    @Test
    @DisplayName("获取话术详情 - 200")
    void get_shouldReturn200() throws Exception {
        ScriptVO scriptVO = new ScriptVO();
        scriptVO.setId(1L);
        scriptVO.setTitle("测试话术");
        scriptVO.setContent("欢迎来到直播间");
        scriptVO.setCategory("开场");

        when(scriptLibraryService.getById(1L, USER_ID)).thenReturn(scriptVO);

        mockMvc.perform(post("/api/v1/script/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("测试话术"));
    }

    @Test
    @DisplayName("新增话术 - 200")
    void save_shouldReturn200() throws Exception {
        ScriptSaveVO saveVO = new ScriptSaveVO();
        saveVO.setUserId(1L);
        saveVO.setTitle("新话术");
        saveVO.setContent("新的话术内容");
        saveVO.setCategory("开场");

        when(scriptLibraryService.save(any(ScriptSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/script/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除话术 - 204")
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/v1/script/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("递增使用次数 - 204")
    void useCount_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/v1/script/use-count")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取话术分类列表 - 200")
    void categories_shouldReturn200() throws Exception {
        List<String> categories = List.of("开场", "互动", "促单", "结束");

        when(scriptLibraryService.listCategories()).thenReturn(categories);

        mockMvc.perform(post("/api/v1/script/categories")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("开场"));
    }

    @Test
    @DisplayName("文本违规检测 - 200")
    void check_shouldReturn200() throws Exception {
        ViolationCheckVO checkVO = new ViolationCheckVO();
        checkVO.setText("测试文本内容");
        checkVO.setScope("all");

        ViolationCheckResultVO resultVO = new ViolationCheckResultVO();
        resultVO.setHasViolation(false);
        resultVO.setViolations(new ArrayList<>());

        when(violationWordService.check(anyString(), anyString(), anyLong())).thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/script/violation/check")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.hasViolation").value(false));
    }

    @Test
    @DisplayName("批量文本违规检测 - 200")
    void checkBatch_shouldReturn200() throws Exception {
        ViolationCheckBatchVO batchVO = new ViolationCheckBatchVO();
        ViolationCheckBatchVO.TextItem item1 = new ViolationCheckBatchVO.TextItem();
        item1.setKey("text1");
        item1.setText("文本1");
        ViolationCheckBatchVO.TextItem item2 = new ViolationCheckBatchVO.TextItem();
        item2.setKey("text2");
        item2.setText("文本2");
        batchVO.setTexts(List.of(item1, item2));

        ViolationCheckBatchResultVO resultVO = new ViolationCheckBatchResultVO();
        resultVO.setResults(new java.util.HashMap<>());

        when(violationWordService.checkBatch(any(ViolationCheckBatchVO.class), anyLong())).thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/script/violation/check-batch")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.results").exists());
    }

    @Test
    @DisplayName("公共违规词列表 - 200")
    void publicList_shouldReturn200() throws Exception {
        ViolationWordSearchVO searchVO = new ViolationWordSearchVO();

        ViolationWordVO wordVO = new ViolationWordVO();
        wordVO.setId(1L);
        wordVO.setWord("违规词");
        wordVO.setScope("live");

        PageResultVO<ViolationWordVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(wordVO));

        when(violationWordService.search(any(ViolationWordSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/script/violation/public/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("AI 推荐违规词替换建议 - 200")
    void suggestReplacement_shouldReturn200() throws Exception {
        ViolationReplacementRequestVO requestVO = new ViolationReplacementRequestVO();
        requestVO.setText("包含违规词的文本");
        requestVO.setViolationWords(List.of("违规词"));

        ViolationReplacementResultVO resultVO = new ViolationReplacementResultVO();
        resultVO.setSuggestedText("替换后的文本");

        when(violationWordService.suggestReplacement(any(ViolationReplacementRequestVO.class))).thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/script/violation/suggest-replacement")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.suggestedText").value("替换后的文本"));
    }

    @Test
    @DisplayName("未登录访问 - 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/script/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
