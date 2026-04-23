package cn.gaifan.douyinOperations.module.slangdict.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.slangdict.service.SlangDictService;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySearchVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySaveVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntryVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SlangDictController 集成测试")
class SlangDictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SlangDictService slangDictService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @Test
    @DisplayName("分页搜索梗条目 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        SdEntrySearchVO searchVO = new SdEntrySearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        SdEntryVO entryVO = new SdEntryVO();
        entryVO.setId(1L);
        entryVO.setPhrase("测试梗");

        PageResultVO<SdEntryVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(entryVO));

        when(dataScopeService.getVisibleUserIds(eq(1L), anyString()))
                .thenReturn(List.of(1L));
        when(slangDictService.search(any(SdEntrySearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/slangdict/entry/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("分页搜索梗条目（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        SdEntrySearchVO searchVO = new SdEntrySearchVO();

        mockMvc.perform(post("/api/v1/slangdict/entry/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取梗条目详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        SdEntryVO entryVO = new SdEntryVO();
        entryVO.setId(1L);
        entryVO.setPhrase("测试梗");

        when(slangDictService.getById(eq(1L)))
                .thenReturn(entryVO);

        mockMvc.perform(post("/api/v1/slangdict/entry/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.phrase").value("测试梗"));
    }

    @Test
    @DisplayName("获取梗条目详情（未登录）- 应返回 2001")
    void get_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/slangdict/entry/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("新增/更新梗条目 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SdEntrySaveVO saveVO = new SdEntrySaveVO();
        saveVO.setPhrase("新梗");
        saveVO.setMeaning("解释");

        when(slangDictService.save(any(SdEntrySaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/slangdict/entry/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("新增/更新梗条目（未登录）- 应返回 2001")
    void save_unauthorized_shouldReturn2001() throws Exception {
        SdEntrySaveVO saveVO = new SdEntrySaveVO();
        saveVO.setPhrase("新梗");

        mockMvc.perform(post("/api/v1/slangdict/entry/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除梗条目 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(slangDictService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/slangdict/entry/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除梗条目（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/slangdict/entry/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("按产品ID查关联梗 - 应返回 200")
    void byProduct_shouldReturn200() throws Exception {
        SdEntryVO entryVO = new SdEntryVO();
        entryVO.setId(1L);
        entryVO.setPhrase("产品梗");

        when(slangDictService.getByProductId(eq(1L), eq(1L)))
                .thenReturn(List.of(entryVO));

        mockMvc.perform(post("/api/v1/slangdict/entry/by-product")
                        .requestAttr("userId", 1L)
                        .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("按产品ID查关联梗（未登录）- 应返回 2001")
    void byProduct_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/slangdict/entry/by-product")
                        .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("绑定梗到产品 - 应返回 204")
    void bindProduct_shouldReturn204() throws Exception {
        doNothing().when(slangDictService).bindProduct(eq(1L), eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/slangdict/entry/bind-product")
                        .requestAttr("userId", 1L)
                        .param("entryId", "1")
                        .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("绑定梗到产品（未登录）- 应返回 2001")
    void bindProduct_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/slangdict/entry/bind-product")
                        .param("entryId", "1")
                        .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("解绑梗与产品 - 应返回 204")
    void unbindProduct_shouldReturn204() throws Exception {
        doNothing().when(slangDictService).unbindProduct(eq(1L), eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/slangdict/entry/unbind-product")
                        .requestAttr("userId", 1L)
                        .param("entryId", "1")
                        .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("解绑梗与产品（未登录）- 应返回 2001")
    void unbindProduct_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/slangdict/entry/unbind-product")
                        .param("entryId", "1")
                        .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("AI 为产品生成候选梗 - 应返回 200")
    void aiGenerate_shouldReturn200() throws Exception {
        when(slangDictService.aiGeneratePhrases(eq(1L), eq(1L), eq(5)))
                .thenReturn(List.of("梗1", "梗2", "梗3"));

        mockMvc.perform(post("/api/v1/slangdict/entry/ai-generate")
                        .requestAttr("userId", 1L)
                        .param("productId", "1")
                        .param("count", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("AI 为产品生成候选梗（未登录）- 应返回 2001")
    void aiGenerate_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/slangdict/entry/ai-generate")
                        .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
