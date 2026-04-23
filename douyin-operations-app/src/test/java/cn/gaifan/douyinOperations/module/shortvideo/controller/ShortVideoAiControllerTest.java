package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiCopyGenerateVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiScriptGenerateVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiTitleGenerateVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiVideoPlanGenerateVO;
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
 * ShortVideoAiController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoAiController 集成测试")
class ShortVideoAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortVideoAiService aiService;

    @MockBean
    private ViolationWordService violationWordService;

    @Test
    @DisplayName("文案违规检测 - 无违规 - 应返回 200")
    void checkViolation_noViolation_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("text", "正常文案内容");

        ViolationCheckResultVO result = new ViolationCheckResultVO();
        result.setHasViolation(false);
        result.setTotalCount(0);
        result.setViolations(List.of());

        when(violationWordService.check(eq("正常文案内容"), eq("video"), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/ai/check-violation")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.data.hasViolation").value(false))
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("文案违规检测 - 有违规 - 应返回 200")
    void checkViolation_hasViolation_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("text", "违规文案");

        ViolationCheckResultVO result = new ViolationCheckResultVO();
        result.setHasViolation(true);
        result.setTotalCount(1);
        result.setViolations(List.of());

        when(violationWordService.check(eq("违规文案"), eq("video"), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/ai/check-violation")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.data.hasViolation").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("AI 生成文案 - 应返回 200")
    void generateCopy_shouldReturn200() throws Exception {
        AiCopyGenerateVO vo = new AiCopyGenerateVO();
        vo.setTopic("护肤");
        vo.setStyle("humorous");

        when(aiService.generateCopy(any(AiCopyGenerateVO.class), eq(1L)))
                .thenReturn("生成的文案内容");

        mockMvc.perform(post("/api/v1/short-video/ai/generate-copy")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.data").value("生成的文案内容"));
    }

    @Test
    @DisplayName("AI 生成脚本 - 应返回 200")
    void generateScript_shouldReturn200() throws Exception {
        AiScriptGenerateVO vo = new AiScriptGenerateVO();
        vo.setCopyText("文案内容");
        vo.setDuration(60);

        when(aiService.generateScript(any(AiScriptGenerateVO.class), eq(1L)))
                .thenReturn("生成的脚本内容");

        mockMvc.perform(post("/api/v1/short-video/ai/generate-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.data").value("生成的脚本内容"));
    }

    @Test
    @DisplayName("AI 生成标题 - 应返回 200")
    void generateTitles_shouldReturn200() throws Exception {
        AiTitleGenerateVO vo = new AiTitleGenerateVO();
        vo.setCopyText("文案内容");
        vo.setCount(3);

        when(aiService.generateTitles(any(AiTitleGenerateVO.class), eq(1L)))
                .thenReturn(List.of("标题1", "标题2", "标题3"));

        mockMvc.perform(post("/api/v1/short-video/ai/generate-title")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("标题1"));
    }

    @Test
    @DisplayName("AI 生成视频方案 - 应返回 200")
    void generateVideoPlan_shouldReturn200() throws Exception {
        AiVideoPlanGenerateVO vo = new AiVideoPlanGenerateVO();
        vo.setCopyText("护肤教程文案");
        vo.setShootingStyle("tutorial");

        when(aiService.generateVideoPlan(any(AiVideoPlanGenerateVO.class), eq(1L)))
                .thenReturn("生成的视频方案");

        mockMvc.perform(post("/api/v1/short-video/ai/generate-plan")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.data").value("生成的视频方案"));
    }

    @Test
    @DisplayName("AI 生成脚本（文案为空）- 应返回 1001")
    void generateScript_emptyCopyText_shouldReturn1001() throws Exception {
        AiScriptGenerateVO vo = new AiScriptGenerateVO();
        vo.setCopyText("");

        mockMvc.perform(post("/api/v1/short-video/ai/generate-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("文案违规检测（未登录）- 应返回 2001")
    void checkViolation_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("text", "测试");

        mockMvc.perform(post("/api/v1/short-video/ai/check-violation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
