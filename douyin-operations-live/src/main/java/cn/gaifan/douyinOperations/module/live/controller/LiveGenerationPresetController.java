package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationPreset;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationPresetService;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationPresetSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationPresetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生成配置预设 Controller
 */
@RestController
@RequestMapping("/api/v1/live/generation-preset")
@Tag(name = "话术生成预设 / Generation Preset", description = "话术生成配置预设管理（需登录）")
public class LiveGenerationPresetController {

    @Resource
    private LiveGenerationPresetService presetService;

    @PostMapping("/list")
    @Operation(summary = "获取用户的预设列表 / List User Presets")
    public RESTResult<List<LiveGenerationPresetVO>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        List<LiveGenerationPreset> presets = presetService.list(userId);
        List<LiveGenerationPresetVO> voList = presets.stream().map(this::toVO).collect(Collectors.toList());
        return withTraceId(RESTResult.getSuccess(voList));
    }

    @PostMapping("/save")
    @Operation(summary = "保存预设 / Save Preset", description = "创建或更新话术生成配置预设（需登录）")
    public RESTResult<LiveGenerationPresetVO> save(HttpServletRequest request,
            @Valid @RequestBody LiveGenerationPresetSaveVO vo) {
        Long userId = requireUserId(request);
        LiveGenerationPreset preset = presetService.save(vo, userId);
        return withTraceId(RESTResult.addSuccess(toVO(preset)));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除预设 / Delete Preset")
    public RESTResult<Integer> delete(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long id = parseId(body);
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = requireUserId(request);
        presetService.delete(id, userId);
        return withTraceId(RESTResult.deleteSuccess(1));
    }

    @PostMapping("/getDefault")
    @Operation(summary = "获取默认预设 / Get Default Preset")
    public RESTResult<LiveGenerationPresetVO> getDefault(HttpServletRequest request) {
        Long userId = requireUserId(request);
        LiveGenerationPreset preset = presetService.getDefault(userId);
        return withTraceId(RESTResult.getSuccess(preset != null ? toVO(preset) : null));
    }

    @PostMapping("/set-default")
    @Operation(summary = "设为默认预设 / Set Default Preset")
    public RESTResult<Void> setDefault(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long id = parseId(body);
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = requireUserId(request);
        presetService.setDefault(id, userId);
        return withTraceId(RESTResult.updateSuccess(null));
    }

    private LiveGenerationPresetVO toVO(LiveGenerationPreset entity) {
        LiveGenerationPresetVO vo = new LiveGenerationPresetVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setStyle(entity.getStyle());
        vo.setModelId(entity.getModelId());
        vo.setUseKbRef(entity.getUseKbRef());
        vo.setDurationMode(entity.getDurationMode());
        vo.setHotKeywords(entity.getHotKeywords());
        vo.setIpType(entity.getIpType());
        vo.setMaterialType(entity.getMaterialType());
        vo.setScriptModule(entity.getScriptModule());
        vo.setRetentionStrategy(entity.getRetentionStrategy());
        vo.setInteractionLevel(entity.getInteractionLevel());
        vo.setIsDefault(entity.getIsDefault());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseId(Map<String, Object> body) {
        if (body == null) return null;
        Object v = body.get("id");
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }
}
