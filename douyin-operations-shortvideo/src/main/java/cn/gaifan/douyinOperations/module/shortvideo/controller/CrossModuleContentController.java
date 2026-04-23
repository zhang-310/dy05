package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.CrossModuleContentService;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "跨模块内容联动 / Cross Module Content")
@RestController
@RequestMapping("/api/v1/short-video/cross")
public class CrossModuleContentController {

    @Resource
    private CrossModuleContentService crossModuleContentService;

    @Resource
    private SvHotTopicRepository svHotTopicRepository;

    @PostMapping("/live-to-video")
    @Operation(summary = "直播话术转短视频脚本")
    public RESTResult<String> liveToVideo(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long liveScriptId = body.get("liveScriptId");
        if (liveScriptId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "liveScriptId 必填");
        String result = crossModuleContentService.convertLiveScriptToVideoScript(liveScriptId, userId);
        RESTResult<String> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/video-preview-for-live")
    @Operation(summary = "查询直播关联的短视频引流效果预览")
    public RESTResult<Map<String, Object>> videoPreviewForLive(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.valueOf(body.get("sessionId").toString()) : null;
        List<SvHotTopic> relatedTopics = svHotTopicRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 5, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "heatScore"))
        ).getContent();
        List<Map<String, Object>> topicSummary = relatedTopics.stream().map(t ->
                Map.<String, Object>of("title", t.getTitle(), "heat", t.getHeatScore(), "source", t.getSource())
        ).collect(Collectors.toList());
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("sessionId", sessionId);
        preview.put("relatedHotTopics", topicSummary);
        preview.put("convertedScripts", 0);
        preview.put("suggestion", relatedTopics.isEmpty() ? "暂无关联热点，建议先同步热点数据" : "可基于当前热点创建引流短视频");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(preview);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/hot-topic-pool")
    @Operation(summary = "统一热点池（短视频+直播共享）")
    public RESTResult<Map<String, Object>> hotTopicPool(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        int limit = 20;
        if (body != null && body.get("limit") != null) {
            limit = Integer.parseInt(body.get("limit").toString());
        }
        List<SvHotTopic> topics = svHotTopicRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "heatScore"))
        ).getContent();
        List<Map<String, Object>> topicList = topics.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("topic", t.getTitle());
            m.put("source", t.getSource());
            m.put("heat", t.getHeatScore());
            m.put("category", t.getCategory());
            m.put("status", t.getStatus());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> pool = new LinkedHashMap<>();
        pool.put("timestamp", System.currentTimeMillis());
        pool.put("hotTopics", topicList);
        pool.put("totalCount", topicList.size());
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(pool);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
