package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinVideoService;
import cn.gaifan.douyinOperations.module.douyin.vo.*;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 抖音视频管理 Controller
 * Douyin Video Management Controller
 */
@RestController
@RequestMapping("/api/v1/douyin/video")
@Tag(name = "抖音视频 / Douyin Video", description = "抖音视频的管理（需登录）")
public class DouyinVideoController {

    private static final Logger log = LoggerFactory.getLogger(DouyinVideoController.class);

    @Resource
    private DouyinVideoService douyinVideoService;
    @Resource
    private DataScopeResolver dataScopeService;
    @Resource
    private DouyinAccountRepository douyinAccountRepository;

    @Resource
    private RateLimiter videoSyncRateLimiter;

    @PostMapping("/search")
    @Operation(
            summary = "查询抖音视频 / Search Douyin Videos",
            description = "分页查询抖音视频（需登录，非管理员仅能查看自己账号下的视频） / Search and paginate Douyin videos (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<DouyinVideoVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询条件 / Search criteria",
            required = false
    ) @RequestBody(required = false) DouyinVideoSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) vo = new DouyinVideoSearchVO();
        // 数据范围：按角色限制可见账号
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleUserIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleUserIds != null) {
            // 视频通过 accountId 关联账号，需先查可见用户的账号 ID 列表
            List<Long> visibleAccountIds = douyinAccountRepository.findIdsByUserIdIn(visibleUserIds);
            vo.setAccountIds(visibleAccountIds);
        }
        PageResultVO<DouyinVideoVO> data = douyinVideoService.search(vo);
        RESTResult<PageResultVO<DouyinVideoVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取视频详情 / Get Video Details",
            description = "根据视频 ID 获取视频详细信息（需登录） / Get video details by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "视频不存在 / Video not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<DouyinVideoVO> get(HttpServletRequest request, @Parameter(
            description = "视频 ID / Video ID",
            required = true
    ) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        DouyinVideoVO data = douyinVideoService.getVideo(id);
        RESTResult<DouyinVideoVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存视频 / Save Video",
            description = "创建或更新视频信息（需登录） / Create or update video information (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "视频信息 / Video information",
            required = true
    ) @RequestBody DouyinVideoSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        long id = douyinVideoService.saveVideo(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/sync")
    @Operation(
            summary = "同步抖音视频 / Sync Douyin Videos",
            description = "从抖音 API 同步指定账号的视频数据（需登录） / Sync videos from Douyin API for specified account (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "同步成功 / Sync successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "账号不存在 / Account not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> sync(HttpServletRequest request, @Parameter(
            description = "账号 ID / Account ID",
            required = true
    ) @RequestParam Long accountId) {
        // P1-3: 限流保护
        try {
            videoSyncRateLimiter.acquirePermission();
        } catch (RequestNotPermitted e) {
            log.warn("视频同步接口触发限流: userId={}, accountId={}", AuthTokenFilter.getUserId(request), accountId);
            return RESTResult.error(ErrorCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
        }

        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        var account = douyinAccountRepository.findByIdAndDeleted(accountId, 0)
                .orElseThrow(() -> new cn.gaifan.douyinOperations.common.exception.BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null && (visibleIds.isEmpty() || !visibleIds.contains(account.getUserId()))) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限同步该账号");
        }
        douyinVideoService.syncVideos(accountId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
