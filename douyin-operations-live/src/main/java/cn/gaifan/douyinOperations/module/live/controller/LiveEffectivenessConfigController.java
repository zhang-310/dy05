package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LiveEffectivenessConfig;
import cn.gaifan.douyinOperations.module.live.service.LiveEffectivenessConfigService;
import cn.gaifan.douyinOperations.module.live.vo.LiveEffectivenessConfigSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveEffectivenessConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 直播效果评分权重配置 Controller
 * Q3-5: Configurable Effectiveness Score Formula
 */
@RestController
@RequestMapping("/api/v1/live/effectiveness-config")
@Tag(name = "直播效果评分配置 / Live Effectiveness Config", description = "效果评分公式权重的自定义配置")
public class LiveEffectivenessConfigController {

    private static final Logger log = LoggerFactory.getLogger(LiveEffectivenessConfigController.class);

    @Resource
    private LiveEffectivenessConfigService configService;

    @PostMapping("/list")
    @Operation(summary = "查询用户的所有评分配置 / List User Effectiveness Configs")
    public RESTResult<List<LiveEffectivenessConfigVO>> list(@CurrentUserId Long userId) {
        try {
            List<LiveEffectivenessConfig> configs = configService.list(userId);
            List<LiveEffectivenessConfigVO> voList = configs.stream()
                    .map(LiveEffectivenessConfigVO::fromEntity)
                    .collect(Collectors.toList());
            return RESTResult.ok(voList);
        } catch (Exception e) {
            log.error("查询评分配置失败, userId={}", userId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "查询评分配置失败");
        }
    }

    @PostMapping("/save")
    @Operation(summary = "创建或更新评分配置 / Save Effectiveness Config")
    public RESTResult<LiveEffectivenessConfigVO> save(
            @CurrentUserId Long userId,
            @Valid @RequestBody LiveEffectivenessConfigSaveVO vo) {
        try {
            LiveEffectivenessConfig saved = configService.save(vo, userId);
            return RESTResult.ok(LiveEffectivenessConfigVO.fromEntity(saved));
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("保存评分配置失败, userId={}", userId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "保存评分配置失败");
        }
    }

    @PostMapping("/default")
    @Operation(summary = "获取用户默认评分配置 / Get Default Effectiveness Config")
    public RESTResult<LiveEffectivenessConfigVO> getDefault(@CurrentUserId Long userId) {
        try {
            LiveEffectivenessConfig config = configService.getDefaultConfig(userId);
            return RESTResult.ok(LiveEffectivenessConfigVO.fromEntity(config));
        } catch (Exception e) {
            log.error("获取默认评分配置失败, userId={}", userId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "获取默认评分配置失败");
        }
    }

    @PostMapping("/set-default")
    @Operation(summary = "设置默认评分配置 / Set Default Effectiveness Config")
    public RESTResult<Void> setDefault(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        Long configId = body.get("configId") != null ? ((Number) body.get("configId")).longValue() : null;
        if (configId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "configId 不能为空");
        }
        try {
            configService.setDefault(configId, userId);
            return RESTResult.success();
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("设置默认评分配置失败, userId={}, configId={}", userId, configId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "设置默认评分配置失败");
        }
    }

    @PostMapping("/delete")
    @Operation(summary = "删除评分配置 / Delete Effectiveness Config")
    public RESTResult<Integer> delete(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        Long configId = body.get("configId") != null ? ((Number) body.get("configId")).longValue() : null;
        if (configId == null) {
            return RESTResult.fail(ErrorCode.VALIDATION_FAIL, "configId 不能为空");
        }
        try {
            configService.delete(configId, userId);
            return RESTResult.deleteSuccess(1);
        } catch (BusinessException e) {
            return RESTResult.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除评分配置失败, userId={}, configId={}", userId, configId, e);
            return RESTResult.fail(ErrorCode.SYSTEM_BUSY, "删除评分配置失败");
        }
    }
}
