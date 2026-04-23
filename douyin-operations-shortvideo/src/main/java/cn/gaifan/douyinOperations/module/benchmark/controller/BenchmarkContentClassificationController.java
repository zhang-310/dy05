package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkContentClassificationService;
import cn.gaifan.douyinOperations.module.benchmark.vo.ContentClassificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容分类验证控制器
 */
@Tag(name = "对标账号 - 内容分类验证")
@RestController
@RequestMapping("/api/v1/benchmark/classification")
@RequiredArgsConstructor
public class BenchmarkContentClassificationController {

    private final BenchmarkContentClassificationService classificationService;

    @Operation(summary = "验证内容分类")
    @PostMapping("/validate")
    public RESTResult<ContentClassificationVO> validateClassification(@RequestBody ValidateRequest request) {
        ContentClassificationVO result = classificationService.validateClassification(
                request.getScriptContent(),
                request.getTargetIndustry(),
                request.getTargetSceneType(),
                request.getOwnerId()
        );
        return RESTResult.success(result);
    }

    @Operation(summary = "AI自动分类")
    @PostMapping("/auto-classify")
    public RESTResult<ContentClassificationVO> autoClassify(@RequestBody AutoClassifyRequest request) {
        ContentClassificationVO result = classificationService.autoClassify(
                request.getScriptContent(),
                request.getOwnerId()
        );
        return RESTResult.success(result);
    }

    @Operation(summary = "批量验证分类")
    @PostMapping("/batch-validate")
    public RESTResult<List<ContentClassificationVO>> batchValidate(@RequestBody BatchValidateRequest request) {
        List<ContentClassificationVO> results = classificationService.batchValidate(
                request.getScriptIds(),
                request.getOwnerId()
        );
        return RESTResult.success(results);
    }

    @Operation(summary = "获取行业分类字典")
    @PostMapping("/industries")
    public RESTResult<List<String>> getIndustryCategories() {
        return RESTResult.success(classificationService.getIndustryCategories());
    }

    @Operation(summary = "获取场景类型字典")
    @PostMapping("/scene-types")
    public RESTResult<List<String>> getSceneTypes() {
        return RESTResult.success(classificationService.getSceneTypes());
    }

    @Operation(summary = "获取脚本类型字典")
    @PostMapping("/script-types")
    public RESTResult<List<String>> getScriptTypes() {
        return RESTResult.success(classificationService.getScriptTypes());
    }

    @Operation(summary = "标记为需要人工审核")
    @PostMapping("/mark-review")
    public RESTResult<Void> markForManualReview(@RequestBody MarkReviewRequest request) {
        classificationService.markForManualReview(
                request.getScriptId(),
                request.getReason(),
                request.getOwnerId()
        );
        return RESTResult.success(null);
    }

    @Operation(summary = "人工确认分类")
    @PostMapping("/confirm")
    public RESTResult<Void> confirmClassification(@RequestBody ConfirmRequest request) {
        classificationService.confirmClassification(
                request.getScriptId(),
                request.getConfirmedIndustry(),
                request.getConfirmedSceneType(),
                request.getConfirmedScriptType(),
                request.getOwnerId()
        );
        return RESTResult.success(null);
    }

    // ==================== 请求VO ====================

    @Data
    public static class ValidateRequest {
        private String scriptContent;
        private String targetIndustry;
        private String targetSceneType;
        private Long ownerId;
    }

    @Data
    public static class AutoClassifyRequest {
        private String scriptContent;
        private Long ownerId;
    }

    @Data
    public static class BatchValidateRequest {
        private List<Long> scriptIds;
        private Long ownerId;
    }

    @Data
    public static class MarkReviewRequest {
        private Long scriptId;
        private String reason;
        private Long ownerId;
    }

    @Data
    public static class ConfirmRequest {
        private Long scriptId;
        private String confirmedIndustry;
        private String confirmedSceneType;
        private String confirmedScriptType;
        private Long ownerId;
    }
}
