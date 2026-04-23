package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.StyleRecommendService;
import cn.gaifan.douyinOperations.module.live.vo.StyleRecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 直播话术风格推荐 #29：基于历史效果推荐最优风格
 */
@RestController
@RequestMapping("/api/v1/live/style")
@Tag(name = "直播风格推荐 / Live Style Recommend", description = "基于历史话术效果推荐风格")
public class LiveStyleController {

    @Resource
    private StyleRecommendService styleRecommendService;

    @PostMapping("/recommend")
    @Operation(summary = "智能风格推荐（按历史 effectiveness_score 聚合）")
    public RESTResult<List<StyleRecommendationVO>> recommend(HttpServletRequest request,
            @RequestBody(required = false) StyleRecommendRequest body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long productId = body != null ? body.getProductId() : null;
        int limit = (body != null && body.getLimit() != null && body.getLimit() > 0) ? body.getLimit() : 10;
        List<StyleRecommendationVO> list = styleRecommendService.recommendStyles(userId, productId, limit);
        RESTResult<List<StyleRecommendationVO>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @lombok.Data
    public static class StyleRecommendRequest {
        private Long productId;
        private Integer limit;
    }
}
