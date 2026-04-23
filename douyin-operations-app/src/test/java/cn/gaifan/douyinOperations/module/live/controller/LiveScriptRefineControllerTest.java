package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.vo.*;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptRefineController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptRefineController 集成测试")
class LiveScriptRefineControllerTest {

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

    @Test
    @DisplayName("AI 提问式修改话术 - 应返回 200")
    void refineScript_shouldReturn200() throws Exception {
        ScriptRefineVO vo = new ScriptRefineVO();
        vo.setScriptId(1L);
        vo.setQuestion("请把这段话术改得更亲切一些");
        vo.setModelId(1L);

        when(liveAiService.refineScript(eq(1L), eq("请把这段话术改得更亲切一些"), eq(1L), eq(1L)))
                .thenReturn("老铁们好啊，今天给大家带来超值好物...");

        mockMvc.perform(post("/api/v1/live/ai/refine-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("老铁们好啊，今天给大家带来超值好物..."));
    }

    @Test
    @DisplayName("AI 段内微调 - 应返回 200")
    void refineSegment_shouldReturn200() throws Exception {
        ScriptRefineSegmentVO vo = new ScriptRefineSegmentVO();
        vo.setScriptId(1L);
        vo.setSegmentText("这款产品非常好用");
        vo.setInstruction("加入具体功效描述");
        vo.setModelId(1L);

        when(liveAiService.refineSegment(eq(1L), eq("这款产品非常好用"), eq("加入具体功效描述"), eq(1L), eq(1L)))
                .thenReturn("这款产品非常好用，能够深层补水，改善肌肤干燥问题");

        mockMvc.perform(post("/api/v1/live/ai/refine-segment")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("这款产品非常好用，能够深层补水，改善肌肤干燥问题"));
    }

    @Test
    @DisplayName("编辑时 AI 写作助手 - 应返回 200")
    void chatForScript_shouldReturn200() throws Exception {
        ScriptChatVO vo = new ScriptChatVO();
        vo.setScriptId(1L);
        vo.setMessage("帮我加一段关于成分的介绍");
        vo.setModelId(1L);

        when(liveAiService.chatForScript(eq(1L), eq("帮我加一段关于成分的介绍"), eq(1L), eq(1L)))
                .thenReturn("这款产品含有玻尿酸、烟酰胺等核心成分，能够有效提亮肤色...");

        mockMvc.perform(post("/api/v1/live/ai/chat-for-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("这款产品含有玻尿酸、烟酰胺等核心成分，能够有效提亮肤色..."));
    }

    @Test
    @DisplayName("批量应用同一指令 - 应返回 200")
    void batchChatForScript_shouldReturn200() throws Exception {
        BatchChatVO vo = new BatchChatVO();
        vo.setScriptIds(List.of(1L, 2L));
        vo.setMessage("加入促销信息");
        vo.setModelId(1L);

        when(liveAiService.chatForScript(eq(1L), eq("加入促销信息"), eq(1L), eq(1L)))
                .thenReturn("限时优惠，现在下单立减50元！");
        when(liveAiService.chatForScript(eq(2L), eq("加入促销信息"), eq(1L), eq(1L)))
                .thenReturn("今日特惠，买二送一！");

        mockMvc.perform(post("/api/v1/live/ai/batch-chat-for-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.1").value("限时优惠，现在下单立减50元！"))
                .andExpect(jsonPath("$.data.2").value("今日特惠，买二送一！"));
    }

    @Test
    @DisplayName("根据效果生成改进版话术（有改进）- 应返回 200")
    void suggestImprovement_withImprovement_shouldReturn200() throws Exception {
        ScriptIdVO vo = new ScriptIdVO();
        vo.setScriptId(1L);
        vo.setModelId(1L);

        when(liveAiService.suggestImprovement(eq(1L), eq(1L), eq(1L)))
                .thenReturn("改进版话术：增加互动性，加入提问环节...");

        mockMvc.perform(post("/api/v1/live/ai/suggest-improvement")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.improved").value("改进版话术：增加互动性，加入提问环节..."))
                .andExpect(jsonPath("$.data.skipped").value(false));
    }

    @Test
    @DisplayName("根据效果生成改进版话术（无需改进）- 应返回 200")
    void suggestImprovement_noImprovement_shouldReturn200() throws Exception {
        ScriptIdVO vo = new ScriptIdVO();
        vo.setScriptId(1L);
        vo.setModelId(1L);

        when(liveAiService.suggestImprovement(eq(1L), eq(1L), eq(1L)))
                .thenReturn(null);

        mockMvc.perform(post("/api/v1/live/ai/suggest-improvement")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.skipped").value(true))
                .andExpect(jsonPath("$.data.reason").value("话术效果良好或暂无效果数据，无需改进"));
    }

    @Test
    @DisplayName("相似度检测 - 应返回 200")
    void checkSimilarity_shouldReturn200() throws Exception {
        SessionIdVO vo = new SessionIdVO();
        vo.setSessionId(100L);

        SimilarityItemVO item1 = new SimilarityItemVO();
        item1.setScriptId1(1L);
        item1.setScriptId2(2L);
        item1.setType1("opening");
        item1.setType2("opening");
        item1.setSimilarityLevel("high");
        item1.setSuggestion("建议合并或删除重复话术");

        when(liveAiService.checkSimilarity(eq(100L), eq(1L)))
                .thenReturn(List.of(item1));

        mockMvc.perform(post("/api/v1/live/ai/check-similarity")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].scriptId1").value(1))
                .andExpect(jsonPath("$.data[0].scriptId2").value(2))
                .andExpect(jsonPath("$.data[0].similarityLevel").value("high"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        ScriptRefineVO vo = new ScriptRefineVO();
        vo.setScriptId(1L);
        vo.setQuestion("修改话术");

        mockMvc.perform(post("/api/v1/live/ai/refine-script")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
