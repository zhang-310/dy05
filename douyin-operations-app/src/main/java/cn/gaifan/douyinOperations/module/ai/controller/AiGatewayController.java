package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.ai.AiCallRequest;
import cn.gaifan.douyinOperations.contract.ai.AiCallResponse;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.module.ai.gateway.AiGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 网关控制器 — 统一 AI 调用入口 + MCP 工具管理
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI 网关 / AI Gateway", description = "AI 调用、模型管理、用量查询")
public class AiGatewayController {

    @Resource
    private AiGatewayService aiGateway;

    @PostMapping("/call")
    @Operation(summary = "调用 AI 模型")
    public ResponseEntity<RESTResult<AiCallResponse>> call(@RequestBody AiCallRequest body) {
        AiCallResponse result = aiGateway.callContract(body);
        HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.PAYMENT_REQUIRED;
        RESTResult<AiCallResponse> r = RESTResult.getSuccess(result);
        r.setTraceId(result.traceId() != null ? result.traceId() : MDC.get("traceId"));
        return ResponseEntity.status(status).body(r);
    }

    @PostMapping("/usage")
    @Operation(summary = "AI 用量统计")
    public RESTResult<Map<String, Object>> usage() {
        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = ctx != null && ctx.tenantId() != null ? ctx.tenantId() : "demo-tenant";
        Map<String, Long> byFeature = aiGateway.getUsageStats(tenantId);
        Map<String, Object> data = new HashMap<>();
        data.put("tenantId", tenantId);
        data.put("chatCount", byFeature.getOrDefault("ai.chat", 0L));
        data.put("generationCount", byFeature.getOrDefault("ai.generation", 0L));
        data.put("mcpToolCount", byFeature.getOrDefault("ai.mcp-tool", 0L));
        data.put("byFeature", byFeature);
        data.put("totalInvocations", aiGateway.totalInvocations(tenantId));
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/tools")
    @Operation(summary = "AI 工具列表")
    public RESTResult<Map<String, Object>> tools() {
        Map<String, Object> tools = Map.of(
                "tools", java.util.List.of(
                        Map.of("name", "video.analyze", "desc", "短视频拆解分析", "productCode", "video-insight"),
                        Map.of("name", "video.search_hot", "desc", "爆款视频搜索", "productCode", "video-insight"),
                        Map.of("name", "script.generate", "desc", "AI 脚本生成", "productCode", "douyin-ops"),
                        Map.of("name", "knowledge.rag", "desc", "知识库 RAG 查询", "productCode", "knowledge-base"),
                        Map.of("name", "knowledge.document", "desc", "文档智能处理", "productCode", "knowledge-base"),
                        Map.of("name", "digital-human.generate", "desc", "数字人生成", "productCode", "digital-human")
                )
        );
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(tools);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
