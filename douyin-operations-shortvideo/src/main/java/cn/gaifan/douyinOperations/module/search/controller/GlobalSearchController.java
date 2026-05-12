package cn.gaifan.douyinOperations.module.search.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.search.service.GlobalSearchService;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchRequestVO;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * D1-04 全局搜索（跨模块实体，数据范围与 Dashboard 一致）
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search / 全局搜索", description = "跨模块关键词搜索")
public class GlobalSearchController {

    @Resource
    private GlobalSearchService globalSearchService;

    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/global")
    @Operation(summary = "全局搜索")
    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "searchApi", fallbackMethod = "searchFallback")
    public RESTResult<GlobalSearchResponseVO> globalSearch(
            @Valid @RequestBody GlobalSearchRequestVO vo,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        GlobalSearchResponseVO data = globalSearchService.search(vo, visibleIds);
        RESTResult<GlobalSearchResponseVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // P1-1: 限流降级方法
    private RESTResult<GlobalSearchResponseVO> searchFallback(
            GlobalSearchRequestVO vo,
            HttpServletRequest request,
            io.github.resilience4j.ratelimiter.RequestNotPermitted ex) {
        return RESTResult.error(ErrorCode.RATE_LIMIT, "搜索请求过于频繁，请稍后再试");
    }
}
