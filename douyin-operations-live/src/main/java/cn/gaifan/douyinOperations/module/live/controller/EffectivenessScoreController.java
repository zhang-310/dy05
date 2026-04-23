package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.EffectivenessScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 直播话术效果评分 Controller
 * W-04: 效果评分系统
 */
@RestController("liveEffectivenessScoreController")
@RequestMapping("/api/v1/live/effectiveness")
@Tag(name = "直播效果评分 / Live Effectiveness Score", description = "话术效果评分、版本对比、排行榜")
public class EffectivenessScoreController {

    private static final Logger log = LoggerFactory.getLogger(EffectivenessScoreController.class);

    @Resource
    private EffectivenessScoreService effectivenessScoreService;

    @PostMapping("/calculate")
    @Operation(summary = "计算单个话术效果评分 / Calculate Script Score")
    public RESTResult<Map<String, Object>> calculateScore(@RequestBody Map<String, Object> body) {
        Long scriptId = body.get("scriptId") != null ? ((Number) body.get("scriptId")).longValue() : null;
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;

        if (scriptId == null || sessionId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "scriptId 和 sessionId 不能为空");
        }

        try {
            Map<String, Object> result = effectivenessScoreService.calculateScore(scriptId, sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("计算评分失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "计算评分失败");
        }
    }

    @PostMapping("/session-ranking")
    @Operation(summary = "计算直播场次所有话术排行榜 / Calculate Session Ranking")
    public RESTResult<List<Map<String, Object>>> calculateSessionRanking(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;

        if (sessionId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        }

        try {
            List<Map<String, Object>> result = effectivenessScoreService.calculateSessionRanking(sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("计算排行榜失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "计算排行榜失败");
        }
    }

    @PostMapping("/compare")
    @Operation(summary = "版本对比 / Compare Two Versions")
    public RESTResult<Map<String, Object>> compareVersions(@RequestBody Map<String, Object> body) {
        Long versionA = body.get("versionA") != null ? ((Number) body.get("versionA")).longValue() : null;
        Long versionB = body.get("versionB") != null ? ((Number) body.get("versionB")).longValue() : null;

        if (versionA == null || versionB == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "versionA 和 versionB 不能为空");
        }

        try {
            Map<String, Object> result = effectivenessScoreService.compareVersions(versionA, versionB);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("版本对比失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "版本对比失败");
        }
    }

    @PostMapping("/ranking")
    @Operation(summary = "获取排行榜 / Get Ranking")
    public RESTResult<PageResultVO<Map<String, Object>>> getRanking(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int pageSize = body.get("pageSize") != null ? ((Number) body.get("pageSize")).intValue() : 30;

        if (sessionId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        }

        try {
            PageResultVO<Map<String, Object>> result = effectivenessScoreService.getRanking(sessionId, page, pageSize);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取排行榜失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取排行榜失败");
        }
    }

    @PostMapping("/top-scripts")
    @Operation(summary = "获取热门话术 / Get Top Scripts")
    public RESTResult<List<Map<String, Object>>> getTopScripts(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        int limit = body.get("limit") != null ? ((Number) body.get("limit")).intValue() : 3;

        if (sessionId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        }

        try {
            List<Map<String, Object>> result = effectivenessScoreService.getTopScripts(sessionId, limit);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取热门话术失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取热门话术失败");
        }
    }

    @PostMapping("/recommended-scripts")
    @Operation(summary = "获取推荐话术 / Get Recommended Scripts")
    public RESTResult<List<Map<String, Object>>> getRecommendedScripts(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;

        if (sessionId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        }

        try {
            List<Map<String, Object>> result = effectivenessScoreService.getRecommendedScripts(sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取推荐话术失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取推荐话术失败");
        }
    }

    @PostMapping("/emerged-scripts")
    @Operation(summary = "获取新兴话术 / Get Emerged Scripts")
    public RESTResult<List<Map<String, Object>>> getEmergedScripts(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;

        if (sessionId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        }

        try {
            List<Map<String, Object>> result = effectivenessScoreService.getEmergedScripts(sessionId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取新兴话术失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取新兴话术失败");
        }
    }

    @PostMapping("/script-effectiveness")
    @Operation(summary = "获取单个话术效果详情 / Get Script Effectiveness")
    public RESTResult<Map<String, Object>> getScriptEffectiveness(@RequestBody Map<String, Object> body) {
        Long scriptId = body.get("scriptId") != null ? ((Number) body.get("scriptId")).longValue() : null;

        if (scriptId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "scriptId 不能为空");
        }

        try {
            Map<String, Object> result = effectivenessScoreService.getScriptEffectiveness(scriptId);
            return RESTResult.ok(result);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取效果详情失败", e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取效果详情失败");
        }
    }
}
