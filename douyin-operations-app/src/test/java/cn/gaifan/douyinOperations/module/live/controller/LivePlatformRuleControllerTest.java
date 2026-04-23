package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LivePlatform;
import cn.gaifan.douyinOperations.module.live.service.LivePlatformRuleService;
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LivePlatformRuleController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LivePlatformRuleController 集成测试")
class LivePlatformRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LivePlatformRuleService platformRuleService;

    @Test
    @DisplayName("获取所有启用的平台列表 - 应返回 200")
    void listPlatforms_shouldReturn200() throws Exception {
        LivePlatform platform1 = new LivePlatform();
        platform1.setId(1L);
        platform1.setPlatformCode("douyin");
        platform1.setPlatformName("抖音");
        platform1.setActive(1);
        platform1.setCreateTime(new Timestamp(System.currentTimeMillis()));

        LivePlatform platform2 = new LivePlatform();
        platform2.setId(2L);
        platform2.setPlatformCode("kuaishou");
        platform2.setPlatformName("快手");
        platform2.setActive(1);
        platform2.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(platformRuleService.listActivePlatforms()).thenReturn(List.of(platform1, platform2));

        mockMvc.perform(post("/api/v1/live/platform/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].platformCode").value("douyin"))
                .andExpect(jsonPath("$.data[1].platformCode").value("kuaishou"));
    }

    @Test
    @DisplayName("平台级违禁词检查 - 应返回 200")
    void checkViolation_shouldReturn200() throws Exception {
        LivePlatformRuleController.ViolationCheckVO vo = new LivePlatformRuleController.ViolationCheckVO();
        vo.setText("这是最好的产品，全国第一");
        vo.setPlatformCode("douyin");

        Map<String, Object> violation1 = new HashMap<>();
        violation1.put("word", "最好");
        violation1.put("position", 2);
        violation1.put("severity", "high");

        Map<String, Object> violation2 = new HashMap<>();
        violation2.put("word", "第一");
        violation2.put("position", 12);
        violation2.put("severity", "high");

        when(platformRuleService.checkPlatformViolation(eq("这是最好的产品，全国第一"), eq("douyin")))
                .thenReturn(List.of(violation1, violation2));

        mockMvc.perform(post("/api/v1/live/platform/violation-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].word").value("最好"))
                .andExpect(jsonPath("$.data[1].word").value("第一"));
    }

    @Test
    @DisplayName("获取平台 prompt 模板片段 - 应返回 200")
    void getPromptTemplate_shouldReturn200() throws Exception {
        LivePlatformRuleController.PlatformCodeVO vo = new LivePlatformRuleController.PlatformCodeVO();
        vo.setPlatformCode("douyin");

        String template = "抖音平台话术要求：1. 避免使用绝对化用语；2. 注意价格表述规范；3. 禁止医疗承诺";

        when(platformRuleService.getPlatformPromptTemplate(eq("douyin"))).thenReturn(template);

        mockMvc.perform(post("/api/v1/live/platform/prompt-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(template));
    }
}
