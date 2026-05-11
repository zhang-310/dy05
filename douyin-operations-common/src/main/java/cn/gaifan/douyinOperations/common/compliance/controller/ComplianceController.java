package cn.gaifan.douyinOperations.common.compliance.controller;

import cn.gaifan.douyinOperations.common.compliance.service.ComplianceService;
import cn.gaifan.douyinOperations.common.compliance.vo.ComplianceCheckResult;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

/**
 * 违规检测 Controller
 */
@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    @Resource
    private ComplianceService complianceService;

    /**
     * 检测内容是否违规
     */
    @PostMapping("/check")
    public RESTResult<ComplianceCheckResult> check(
            HttpServletRequest request,
            @Valid @RequestBody CheckRequest req
    ) {
        Long userId = getUserIdFromRequest(request);
        ComplianceCheckResult result = complianceService.check(
                userId,
                req.getContentType(),
                req.getContentId(),
                req.getContent()
        );
        return RESTResult.success(result);
    }

    /**
     * 从请求中获取用户 ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        return null;
    }

    /**
     * 关键词匹配检测
     */
    @PostMapping("/check-keywords")
    public RESTResult<ComplianceCheckResult> checkKeywords(@Valid @RequestBody CheckRequest req) {
        ComplianceCheckResult result = complianceService.checkKeywords(req.getContent());
        return RESTResult.success(result);
    }

    /**
     * 正则表达式匹配检测
     */
    @PostMapping("/check-patterns")
    public RESTResult<ComplianceCheckResult> checkPatterns(@Valid @RequestBody CheckRequest req) {
        ComplianceCheckResult result = complianceService.checkPatterns(req.getContent());
        return RESTResult.success(result);
    }

    /**
     * 语义检测
     */
    @PostMapping("/check-semantic")
    public RESTResult<ComplianceCheckResult> checkSemantic(@Valid @RequestBody CheckRequest req) {
        ComplianceCheckResult result = complianceService.checkSemantic(req.getContent());
        return RESTResult.success(result);
    }

    /**
     * 检测请求 VO
     */
    @Data
    public static class CheckRequest {
        /**
         * 内容类型: script=话术, video=短视频, material=素材
         */
        private String contentType;

        /**
         * 内容 ID（可选）
         */
        private Long contentId;

        /**
         * 检测内容
         */
        private String content;
    }
}
