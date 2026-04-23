package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.config.AppComplianceProperties;
import cn.gaifan.douyinOperations.module.script.service.IndustryComplianceService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ComplianceController 集成测试")
class ComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IndustryComplianceService industryComplianceService;

    @MockBean
    private AppComplianceProperties appComplianceProperties;

    @Test
    @DisplayName("合规检测 - 应返回 200")
    void check_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("text", "测试文本");
        body.put("industryCode", "cosmetics");

        Map<String, Object> result = new HashMap<>();
        result.put("compliant", true);

        when(industryComplianceService.checkCompliance(eq("测试文本"), eq("cosmetics")))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/v1/script/compliance/check")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("合规检测（未登录）- 应返回 2001")
    void check_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("text", "测试文本");

        mockMvc.perform(post("/api/v1/script/compliance/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取行业合规规则 - 应返回 200")
    void rules_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("industryCode", "cosmetics");

        Map<String, Object> rule = new HashMap<>();
        rule.put("ruleId", 1);
        rule.put("ruleName", "禁用词规则");

        when(industryComplianceService.listRules(eq("cosmetics")))
                .thenReturn(List.of(rule));

        mockMvc.perform(post("/api/v1/script/compliance/rules")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取行业合规规则（未登录）- 应返回 2001")
    void rules_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("industryCode", "cosmetics");

        mockMvc.perform(post("/api/v1/script/compliance/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("已支持的垂直行业合规编码列表 - 应返回 200")
    void industryCodes_shouldReturn200() throws Exception {
        when(industryComplianceService.listSupportedIndustryCodes())
                .thenReturn(List.of("cosmetics", "food", "health"));

        mockMvc.perform(post("/api/v1/script/compliance/industry-codes")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.defaultVerticalCode").value("cosmetics"));
    }

    @Test
    @DisplayName("已支持的垂直行业合规编码列表（未登录）- 应返回 2001")
    void industryCodes_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/script/compliance/industry-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("抖音公开规则文档入口 - 应返回 200")
    void douyinOfficialReferences_shouldReturn200() throws Exception {
        AppComplianceProperties.Douyin douyin = new AppComplianceProperties.Douyin();
        douyin.setNotice("抖音合规规则");
        douyin.setReferenceUrls(List.of("https://example.com/rules"));

        when(appComplianceProperties.getDouyin()).thenReturn(douyin);

        mockMvc.perform(post("/api/v1/script/compliance/douyin-official-references")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.notice").value("抖音合规规则"));
    }

    @Test
    @DisplayName("抖音公开规则文档入口（未登录）- 应返回 2001")
    void douyinOfficialReferences_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/script/compliance/douyin-official-references")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
