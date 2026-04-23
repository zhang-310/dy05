package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.config.AppComplianceProperties;
import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.IndustryComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "行业合规检测 / Industry Compliance")
@RestController
@RequestMapping("/api/v1/script/compliance")
public class ComplianceController {

    @Resource
    private IndustryComplianceService industryComplianceService;

    @Resource
    private AppComplianceProperties appComplianceProperties;

    @PostMapping("/check")
    @Operation(summary = "合规检测")
    public RESTResult<List<Map<String, Object>>> check(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        String text = body.getOrDefault("text", "");
        String industryCode = body.getOrDefault("industryCode", "cosmetics");
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(industryComplianceService.checkCompliance(text, industryCode));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/rules")
    @Operation(summary = "获取行业合规规则")
    public RESTResult<List<Map<String, Object>>> rules(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        String industryCode = body.getOrDefault("industryCode", "cosmetics");
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(industryComplianceService.listRules(industryCode));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 列出已配置的垂直行业编码；检测时另含全业通用规则（GENERAL_AD_RULES）与抖音公开摘要正则（对所有行业生效）。
     */
    @PostMapping("/industry-codes")
    @Operation(summary = "已支持的垂直行业合规编码列表")
    public RESTResult<Map<String, Object>> industryCodes(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = new HashMap<>();
        data.put("verticalCodes", industryComplianceService.listSupportedIndustryCodes());
        data.put("defaultVerticalCode", "cosmetics");
        data.put("note", "任意检测均叠加：全业通用摘要 + 抖音公开摘要；未知 verticalCode 仅通用+抖音公开（不套用垂直包）");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 返回抖音等平台公开规则文档入口（配置于 app.compliance.douyin），便于运营对照维护词库；不做页面爬取。
     */
    @PostMapping("/douyin-official-references")
    @Operation(summary = "抖音公开规则文档入口（配置列表）")
    public RESTResult<Map<String, Object>> douyinOfficialReferences(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        AppComplianceProperties.Douyin d = appComplianceProperties.getDouyin();
        Map<String, Object> data = new HashMap<>();
        data.put("notice", d.getNotice());
        data.put("referenceUrls", d.getReferenceUrls() != null ? d.getReferenceUrls() : List.of());
        data.put("hint", "词库 Flyway V067/V108（sc_compliance_word）；多行业正则见 IndustryComplianceServiceImpl；垂直编码见 POST .../compliance/industry-codes");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
