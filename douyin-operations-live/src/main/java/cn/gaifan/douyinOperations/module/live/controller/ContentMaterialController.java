package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.ContentMaterialService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/live/material")
public class ContentMaterialController {

    @Resource
    private ContentMaterialService contentMaterialService;

    @PostMapping("/random")
    public RESTResult<List<Map<String, Object>>> getRandomMaterials(@RequestBody Map<String, Object> params) {
        String type = (String) params.getOrDefault("materialType", "joke");
        String category = (String) params.get("category");
        int count = params.containsKey("count") ? ((Number) params.get("count")).intValue() : 3;
        return RESTResult.success(contentMaterialService.getRandomMaterials(type, category, Math.min(count, 10)));
    }

    @PostMapping("/by-persona")
    public RESTResult<List<Map<String, Object>>> getMaterialsByPersona(@RequestBody Map<String, Object> params) {
        String type = (String) params.getOrDefault("materialType", "joke");
        String personaType = (String) params.get("personaType");
        String ageRange = (String) params.get("ageRange");
        int count = params.containsKey("count") ? ((Number) params.get("count")).intValue() : 3;
        return RESTResult.success(contentMaterialService.getMaterialsByPersona(type, personaType, ageRange, Math.min(count, 10)));
    }

    @PostMapping("/categories")
    public RESTResult<Map<String, List<String>>> getCategories() {
        return RESTResult.success(contentMaterialService.getMaterialCategories());
    }

    @PostMapping("/prompt")
    public RESTResult<String> buildMaterialPrompt(@RequestBody Map<String, String> params) {
        String type = params.getOrDefault("materialType", "joke");
        String category = params.get("category");
        return RESTResult.success(contentMaterialService.buildMaterialPrompt(type, category));
    }

    @PostMapping("/performance-prompt")
    public RESTResult<String> getPerformancePrompt(@RequestBody Map<String, String> params) {
        String category = params.get("category");
        return RESTResult.success(contentMaterialService.buildPerformancePrompt(category));
    }

    @PostMapping("/risk-match")
    public RESTResult<List<String>> matchRiskScripts(@RequestBody Map<String, String> params) {
        String riskType = params.get("riskType");
        return RESTResult.success(contentMaterialService.matchRiskScripts(riskType));
    }
}
