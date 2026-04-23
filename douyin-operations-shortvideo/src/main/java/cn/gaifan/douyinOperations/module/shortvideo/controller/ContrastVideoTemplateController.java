package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContrastVideoTemplateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/short-video/contrast-template")
public class ContrastVideoTemplateController {

    @Resource
    private ContrastVideoTemplateService contrastVideoTemplateService;

    @PostMapping("/shot-template")
    public RESTResult<Map<String, Object>> getShotTemplate(@RequestBody Map<String, Object> params) {
        String type = (String) params.getOrDefault("contrastType", "social_identity_contrast");
        int duration = params.containsKey("duration") ? ((Number) params.get("duration")).intValue() : 25;
        return RESTResult.success(contrastVideoTemplateService.getShotTemplate(type, duration));
    }

    @PostMapping("/comedy-config")
    public RESTResult<Map<String, Object>> getComedyConfig(@RequestBody Map<String, Object> params) {
        String type = (String) params.getOrDefault("contrastType", "social_identity_contrast");
        return RESTResult.success(contrastVideoTemplateService.getComedyConfig(type));
    }

    @PostMapping("/bgm-strategy")
    public RESTResult<Map<String, Object>> getBgmStrategy(@RequestBody Map<String, Object> params) {
        String type = (String) params.getOrDefault("contrastType", "social_identity_contrast");
        return RESTResult.success(contrastVideoTemplateService.getBgmStrategy(type));
    }

    @PostMapping("/list")
    public RESTResult<List<Map<String, Object>>> listTemplates() {
        return RESTResult.success(contrastVideoTemplateService.listTemplates());
    }

    @PostMapping("/preset-template")
    public RESTResult<Map<String, Object>> getPresetTemplate(@RequestBody Map<String, Object> params) {
        String type = (String) params.getOrDefault("contrastType", "social_identity_contrast");
        String preset = (String) params.getOrDefault("preset", "25s");
        return RESTResult.success(contrastVideoTemplateService.getPresetTemplate(type, preset));
    }
}
