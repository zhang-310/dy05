package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoQuickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 快速生成 API（新手模式 3 步）
 * 路径：/api/v1/short-video/quick
 */
@RestController
@RequestMapping("/api/v1/short-video/quick")
@Tag(name = "快速生成", description = "3 步一键生成：主题 + 内容 + 风格")
public class ShortVideoQuickController {

    @Resource
    private ShortVideoQuickService quickService;

    @PostMapping("/generate")
    @Operation(summary = "一键生成")
    public RESTResult<Map<String, Object>> generate(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String theme = body != null && body.get("theme") instanceof String s ? s : "美食";
        String keywords = body != null && body.get("keywords") instanceof String s ? s : "";
        String style = body != null && body.get("style") instanceof String s ? s : "温馨";
        Map<String, Object> data = quickService.quickGenerate(theme, keywords, style, userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
