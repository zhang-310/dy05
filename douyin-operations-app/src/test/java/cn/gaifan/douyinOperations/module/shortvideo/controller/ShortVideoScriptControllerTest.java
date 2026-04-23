package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
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
 * ShortVideoScriptController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoScriptController 集成测试")
class ShortVideoScriptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvScriptService scriptService;

    @Test
    @DisplayName("脚本列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        SvScriptSearchVO vo = new SvScriptSearchVO();
        vo.setPage(0);
        vo.setRows(30);

        PageResultVO<SvScriptVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(scriptService.search(any(), eq(1L))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/script/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("脚本详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        SvScriptVO script = new SvScriptVO();
        script.setId(1L);
        script.setTitle("测试脚本");

        when(scriptService.get(eq(1L), eq(1L))).thenReturn(script);

        mockMvc.perform(post("/api/v1/short-video/script/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("测试脚本"));
    }

    @Test
    @DisplayName("保存脚本 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SvScriptSaveVO vo = new SvScriptSaveVO();
        vo.setTitle("新脚本");
        vo.setContent("脚本内容");
        vo.setScriptType("daily");

        when(scriptService.save(any(), eq(1L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/script/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除脚本 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(scriptService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/script/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("AI 生成脚本 - 应返回 200")
    void generate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "daily");
        body.put("theme", "护肤");
        body.put("style", "轻松");
        body.put("duration", 60);

        when(scriptService.generate(eq("daily"), eq("护肤"), isNull(), isNull(), eq("轻松"), eq(60), eq(1L)))
                .thenReturn("生成的脚本内容");

        mockMvc.perform(post("/api/v1/short-video/script/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("生成的脚本内容"));
    }

    @Test
    @DisplayName("分析爆款脚本 - 应返回 200")
    void analyzeViral_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoUrl", "https://douyin.com/video/123");
        body.put("extractLevel", "detailed");

        when(scriptService.analyzeViral(eq("https://douyin.com/video/123"), eq("detailed"), eq(1L)))
                .thenReturn("分析结果");

        mockMvc.perform(post("/api/v1/short-video/script/analyze-viral")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("分析结果"));
    }

    @Test
    @DisplayName("脚本列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        SvScriptSearchVO vo = new SvScriptSearchVO();

        mockMvc.perform(post("/api/v1/short-video/script/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取脚本详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/script/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("分析爆款脚本（缺少 URL）- 应返回 1001")
    void analyzeViral_missingUrl_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/script/analyze-viral")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
