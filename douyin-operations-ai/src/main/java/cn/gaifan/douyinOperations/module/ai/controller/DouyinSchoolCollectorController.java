package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.official.DouyinSchoolCollectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Tag(name = "AI 官方知识采集")
@RestController
@RequestMapping("/api/v1/ai/admin/douyin-school")
public class DouyinSchoolCollectorController {

    private static final Logger log = LoggerFactory.getLogger(DouyinSchoolCollectorController.class);
    private final AtomicBoolean manualRunning = new AtomicBoolean(false);

    @Resource
    private DouyinSchoolCollectorService collectorService;

    @Resource
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @GetMapping("/status")
    @Operation(summary = "查看抖音电商学习中心官方资料采集状态")
    public RESTResult<Map<String, Object>> status(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(collectorService.status());
    }

    @GetMapping("/topics")
    @Operation(summary = "查看抖音电商学习中心官方资料采集专题")
    public RESTResult<Map<String, Object>> topics(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(collectorService.topics());
    }

    @PostMapping("/collect")
    @Operation(summary = "手动触发抖音电商学习中心官方资料采集")
    public RESTResult<Map<String, Object>> collect(HttpServletRequest request) {
        return submitCollect(request, null);
    }

    @PostMapping("/collect-topic")
    @Operation(summary = "手动触发抖音电商学习中心官方资料专题采集")
    public RESTResult<Map<String, Object>> collectTopic(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String topicCode = body == null ? null : String.valueOf(body.getOrDefault("topicCode", ""));
        return submitCollect(request, topicCode);
    }

    private RESTResult<Map<String, Object>> submitCollect(HttpServletRequest request, String topicCode) {
        requireAdmin(request);
        if (!manualRunning.compareAndSet(false, true)) {
            RESTResult<Map<String, Object>> result = RESTResult.getSuccess(Map.of(
                    "accepted", false,
                    "running", true,
                    "message", "抖音学习中心官方资料采集已在后台运行"
            ));
            result.setTraceId(MDC.get("traceId"));
            return result;
        }

        String traceId = MDC.get("traceId");
        taskExecutor.execute(() -> {
            try {
                log.info("手动触发抖音学习中心官方资料采集开始 traceId={}, topic={}", traceId, topicCode);
                Map<String, Object> collectResult = topicCode == null || topicCode.isBlank()
                        ? collectorService.collect()
                        : collectorService.collectTopic(topicCode);
                log.info("手动触发抖音学习中心官方资料采集完成 traceId={}, result={}", traceId, collectResult);
            } catch (Exception e) {
                log.error("手动触发抖音学习中心官方资料采集失败 traceId={}", traceId, e);
            } finally {
                manualRunning.set(false);
            }
        });

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accepted", true);
        payload.put("running", true);
        payload.put("topicCode", topicCode == null ? "full" : topicCode);
        payload.put("startedAt", Instant.now().toString());
        payload.put("message", "抖音学习中心官方资料采集已提交后台执行，请稍后查看日志和知识库入库结果");
        RESTResult<Map<String, Object>> result = RESTResult.getSuccess(payload);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
