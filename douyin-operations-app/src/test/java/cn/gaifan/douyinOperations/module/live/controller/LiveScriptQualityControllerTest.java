package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.copy.service.CopyLibraryService;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.live.vo.SaveToCopyVO;
import cn.gaifan.douyinOperations.module.live.vo.ScriptIdVO;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptQualityController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptQualityController 集成测试")
class LiveScriptQualityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveAiService liveAiService;

    @MockBean
    private AiQuotaService aiQuotaService;

    @MockBean
    private AiCallLogService aiCallLogService;

    @MockBean
    private CopyLibraryService copyLibraryService;

    @MockBean
    private LiveScriptQualityService liveScriptQualityService;

    @MockBean
    private LiveScriptRepository liveScriptRepository;

    @MockBean
    private DyProductRepository dyProductRepository;

    @Test
    @DisplayName("增强合规检测 - 应返回 200")
    void checkViolationEnhanced_shouldReturn200() throws Exception {
        ScriptIdVO vo = new ScriptIdVO();
        vo.setScriptId(1L);

        LiveScript script = new LiveScript();
        script.setId(1L);
        script.setScriptContent("欢迎来到直播间，今天给大家带来优质产品");

        LiveAiResultVO.ViolationCheckResult result = new LiveAiResultVO.ViolationCheckResult();
        result.setPassed(true);
        result.setViolationCount(0);
        result.setViolations(List.of());

        when(liveScriptRepository.findById(eq(1L))).thenReturn(Optional.of(script));
        when(liveScriptQualityService.checkViolationEnhanced(anyString(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/ai/check-violation-enhanced")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.violationCount").value(0));
    }

    @Test
    @DisplayName("话术违规检测 - 应返回 200")
    void checkViolation_shouldReturn200() throws Exception {
        ScriptIdVO vo = new ScriptIdVO();
        vo.setScriptId(1L);

        LiveAiResultVO.ViolationCheckResult result = new LiveAiResultVO.ViolationCheckResult();
        result.setPassed(false);
        result.setViolationCount(2);
        result.setViolations(List.of("包含违禁词：最好", "包含违禁词：第一"));

        when(liveAiService.checkViolation(eq(1L), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/live/ai/check-violation")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.violationCount").value(2));
    }

    @Test
    @DisplayName("审核过关一键到文案库（通过）- 应返回 200")
    void saveToCopyIfPassed_passed_shouldReturn200() throws Exception {
        SaveToCopyVO vo = new SaveToCopyVO();
        vo.setContent("欢迎来到直播间，今天给大家带来优质产品");
        vo.setTitle("开场话术");

        LiveAiResultVO.ViolationCheckResult checkResult = new LiveAiResultVO.ViolationCheckResult();
        checkResult.setPassed(true);
        checkResult.setViolationCount(0);
        checkResult.setViolations(List.of());

        when(liveAiService.checkViolationByContent(anyString(), eq(1L)))
                .thenReturn(checkResult);
        when(copyLibraryService.save(any())).thenReturn(100L);

        mockMvc.perform(post("/api/v1/live/ai/save-to-copy-if-passed")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.copyId").value(100));
    }

    @Test
    @DisplayName("审核过关一键到文案库（未通过）- 应返回 200")
    void saveToCopyIfPassed_notPassed_shouldReturn200() throws Exception {
        SaveToCopyVO vo = new SaveToCopyVO();
        vo.setContent("这是最好的产品，全国第一");
        vo.setTitle("产品介绍");

        LiveAiResultVO.ViolationCheckResult checkResult = new LiveAiResultVO.ViolationCheckResult();
        checkResult.setPassed(false);
        checkResult.setViolationCount(2);
        checkResult.setViolations(List.of("包含违禁词：最好", "包含违禁词：第一"));

        when(liveAiService.checkViolationByContent(anyString(), eq(1L)))
                .thenReturn(checkResult);

        mockMvc.perform(post("/api/v1/live/ai/save-to-copy-if-passed")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.violationCount").value(2))
                .andExpect(jsonPath("$.data.copyId").doesNotExist());
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        ScriptIdVO vo = new ScriptIdVO();
        vo.setScriptId(1L);

        mockMvc.perform(post("/api/v1/live/ai/check-violation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
