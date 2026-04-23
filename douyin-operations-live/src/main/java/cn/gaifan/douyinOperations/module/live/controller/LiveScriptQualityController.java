package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.copy.service.CopyLibraryService;
import cn.gaifan.douyinOperations.module.copy.vo.CopyLibrarySaveVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.live.vo.SaveToCopyVO;
import cn.gaifan.douyinOperations.module.live.vo.ScriptIdVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 直播话术质量 Controller（违规检测、合规、文案库、质量评估）
 */
@RestController
@RequestMapping("/api/v1/live/ai")
@Tag(name = "直播话术质量 / Script Quality", description = "话术违规检测、合规入库、质量评估（需登录）")
public class LiveScriptQualityController {

    @Resource
    private LiveAiService liveAiService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AiCallLogService aiCallLogService;

    @Resource
    private CopyLibraryService copyLibraryService;

    @Resource
    private LiveScriptQualityService liveScriptQualityService;

    @Autowired(required = false)
    private LiveScriptRepository liveScriptRepository;

    @Autowired(required = false)
    private DyProductRepository dyProductRepository;

    @PostMapping("/check-violation-enhanced")
    @Operation(summary = "增强合规检测 / Enhanced Compliance Check",
            description = "在基础违禁词检测之上，叠加品类禁售校验、价格一致性检测、售后承诺高风险检测")
    public RESTResult<LiveAiResultVO.ViolationCheckResult> checkViolationEnhanced(
            HttpServletRequest request,
            @Valid @RequestBody ScriptIdVO vo) {
        requireUserId(request);
        if (liveScriptRepository == null) {
            return withTraceId(RESTResult.fail(ErrorCode.DATA_NOT_FOUND, "话术服务不可用"));
        }
        LiveScript script = liveScriptRepository.findById(vo.getScriptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        DyProduct product = null;
        if (script.getProductId() != null && dyProductRepository != null) {
            product = dyProductRepository.findById(script.getProductId()).orElse(null);
        }
        DyProduct finalProduct = product;
        LiveAiResultVO.ViolationCheckResult data = liveScriptQualityService.checkViolationEnhanced(
                script.getScriptContent(), finalProduct);
        return withTraceId(RESTResult.getSuccess(data));
    }

    // ==================== 违规检测 ====================

    @PostMapping("/check-violation")
    @Operation(summary = "话术违规检测 / Check Script Violation")
    public RESTResult<LiveAiResultVO.ViolationCheckResult> checkViolation(HttpServletRequest request,
                                                                          @Valid @RequestBody ScriptIdVO vo) {
        Long userId = requireUserId(request);
        LiveAiResultVO.ViolationCheckResult data = withAiCall(userId, "live_script_violation",
                () -> liveAiService.checkViolation(userId, vo.getScriptId()));
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/save-to-copy-if-passed")
    @Operation(summary = "审核过关一键到文案库 / Save to Copy Library if Passed")
    public RESTResult<Map<String, Object>> saveToCopyIfPassed(HttpServletRequest request,
                                                              @Valid @RequestBody SaveToCopyVO vo) {
        Long userId = requireUserId(request);
        String content = vo.getContent();
        String title = vo.getTitle();
        LiveAiResultVO.ViolationCheckResult check = liveAiService.checkViolationByContent(content, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", check.isPassed());
        result.put("violationCount", check.getViolationCount());
        result.put("violations", check.getViolations());
        if (!check.isPassed()) {
            return withTraceId(RESTResult.getSuccess(result));
        }
        String finalTitle = (title != null && !title.isBlank()) ? title : "直播话术-" + System.currentTimeMillis();
        CopyLibrarySaveVO copyVo = new CopyLibrarySaveVO();
        copyVo.setUserId(userId);
        copyVo.setTitle(finalTitle);
        copyVo.setContent(content);
        copyVo.setCategory("live");
        copyVo.setTags("直播,AI生成");
        copyVo.setStatus(1);
        long copyId = copyLibraryService.save(copyVo);
        result.put("copyId", copyId);
        return withTraceId(RESTResult.getSuccess(result));
    }

    // ==================== 私有辅助方法 ====================

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> T withAiCall(Long userId, String callType, Supplier<T> supplier) {
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            T result = supplier.get();
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
            return result;
        } catch (Exception e) {
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 0, e.getMessage(), false, null, null));
            throw e;
        }
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
