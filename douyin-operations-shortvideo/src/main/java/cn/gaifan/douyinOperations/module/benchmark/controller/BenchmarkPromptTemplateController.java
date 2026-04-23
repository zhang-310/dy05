package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkPromptTemplateService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Prompt 模板管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/benchmark/prompt-template")
@RequiredArgsConstructor
@Tag(name = "Benchmark - Prompt 模板管理", description = "Prompt 模板的 CRUD 和管理功能")
public class BenchmarkPromptTemplateController {

    private final BenchmarkPromptTemplateService promptTemplateService;

    @PostMapping("/search")
    @Operation(summary = "分页查询 Prompt 模板列表")
    public RESTResult<PageResultVO<BenchmarkPromptTemplateVO>> search(
            @RequestBody @Valid BenchmarkPromptTemplateSearchVO searchVO,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        PageResultVO<BenchmarkPromptTemplateVO> result = promptTemplateService.search(searchVO, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get")
    @Operation(summary = "根据 ID 查询 Prompt 模板详情")
    public RESTResult<BenchmarkPromptTemplateVO> getById(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long id = request.get("id");
        BenchmarkPromptTemplateVO result = promptTemplateService.getById(id, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/save")
    @Operation(summary = "保存 Prompt 模板（新增或更新）")
    public RESTResult<BenchmarkPromptTemplateVO> save(
            @RequestBody @Valid BenchmarkPromptTemplateSaveVO saveVO,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        BenchmarkPromptTemplateVO result = promptTemplateService.save(saveVO, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除 Prompt 模板")
    public RESTResult<Void> delete(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long id = request.get("id");
        promptTemplateService.delete(id, ownerId);
        return RESTResult.success(null);
    }

    @PostMapping("/toggle-active")
    @Operation(summary = "激活/停用 Prompt 模板")
    public RESTResult<Void> toggleActive(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long id = Long.parseLong(request.get("id").toString());
        Boolean isActive = (Boolean) request.get("isActive");
        promptTemplateService.toggleActive(id, isActive, ownerId);
        return RESTResult.success(null);
    }

    @PostMapping("/get-by-scene")
    @Operation(summary = "获取指定场景类型的激活模板列表")
    public RESTResult<List<BenchmarkPromptTemplateVO>> getActiveTemplatesByScene(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String sceneType = request.get("sceneType");
        List<BenchmarkPromptTemplateVO> result = promptTemplateService.getActiveTemplatesByScene(sceneType, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get-by-industry")
    @Operation(summary = "获取指定行业的激活模板列表")
    public RESTResult<List<BenchmarkPromptTemplateVO>> getActiveTemplatesByIndustry(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String industry = request.get("industry");
        List<BenchmarkPromptTemplateVO> result = promptTemplateService.getActiveTemplatesByIndustry(industry, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get-by-code")
    @Operation(summary = "根据模板编码获取模板")
    public RESTResult<BenchmarkPromptTemplateVO> getByTemplateCode(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String templateCode = request.get("templateCode");
        BenchmarkPromptTemplateVO result = promptTemplateService.getByTemplateCode(templateCode, ownerId);
        return RESTResult.success(result);
    }
}
