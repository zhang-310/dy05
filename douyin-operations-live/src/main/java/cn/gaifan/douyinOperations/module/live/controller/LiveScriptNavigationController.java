package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 直播实时话术导航 Controller
 * W-03: 实时辅助面板核心功能
 */
@RestController
@RequestMapping("/api/v1/live/script-navigation")
@Tag(name = "直播实时话术 / Live Script Navigation", description = "实时话术导航、倒计时、数据推送")
public class LiveScriptNavigationController {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptNavigationController.class);

    @Resource
    private LiveMonitorService liveMonitorService;

    @GetMapping("/current-slot/{sessionId}")
    @Operation(summary = "获取当前话术段 / Get Current Script Slot")
    public RESTResult<Map<String, Object>> getCurrentSlot(@PathVariable Long sessionId) {
        try {
            Map<String, Object> result = liveMonitorService.getCurrentSlot(sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取当前话术失败: sessionId={}", sessionId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取当前话术失败");
        }
    }

    @GetMapping("/scripts/{sessionId}")
    @Operation(summary = "获取所有话术列表 / Get All Script List")
    public RESTResult<List<Map<String, Object>>> getScriptSlots(@PathVariable Long sessionId) {
        try {
            List<Map<String, Object>> result = liveMonitorService.getScriptSlots(sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取话术列表失败: sessionId={}", sessionId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取话术列表失败");
        }
    }

    @GetMapping("/metrics/{sessionId}")
    @Operation(summary = "获取实时数据 / Get Realtime Metrics")
    public RESTResult<Map<String, Object>> getRealtimeMetrics(@PathVariable Long sessionId) {
        try {
            Map<String, Object> result = liveMonitorService.getRealtimeMetrics(sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取实时数据失败: sessionId={}", sessionId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取实时数据失败");
        }
    }

    @PostMapping("/next/{sessionId}")
    @Operation(summary = "跳转到下一话术段 / Move to Next Slot")
    public RESTResult<Map<String, Object>> nextSlot(@PathVariable Long sessionId) {
        try {
            Map<String, Object> result = liveMonitorService.nextSlot(sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("跳转到下一话术失败: sessionId={}", sessionId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "跳转到下一话术失败");
        }
    }

    @PostMapping("/skip/{sessionId}")
    @Operation(summary = "跳转到指定话术段 / Skip to Slot")
    public RESTResult<Map<String, Object>> skipToSlot(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        try {
            int slotIndex = body.get("slotIndex") != null ? ((Number) body.get("slotIndex")).intValue() : 0;
            Map<String, Object> result = liveMonitorService.skipToSlot(sessionId, slotIndex);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("跳转到指定话术失败: sessionId={}", sessionId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "跳转到指定话术失败");
        }
    }
}
