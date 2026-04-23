package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LivePlatform;
import cn.gaifan.douyinOperations.module.live.service.LivePlatformRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/live/platform")
@Tag(name = "直播平台规则", description = "多平台规则引擎")
public class LivePlatformRuleController {

    @Resource
    private LivePlatformRuleService platformRuleService;

    @PostMapping("/list")
    @Operation(summary = "获取所有启用的平台列表")
    public RESTResult<List<LivePlatform>> listPlatforms() {
        return RESTResult.success(platformRuleService.listActivePlatforms());
    }

    @PostMapping("/violation-check")
    @Operation(summary = "平台级违禁词检查")
    public RESTResult<List<Map<String, Object>>> checkViolation(@Valid @RequestBody ViolationCheckVO vo) {
        return RESTResult.success(platformRuleService.checkPlatformViolation(vo.getText(), vo.getPlatformCode()));
    }

    @PostMapping("/prompt-template")
    @Operation(summary = "获取平台 prompt 模板片段")
    public RESTResult<String> getPromptTemplate(@Valid @RequestBody PlatformCodeVO vo) {
        return RESTResult.success(platformRuleService.getPlatformPromptTemplate(vo.getPlatformCode()));
    }

    @Data
    public static class ViolationCheckVO {
        @NotBlank(message = "文本不能为空")
        private String text;
        @NotBlank(message = "平台编码不能为空")
        private String platformCode;
    }

    @Data
    public static class PlatformCodeVO {
        @NotBlank(message = "平台编码不能为空")
        private String platformCode;
    }
}
