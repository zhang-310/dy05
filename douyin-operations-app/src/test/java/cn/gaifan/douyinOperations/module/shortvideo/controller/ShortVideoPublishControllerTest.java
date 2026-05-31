package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.ContentAuditService;
import cn.gaifan.douyinOperations.module.shortvideo.service.DouyinPublishService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiTitleGenerateVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShortVideoPublishController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoPublishController 集成测试")
class ShortVideoPublishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortVideoAiService aiService;

    @MockBean
    private ContentAuditService contentAuditService;

    @MockBean
    private SvProjectService projectService;

    @MockBean
    private DouyinPublishService douyinPublishService;

    @MockBean
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @Test
    @DisplayName("AI 生成标题 - 应返回 200")
    void generateTitle_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("script", "护肤品推荐");
        body.put("count", 3);

        when(aiService.generateTitles(any(AiTitleGenerateVO.class), eq(1L)))
                .thenReturn(List.of("标题1", "标题2", "标题3"));

        mockMvc.perform(post("/api/v1/short-video/publish/generate-title")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.titles").isArray())
                .andExpect(jsonPath("$.data.titles[0].text").value("标题1"));
    }

    @Test
    @DisplayName("AI 审核 - 通过 - 应返回 200")
    void aiReview_passed_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("title", "测试标题");
        body.put("cover", "https://example.com/cover.jpg");

        when(contentAuditService.auditVideo(anyString()))
                .thenReturn(ContentAuditService.AuditResult.pass());
        when(contentAuditService.auditImage(anyString()))
                .thenReturn(ContentAuditService.AuditResult.pass());
        when(contentAuditService.auditText(anyString()))
                .thenReturn(new ContentAuditService.AuditResult(true, "pass", List.of(), List.of("建议增加字幕")));
        mockViolationReference();

        mockMvc.perform(post("/api/v1/short-video/publish/ai-review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.issues").isEmpty())
                .andExpect(jsonPath("$.data.suggestions").isArray());
    }

    @Test
    @DisplayName("AI 审核（项目上下文）- 应回写项目审核状态")
    void aiReview_withProject_shouldSaveReviewStatus() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 12L);
        body.put("title", "测试标题");

        SvProjectVO project = new SvProjectVO();
        project.setId(12L);
        project.setOwnerId(1L);
        project.setTitle("项目标题");
        project.setProjectType("soft_ad");
        project.setFinalVideoUrl("https://cdn.example.com/final.mp4");
        when(projectService.get(eq(12L), eq(1L), anyList())).thenReturn(project);
        when(projectService.save(any(SvProjectSaveVO.class), eq(1L))).thenReturn(12L);
        when(contentAuditService.auditVideo(anyString())).thenReturn(ContentAuditService.AuditResult.pass());
        when(contentAuditService.auditText(anyString())).thenReturn(ContentAuditService.AuditResult.pass());
        mockViolationReference();

        mockMvc.perform(post("/api/v1/short-video/publish/ai-review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.projectId").value(12));

        verify(projectService).save(argThat(save ->
                save.getId().equals(12L)
                        && "approved".equals(save.getReviewStatus())
                        && "测试标题".equals(save.getPublishTitle())
        ), eq(1L));
    }

    @Test
    @DisplayName("AI 审核 - 未通过 - 应返回 200")
    void aiReview_failed_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("title", "违规标题");

        when(contentAuditService.auditVideo(anyString()))
                .thenReturn(ContentAuditService.AuditResult.pass());
        when(contentAuditService.auditText(anyString()))
                .thenReturn(new ContentAuditService.AuditResult(false, "reject", List.of("标题包含敏感词"), List.of()));

        mockMvc.perform(post("/api/v1/short-video/publish/ai-review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.issues").isArray())
                .andExpect(jsonPath("$.data.issues[0]").value("标题包含敏感词"));
    }

    @Test
    @DisplayName("抖音平台发布 - 应返回 200")
    void publishDouyin_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("title", "测试视频");
        mockViolationReference();

        mockMvc.perform(post("/api/v1/short-video/publish/douyin")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.platform").value("douyin"));
    }

    @Test
    @DisplayName("发布 - 应返回 200")
    void publish_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("title", "测试视频");
        body.put("platforms", List.of("douyin", "weixin-video"));
        mockViolationReference();

        mockMvc.perform(post("/api/v1/short-video/publish/publish")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.results").isArray())
                .andExpect(jsonPath("$.data.results[0].platform").value("douyin"));
    }

    @Test
    @DisplayName("发布（项目上下文）- 应调用抖音发布服务并回写项目发布状态")
    void publish_withProject_shouldCallDouyinServiceAndSaveProject() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 12L);
        body.put("title", "测试视频");
        body.put("platforms", List.of("douyin"));

        SvProjectVO project = new SvProjectVO();
        project.setId(12L);
        project.setOwnerId(1L);
        project.setTitle("项目标题");
        project.setProjectType("soft_ad");
        project.setFinalVideoUrl("https://cdn.example.com/final.mp4");
        when(projectService.get(eq(12L), eq(1L), anyList())).thenReturn(project);
        when(projectService.save(any(SvProjectSaveVO.class), eq(1L))).thenReturn(12L);
        when(douyinPublishService.publish(eq("https://cdn.example.com/final.mp4"), eq("测试视频"), eq(1L)))
                .thenReturn(DouyinPublishService.PublishResult.ok("item-123"));
        mockViolationReference();

        mockMvc.perform(post("/api/v1/short-video/publish/publish")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(12))
                .andExpect(jsonPath("$.data.results[0].itemId").value("item-123"));

        verify(projectService).save(argThat(save ->
                save.getId().equals(12L)
                        && "published".equals(save.getStatus())
                        && "approved".equals(save.getReviewStatus())
                        && save.getPublishPlatforms().contains("douyin")
        ), eq(1L));
    }

    @Test
    @DisplayName("发布缺 douyin_weigui 官方引用时应阻断且不调用平台发布")
    void publish_shouldBlockWhenViolationReferenceMissing() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("title", "测试视频");
        body.put("platforms", List.of("douyin"));

        when(operationalStrategyKnowledgeService.buildViolationRuleContext(anyLong(), anyString(), anyString(), anyInt()))
                .thenReturn(new OperationalStrategyKnowledgeService.PromptContext(
                        "<douyin_ops_violation_context>未命中违规规则</douyin_ops_violation_context>",
                        List.of(),
                        List.of()));

        mockMvc.perform(post("/api/v1/short-video/publish/publish")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(3148))
                .andExpect(jsonPath("$.data.officialReferenceRequired").value(true))
                .andExpect(jsonPath("$.data.officialReferenceSatisfied").value(false))
                .andExpect(jsonPath("$.data.officialReferenceStatus").value("missing_douyin_weigui_reference"));

        verify(douyinPublishService, never()).publish(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("AI 生成标题（未登录）- 应返回 2001")
    void generateTitle_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("script", "测试");

        mockMvc.perform(post("/api/v1/short-video/publish/generate-title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    private void mockViolationReference() {
        when(operationalStrategyKnowledgeService.buildViolationRuleContext(anyLong(), anyString(), anyString(), anyInt()))
                .thenReturn(new OperationalStrategyKnowledgeService.PromptContext(
                        "<douyin_ops_violation_context>官方违规规则</douyin_ops_violation_context>",
                        List.of(),
                        List.of(new OperationalStrategyKnowledgeService.OfficialReference(
                                "douyin_weigui", "violation_rule", 2L, 22L, "违规规则", "宣传违规", 0.91))));
    }
}
