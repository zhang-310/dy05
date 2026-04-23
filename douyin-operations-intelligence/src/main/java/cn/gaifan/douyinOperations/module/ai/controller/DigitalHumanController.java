package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.DigitalHumanProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "数字人 / Digital Human")
@RestController
@RequestMapping("/api/v1/ai/digital-human")
public class DigitalHumanController {

    @Autowired(required = false)
    private DigitalHumanProvider digitalHumanProvider;

    @PostMapping("/status")
    @Operation(summary = "数字人服务状态")
    public RESTResult<Map<String, Object>> status(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        boolean configured = digitalHumanProvider != null && digitalHumanProvider.isConfigured();
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of(
                "available", configured,
                "provider", configured ? digitalHumanProvider.name() : "none"
        ));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate")
    @Operation(summary = "生成数字人口播视频")
    public RESTResult<Map<String, Object>> generate(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (digitalHumanProvider == null || !digitalHumanProvider.isConfigured()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "数字人服务未配置");
        }
        String avatarId = body.getOrDefault("avatarId", "default");
        String scriptText = body.get("scriptText");
        String voiceId = body.getOrDefault("voiceId", "zh-CN-XiaoxiaoNeural");
        if (scriptText == null || scriptText.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "scriptText 不能为空");
        }
        String videoUrl = digitalHumanProvider.generateTalkingHead(avatarId, scriptText, voiceId);
        boolean success = videoUrl != null && !videoUrl.isBlank();
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of(
                "videoUrl", success ? videoUrl : "",
                "provider", digitalHumanProvider.name(),
                "success", success,
                "message", success ? "生成成功" : "生成失败"
        ));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
