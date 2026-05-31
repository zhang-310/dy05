package cn.gaifan.douyinOperations.contract.openapi;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI/MCP 开发者快速接入说明。
 *
 * <p>该合同把公网域名、签名规则、示例请求和治理清单固化成后端可测试的数据，
 * 前端和 docs.gaifan.cn 只负责展示，不能各自手写一套签名规则。</p>
 */
public record OpenApiMcpQuickstart(
        // 当前租户；控制台和 smoke 脚本默认使用 demo-tenant。
        String tenantId,
        // 开发者文档域名。
        String docsDomain,
        // OpenAPI 公网基准地址。
        String openApiBaseUrl,
        // MCP 公网基准地址。
        String mcpBaseUrl,
        // OpenAPI 签名调用路径。
        String invokePath,
        // 签名算法。
        String signatureAlgorithm,
        // 时间戳有效窗口说明。
        String timestampWindow,
        // 必传签名请求头，全部使用占位值。
        Map<String, String> requiredHeaders,
        // 稳定签名步骤，按顺序执行。
        List<String> signingSteps,
        // 签名原文模板，字段顺序必须和服务端验签一致。
        String canonicalStringTemplate,
        // 面向开发者的可复制示例。
        List<OpenApiMcpExample> examples,
        // 商业治理清单：授权、支付、积分、用量、审计、成本。
        List<String> governanceChecklist,
        // 合规边界：不允许 Cookie 绕过、失控爬取或未授权采集。
        List<String> complianceBoundaries,
        // 开发者从购买开通到首次调用的执行步骤。
        List<String> productAccessSteps,
        // MCP 客户端和 OpenAPI 同源能力的接入步骤。
        List<String> mcpClientSteps,
        // 成功或失败调用后必须核对的账本和证据。
        List<String> ledgerCheckpoints,
        // 常见拒绝码和排障路径。
        List<String> troubleshooting,
        // 公网上线前必须完成的生产检查项。
        List<String> productionChecklist
) {
}
