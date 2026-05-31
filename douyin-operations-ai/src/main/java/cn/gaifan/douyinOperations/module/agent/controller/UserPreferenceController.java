package cn.gaifan.douyinOperations.module.agent.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.agent.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "智能体 / Agent", description = "用户偏好、REFINE_SUGGESTIONS")
public class UserPreferenceController {

    @Resource
    private UserPreferenceService userPreferenceService;

    @PostMapping("/preference/refine-suggestions")
    @Operation(summary = "获取话术迭代建议（优先展示用户常用 instruction、script_type）")
    public RESTResult<Map<String, Object>> getRefineSuggestions(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int lim = limit != null && limit > 0 && limit <= 50 ? limit : 10;
        var instructions = userPreferenceService.getTopPreferences(userId, "instruction_used", lim);
        var scriptTypes = userPreferenceService.getTopPreferences(userId, "script_type", lim);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of(
                "instructionUsed", instructions,
                "scriptType", scriptTypes
        ));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
