package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityScoringService;
import cn.gaifan.douyinOperations.module.live.service.ScriptQualityEvaluator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 话术质量多维度评估 Controller
 * 7维度10分制 + FIRE/DEPTH 法则评估 + 四维加权评分（合规/流畅/吸引力/关键词）
 */
@RestController
@RequestMapping("/api/v1/live/script-quality")
@Tag(name = "话术质量评估 / Script Quality Evaluation", description = "7维度10分制评估 + FIRE/DEPTH法则 + 四维加权评分")
public class ScriptQualityController {

    @Resource
    private ScriptQualityEvaluator scriptQualityEvaluator;

    @Autowired(required = false)
    private LiveScriptQualityScoringService qualityScoringService;

    @PostMapping("/evaluate")
    @Operation(summary = "多维度话术质量评估 / Multi-dimension Script Quality Evaluation",
            description = "7维度10分制（LLM）+ FIRE/DEPTH法则，适用于生成后人工评估")
    public RESTResult<Map<String, Object>> evaluate(@RequestBody Map<String, String> params,
                                                     HttpServletRequest request) {
        Long userId = requireUserId(request);
        String content = params.get("content");
        String ipType = params.get("ipType");

        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术内容不能为空");
        }

        Map<String, Object> result = scriptQualityEvaluator.evaluate(content, ipType, userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/score")
    @Operation(summary = "四维加权质量评分 / Four-dimension Weighted Quality Score",
            description = "合规(35%) + 流畅(25%) + 吸引力(25%) + 关键词密度(15%)，结果持久化到 live_script_quality_score")
    public RESTResult<Map<String, Object>> score(@RequestBody Map<String, Object> params,
                                                  @CurrentUserId Long userId) {
        if (qualityScoringService == null) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "质量评分服务暂不可用");
        }
        Object scriptIdObj = params.get("scriptId");
        if (scriptIdObj == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "scriptId 不能为空");
        }
        Long scriptId = scriptIdObj instanceof Number n ? n.longValue() : Long.parseLong(scriptIdObj.toString());
        Map<String, Object> result = qualityScoringService.scoreScript(scriptId, userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/score-session")
    @Operation(summary = "批量评分场次下所有话术 / Batch Score All Scripts in Session",
            description = "对场次下所有话术进行四维加权评分，异步写入 live_script_quality_score")
    public RESTResult<Map<String, Object>> scoreSession(@RequestBody Map<String, Object> params,
                                                         @CurrentUserId Long userId) {
        if (qualityScoringService == null) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "质量评分服务暂不可用");
        }
        Object sessionIdObj = params.get("sessionId");
        if (sessionIdObj == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        }
        Long sessionId = sessionIdObj instanceof Number n ? n.longValue() : Long.parseLong(sessionIdObj.toString());
        qualityScoringService.scoreSession(sessionId, userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of("sessionId", sessionId, "status", "ok"));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/tts-preview")
    @Operation(summary = "TTS 试听预览（P3-01）",
            description = "根据话术字数估算口播时长，返回时长信息与字数分析（不调用真实 TTS，节省成本）。" +
                    "可选：若集成了 TTS 服务（app.ai.tts.enabled=true），返回试听 URL。")
    public RESTResult<Map<String, Object>> ttsPreview(@RequestBody Map<String, Object> params,
                                                       @CurrentUserId Long userId) {
        Object contentObj = params.get("content");
        if (contentObj == null || contentObj.toString().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "content 不能为空");
        }
        String content = contentObj.toString();
        // 按 4.5 字/秒估算口播时长（与 LiveScriptQualityServiceImpl.checkDurationFit 一致）
        double charsPerSecond = 4.5;
        int charCount = content.replaceAll("\\s+", "").length();
        double estimatedSec = charCount / charsPerSecond;
        int estimatedMin = (int) (estimatedSec / 60);
        int remainSec = (int) (estimatedSec % 60);
        String durationStr = estimatedMin > 0
                ? String.format("%d分%d秒", estimatedMin, remainSec)
                : String.format("%d秒", (int) Math.ceil(estimatedSec));
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("charCount", charCount);
        result.put("estimatedSec", Math.round(estimatedSec * 10.0) / 10.0);
        result.put("estimatedDuration", durationStr);
        result.put("charsPerSecond", charsPerSecond);
        result.put("ttsEnabled", false);
        result.put("ttsUrl", null);
        // 时长建议
        if (estimatedSec < 3) {
            result.put("durationTip", "话术过短，建议补充内容");
        } else if (estimatedSec > 300) {
            result.put("durationTip", "话术较长（超5分钟），建议分段或精简");
        } else {
            result.put("durationTip", "时长适中");
        }
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
