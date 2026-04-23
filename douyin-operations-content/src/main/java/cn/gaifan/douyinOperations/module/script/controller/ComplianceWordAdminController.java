package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.ComplianceWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 合规词库管理（Admin）
 * 支持刷新内存缓存，供运营修改 sc_compliance_word 后调用
 */
@RestController
@RequestMapping("/api/v1/script/admin/compliance")
@Tag(name = "合规词库管理（Admin）", description = "绝对化用语、医疗功效词，支持刷新缓存")
public class ComplianceWordAdminController {

    @Resource
    private ComplianceWordService complianceWordService;

    @PostMapping("/refresh")
    @Operation(summary = "刷新合规词库缓存")
    public RESTResult<Void> refresh(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        complianceWordService.refresh();
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
