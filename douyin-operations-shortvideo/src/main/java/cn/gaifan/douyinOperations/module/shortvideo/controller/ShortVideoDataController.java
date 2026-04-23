package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 短视频数据分析 API
 * 路径：/api/v1/short-video/data
 * 与设计文档路径统一
 */
@RestController
@RequestMapping("/api/v1/short-video/data")
@Tag(name = "短视频数据分析", description = "热门采集、爆款分析")
public class ShortVideoDataController {

    @Resource
    private ViralVideoService viralVideoService;
    @Resource
    private SvScriptService scriptService;

    @PostMapping("/collect-hot-videos")
    @Operation(summary = "采集热门视频")
    public RESTResult<Long> collectHotVideos(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        ViralCollectVO vo = new ViralCollectVO();
        String videoUrl = body != null && body.get("videoUrl") instanceof String s ? s : null;
        if (videoUrl == null || videoUrl.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoUrl 不能为空");
        vo.setVideoUrl(videoUrl);
        vo.setDouyinVideoId(body != null && body.get("douyinVideoId") instanceof String s ? s : ("url_" + System.currentTimeMillis()));
        vo.setTitle(body != null && body.get("title") instanceof String s ? s : null);
        vo.setCoverUrl(body != null && body.get("coverUrl") instanceof String s ? s : null);
        vo.setAuthorName(body != null && body.get("authorName") instanceof String s ? s : null);
        vo.setViewCount(body != null && body.get("viewCount") instanceof Number n ? n.longValue() : null);
        vo.setLikeCount(body != null && body.get("likeCount") instanceof Number n ? n.longValue() : null);
        vo.setShareCount(body != null && body.get("shareCount") instanceof Number n ? n.longValue() : null);
        vo.setTags(body != null && body.get("tags") instanceof String s ? s : null);
        Long id = viralVideoService.collectViralVideo(vo, userId);
        viralVideoService.triggerAnalysis(id, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/analyze-viral")
    @Operation(summary = "爆款分析", description = "别名：与 script/analyze-viral 相同实现，建议统一使用 script/analyze-viral")
    public RESTResult<String> analyzeViral(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String url = body != null && body.get("viralVideoUrl") instanceof String s ? s : null;
        String level = body != null && body.get("extractLevel") instanceof String s ? s : "basic";
        if (url == null || url.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "viralVideoUrl 不能为空");
        String result = scriptService.analyzeViral(url, level, userId);
        RESTResult<String> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
