package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkScriptSimilarityService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 质量脚本语义相似度 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/benchmark/script-similarity")
@RequiredArgsConstructor
@Tag(name = "Benchmark - 脚本语义相似度", description = "质量脚本的语义相似度计算和推荐")
public class BenchmarkScriptSimilarityController {

    private final BenchmarkScriptSimilarityService similarityService;

    @PostMapping("/generate-embedding")
    @Operation(summary = "为质量脚本生成向量嵌入")
    public RESTResult<Void> generateEmbedding(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long scriptId = request.get("scriptId");
        similarityService.generateEmbedding(scriptId, ownerId);
        return RESTResult.success(null);
    }

    @PostMapping("/batch-generate-embeddings")
    @Operation(summary = "批量生成向量嵌入")
    public RESTResult<Integer> batchGenerateEmbeddings(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        List<Long> scriptIds = request.get("scriptIds");
        Integer count = similarityService.batchGenerateEmbeddings(scriptIds, ownerId);
        return RESTResult.success(count);
    }

    @PostMapping("/index-to-milvus")
    @Operation(summary = "索引向量到 Milvus")
    public RESTResult<Void> indexToMilvus(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long scriptId = request.get("scriptId");
        similarityService.indexToMilvus(scriptId, ownerId);
        return RESTResult.success(null);
    }

    @PostMapping("/batch-index-to-milvus")
    @Operation(summary = "批量索引向量到 Milvus")
    public RESTResult<Integer> batchIndexToMilvus(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        List<Long> scriptIds = request.get("scriptIds");
        Integer count = similarityService.batchIndexToMilvus(scriptIds, ownerId);
        return RESTResult.success(count);
    }

    @PostMapping("/find-similar")
    @Operation(summary = "查找相似脚本")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> findSimilarScripts(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long scriptId = Long.parseLong(request.get("scriptId").toString());
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;
        Double minScore = request.containsKey("minScore") ? Double.parseDouble(request.get("minScore").toString()) : 0.7;

        List<BenchmarkScriptSimilarityVO> result = similarityService.findSimilarScripts(scriptId, ownerId, topK, minScore);
        return RESTResult.success(result);
    }

    @PostMapping("/find-similar-by-text")
    @Operation(summary = "根据文本查找相似脚本")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> findSimilarScriptsByText(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String text = (String) request.get("text");
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;
        Double minScore = request.containsKey("minScore") ? Double.parseDouble(request.get("minScore").toString()) : 0.7;

        List<BenchmarkScriptSimilarityVO> result = similarityService.findSimilarScriptsByText(text, ownerId, topK, minScore);
        return RESTResult.success(result);
    }

    @PostMapping("/calculate-similarity")
    @Operation(summary = "计算两个脚本的相似度")
    public RESTResult<Double> calculateSimilarity(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long scriptId1 = request.get("scriptId1");
        Long scriptId2 = request.get("scriptId2");
        Double similarity = similarityService.calculateSimilarity(scriptId1, scriptId2, ownerId);
        return RESTResult.success(similarity);
    }

    @PostMapping("/get-unembedded-scripts")
    @Operation(summary = "获取未生成向量的脚本列表")
    public RESTResult<List<Long>> getUnembeddedScripts(
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Integer limit = request.getOrDefault("limit", 100);
        List<Long> scriptIds = similarityService.getUnembeddedScriptIds(ownerId, limit);
        return RESTResult.success(scriptIds);
    }

    @PostMapping("/get-unindexed-scripts")
    @Operation(summary = "获取未索引的脚本列表")
    public RESTResult<List<Long>> getUnindexedScripts(
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Integer limit = request.getOrDefault("limit", 100);
        List<Long> scriptIds = similarityService.getUnindexedScriptIds(ownerId, limit);
        return RESTResult.success(scriptIds);
    }
}
