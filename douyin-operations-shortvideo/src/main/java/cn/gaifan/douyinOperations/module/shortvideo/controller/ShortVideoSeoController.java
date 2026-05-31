package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.shortvideo.service.DouyinSeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 抖音 SEO 建议 Controller
 * API 路径：/api/v1/short-video/seo
 * 提供标题 A/B 变体、话题标签、封面、发布时间建议入口
 */
@RestController
@RequestMapping("/api/v1/short-video/seo")
@Tag(name = "抖音 SEO 建议", description = "标题 A/B 变体、话题标签、封面、发布时间推荐")
public class ShortVideoSeoController {

    @Resource
    private DouyinSeoService douyinSeoService;

    @Resource
    private DataScopeResolver dataScopeService;

    /**
     * 标签推荐
     * POST /api/v1/short-video/seo/suggest-tags
     * body: { title, description?, industry? }
     */
    @PostMapping("/suggest-tags")
    @Operation(summary = "话题标签推荐", description = "基于标题/描述/行业分词推荐 5-10 个抖音话题标签")
    public RESTResult<List<String>> suggestTags(@RequestBody(required = false) Map<String, Object> body,
                                                HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String title = body != null && body.get("title") instanceof String s ? s : "";
        String description = body != null && body.get("description") instanceof String s ? s : null;
        String industry = body != null && body.get("industry") instanceof String s ? s : "美妆护肤";
        RESTResult<List<String>> r = RESTResult.getSuccess(douyinSeoService.suggestTags(title, description, industry));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 发布时间推荐
     * POST /api/v1/short-video/seo/suggest-publish-time
     * body: { accountId? }
     */
    @PostMapping("/suggest-publish-time")
    @Operation(summary = "发布时间推荐", description = "基于账号历史数据推荐最优发布时间段")
    public RESTResult<List<String>> suggestPublishTime(@RequestBody(required = false) Map<String, Object> body,
                                                       HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long accountId = body != null && body.get("accountId") instanceof Number n ? n.longValue() : null;
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(AuthTokenFilter.getUserId(request), roleCode);
        RESTResult<List<String>> r = RESTResult.getSuccess(douyinSeoService.suggestPublishTime(accountId, visibleIds));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 封面推荐
     * POST /api/v1/short-video/seo/suggest-cover
     * body: { frameUrls: ["url1", "url2"] }
     */
    @PostMapping("/suggest-cover")
    @Operation(summary = "封面推荐", description = "从视频关键帧中推荐最优封面")
    public RESTResult<String> suggestCover(@RequestBody(required = false) Map<String, Object> body,
                                           HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        @SuppressWarnings("unchecked")
        List<String> frameUrls = body != null && body.get("frameUrls") instanceof List<?> l
                ? (List<String>) l : List.of();
        RESTResult<String> r = RESTResult.getSuccess(douyinSeoService.suggestCover(frameUrls));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 标题 A/B 变体生成
     * POST /api/v1/short-video/seo/suggest-ab-titles
     * body: { baseTitle }
     */
    @PostMapping("/suggest-ab-titles")
    @Operation(summary = "标题 A/B 测试变体", description = "基于基础标题生成 5 个 A/B 测试变体")
    public RESTResult<List<String>> suggestAbTestTitles(@RequestBody(required = false) Map<String, Object> body,
                                                         HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String baseTitle = body != null && body.get("baseTitle") instanceof String s ? s : "";
        RESTResult<List<String>> r = RESTResult.getSuccess(douyinSeoService.suggestAbTestTitles(baseTitle));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
