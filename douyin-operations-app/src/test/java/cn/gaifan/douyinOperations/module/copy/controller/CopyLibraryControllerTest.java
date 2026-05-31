package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.service.CopyLibraryService;
import cn.gaifan.douyinOperations.module.copy.vo.CopyLibrarySaveVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyLibrarySearchVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyLibraryVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CopyLibraryController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CopyLibraryController 集成测试")
class CopyLibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CopyLibraryService copyLibraryService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @BeforeEach
    void setUp() {
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("分页搜索文案库 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        CopyLibrarySearchVO searchVO = new CopyLibrarySearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setKeyword("测试文案");

        CopyLibraryVO copyVO = new CopyLibraryVO();
        copyVO.setId(1L);
        copyVO.setUserId(1L);
        copyVO.setTitle("测试文案标题");
        copyVO.setContent("这是一段测试文案内容");
        copyVO.setCategory("直播");
        copyVO.setWordCount(100);
        copyVO.setUseCount(5);
        copyVO.setStatus(1);

        PageResultVO<CopyLibraryVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(copyVO));

        when(copyLibraryService.search(any(CopyLibrarySearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/copy/library/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("测试文案标题"));
    }

    @Test
    @DisplayName("获取文案详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        CopyLibraryVO copyVO = new CopyLibraryVO();
        copyVO.setId(1L);
        copyVO.setUserId(1L);
        copyVO.setTitle("文案标题");
        copyVO.setContent("文案内容");
        copyVO.setCategory("短视频");

        when(copyLibraryService.getById(1L)).thenReturn(copyVO);

        mockMvc.perform(post("/api/v1/copy/library/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.title").value("文案标题"))
                .andExpect(jsonPath("$.data.content").value("文案内容"));
    }

    @Test
    @DisplayName("新建文案 - 应从登录态注入 userId 并返回 200")
    void save_shouldReturn200() throws Exception {
        CopyLibrarySaveVO saveVO = new CopyLibrarySaveVO();
        saveVO.setTitle("新文案");
        saveVO.setContent("新文案内容");
        saveVO.setCategory("直播");
        saveVO.setTags("开场,互动");

        when(copyLibraryService.save(any(CopyLibrarySaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/copy/library/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));

        verify(copyLibraryService).save(argThat(vo -> Long.valueOf(1L).equals(vo.getUserId())
                && "新文案".equals(vo.getTitle())
                && "新文案内容".equals(vo.getContent())));
    }

    @Test
    @DisplayName("删除文案 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(copyLibraryService).delete(1L, 1L);

        mockMvc.perform(post("/api/v1/copy/library/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新文案状态 - 应返回 204")
    void updateStatus_shouldReturn204() throws Exception {
        doNothing().when(copyLibraryService).updateStatus(1L, 1, 1L);

        mockMvc.perform(post("/api/v1/copy/library/update-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .param("status", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("递增使用次数 - 应返回 204")
    void incrementUseCount_shouldReturn204() throws Exception {
        doNothing().when(copyLibraryService).incrementUseCount(1L);

        mockMvc.perform(post("/api/v1/copy/library/increment-use-count")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        CopyLibrarySearchVO searchVO = new CopyLibrarySearchVO();

        mockMvc.perform(post("/api/v1/copy/library/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
