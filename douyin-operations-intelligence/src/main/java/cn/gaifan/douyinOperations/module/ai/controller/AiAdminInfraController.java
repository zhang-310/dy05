package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiAdminInfraService.InfraHealthItem;
import cn.gaifan.douyinOperations.module.ai.service.AiAdminInfraService;
import cn.gaifan.douyinOperations.module.ai.vo.InfraSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 运维管理：PostgreSQL 文档、ES 文档、Milvus 向量、索引队列
 * 按知识库搜索分页，仅管理员
 */
@Tag(name = "AI 运维管理")
@RestController
@RequestMapping("/api/v1/ai/admin/infra")
public class AiAdminInfraController {

    @Resource
    private AiAdminInfraService aiAdminInfraService;

    @Value("${app.ai.grafana-url:}")
    private String grafanaUrl;

    @Operation(summary = "监控配置（Grafana 嵌入地址等）")
    @PostMapping("/monitoring-config")
    public RESTResult<Map<String, Object>> getMonitoringConfig(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        Map<String, Object> m = new HashMap<>();
        m.put("grafanaUrl", grafanaUrl != null && !grafanaUrl.isBlank() ? grafanaUrl : null);
        return RESTResult.getSuccess(m);
    }

    @Operation(summary = "基础设施健康检查（Milvus、ES、Redis）")
    @PostMapping("/health")
    public RESTResult<List<InfraHealthItem>> health(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(aiAdminInfraService.checkHealth());
    }

    @Operation(summary = "基础设施详情（PostgreSQL、Milvus、ES、Redis 版本/统计/建议）")
    @PostMapping("/detail")
    public RESTResult<Map<String, Object>> getInfraDetail(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(aiAdminInfraService.getInfraDetail());
    }

    @Operation(summary = "PostgreSQL 文档分页（按知识库、关键词）")
    @PostMapping("/documents/pg")
    public RESTResult<PageResultVO<Map<String, Object>>> pagePgDocuments(
            @RequestBody InfraSearchVO vo,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        int page = vo.getPage() != null ? vo.getPage() : 0;
        int rows = vo.getRows() != null ? Math.min(vo.getRows(), 100) : 30;
        PageResultVO<Map<String, Object>> result = aiAdminInfraService.pagePgDocuments(
                vo.getKbId(), vo.getKeyword(), page, rows);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "Elasticsearch 文档分页（按知识库、关键词）")
    @PostMapping("/documents/es")
    public RESTResult<PageResultVO<Map<String, Object>>> pageEsDocuments(
            @RequestBody InfraSearchVO vo,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        int page = vo.getPage() != null ? vo.getPage() : 0;
        int rows = vo.getRows() != null ? Math.min(vo.getRows(), 100) : 30;
        PageResultVO<Map<String, Object>> result = aiAdminInfraService.pageEsDocuments(
                vo.getKbId(), vo.getKeyword(), page, rows);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "Milvus 向量统计（按知识库）")
    @PostMapping("/milvus/stats")
    public RESTResult<Map<String, Object>> getMilvusStats(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        Long kbId = body != null && body.get("kbId") instanceof Number n ? n.longValue() : null;
        Map<String, Object> result = aiAdminInfraService.getMilvusStats(kbId);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "P2 缓存命中率统计")
    @PostMapping("/cache/stats")
    public RESTResult<Map<String, Object>> getCacheStats(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(aiAdminInfraService.getCacheStats());
    }

    @Operation(summary = "P2 检索性能统计（P50/P99/QPS）")
    @PostMapping("/search/stats")
    public RESTResult<Map<String, Object>> getSearchStats(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(aiAdminInfraService.getSearchStats());
    }

    @Operation(summary = "索引队列表分页（按知识库、状态、关键词）")
    @PostMapping("/queue")
    public RESTResult<PageResultVO<Map<String, Object>>> pageIndexQueue(
            @RequestBody InfraSearchVO vo,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        int page = vo.getPage() != null ? vo.getPage() : 0;
        int rows = vo.getRows() != null ? Math.min(vo.getRows(), 100) : 30;
        PageResultVO<Map<String, Object>> result = aiAdminInfraService.pageIndexQueue(
                vo.getKbId(), vo.getStatus(), vo.getKeyword(), page, rows);
        return RESTResult.getSuccess(result);
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
