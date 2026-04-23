package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.vo.SaveFromSessionVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptCustomTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptCustomTemplateController 集成测试")
class LiveScriptCustomTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptRepository liveScriptRepository;

    @MockBean
    private LiveProductRepository liveProductRepository;

    @MockBean
    private LiveScriptTemplateRepository templateRepository;

    @MockBean
    private LiveSessionRepository liveSessionRepository;

    @Test
    @DisplayName("从场次创建模板 - 应返回 200")
    void saveFromSession_shouldReturn200() throws Exception {
        SaveFromSessionVO vo = new SaveFromSessionVO();
        vo.setSessionId(100L);
        vo.setTemplateName("我的自定义模板");
        vo.setDescription("从场次创建的模板");

        LiveSession session = new LiveSession();
        session.setId(100L);
        session.setLiveTitle("测试直播场次");
        session.setScriptStyle("product");
        session.setUserId(1L);

        LiveScript script1 = new LiveScript();
        script1.setId(1L);
        script1.setScriptType("opening");
        script1.setSequenceNo(1);
        script1.setDurationLimitSec(30);
        script1.setScriptContent("欢迎来到直播间，今天给大家带来美白精华");
        script1.setProductId(200L);
        script1.setStyle("enthusiastic");
        script1.setRequirement("开场白");

        LiveScript script2 = new LiveScript();
        script2.setId(2L);
        script2.setScriptType("product");
        script2.setSequenceNo(2);
        script2.setDurationLimitSec(60);
        script2.setScriptContent("这款美白精华效果非常好");
        script2.setProductId(200L);
        script2.setStyle("professional");
        script2.setRequirement("产品介绍");

        LiveProduct product = new LiveProduct();
        product.setProductId(200L);
        product.setProductName("美白精华");

        LiveScriptTemplate savedTemplate = new LiveScriptTemplate();
        savedTemplate.setId(1L);
        savedTemplate.setTemplateName("我的自定义模板");

        when(liveSessionRepository.findByIdAndUserIdAndDeleted(eq(100L), eq(1L), eq(0)))
                .thenReturn(Optional.of(session));
        when(liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(eq(100L), eq(0)))
                .thenReturn(List.of(script1, script2));
        when(liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(eq(100L)))
                .thenReturn(List.of(product));
        when(templateRepository.save(any(LiveScriptTemplate.class)))
                .thenReturn(savedTemplate);

        mockMvc.perform(post("/api/v1/live/script-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("从场次创建模板（管理员）- 应返回 200")
    void saveFromSession_asAdmin_shouldReturn200() throws Exception {
        SaveFromSessionVO vo = new SaveFromSessionVO();
        vo.setSessionId(100L);
        vo.setTemplateName("管理员模板");

        LiveSession session = new LiveSession();
        session.setId(100L);
        session.setLiveTitle("测试场次");
        session.setScriptStyle("chat");
        session.setUserId(2L);

        LiveScript script = new LiveScript();
        script.setId(1L);
        script.setScriptType("custom");
        script.setSequenceNo(1);
        script.setDurationLimitSec(45);
        script.setScriptContent("测试内容");

        LiveScriptTemplate savedTemplate = new LiveScriptTemplate();
        savedTemplate.setId(2L);

        when(liveSessionRepository.findByIdAndDeleted(eq(100L), eq(0)))
                .thenReturn(Optional.of(session));
        when(liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(eq(100L), eq(0)))
                .thenReturn(List.of(script));
        when(liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(eq(100L)))
                .thenReturn(List.of());
        when(templateRepository.save(any(LiveScriptTemplate.class)))
                .thenReturn(savedTemplate);

        mockMvc.perform(post("/api/v1/live/script-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("从场次创建模板（场次不存在）- 应返回 2002")
    void saveFromSession_sessionNotFound_shouldReturn2002() throws Exception {
        SaveFromSessionVO vo = new SaveFromSessionVO();
        vo.setSessionId(999L);
        vo.setTemplateName("测试模板");

        when(liveSessionRepository.findByIdAndUserIdAndDeleted(eq(999L), eq(1L), eq(0)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/live/script-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("从场次创建模板（场次无话术）- 应返回 1005")
    void saveFromSession_noScripts_shouldReturn1005() throws Exception {
        SaveFromSessionVO vo = new SaveFromSessionVO();
        vo.setSessionId(100L);
        vo.setTemplateName("测试模板");

        LiveSession session = new LiveSession();
        session.setId(100L);
        session.setUserId(1L);

        when(liveSessionRepository.findByIdAndUserIdAndDeleted(eq(100L), eq(1L), eq(0)))
                .thenReturn(Optional.of(session));
        when(liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(eq(100L), eq(0)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/live/script-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1005));
    }

    @Test
    @DisplayName("从场次创建模板（缺少 sessionId）- 应返回 1001")
    void saveFromSession_withoutSessionId_shouldReturn1001() throws Exception {
        SaveFromSessionVO vo = new SaveFromSessionVO();
        vo.setTemplateName("测试模板");

        mockMvc.perform(post("/api/v1/live/script-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("从场次创建模板（缺少 templateName）- 应返回 1001")
    void saveFromSession_withoutTemplateName_shouldReturn1001() throws Exception {
        SaveFromSessionVO vo = new SaveFromSessionVO();
        vo.setSessionId(100L);

        mockMvc.perform(post("/api/v1/live/script-template/save-from-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        SaveFromSessionVO vo = new SaveFromSessionVO();
        vo.setSessionId(100L);
        vo.setTemplateName("测试模板");

        mockMvc.perform(post("/api/v1/live/script-template/save-from-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
