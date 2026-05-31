package cn.gaifan.douyinOperations.contract.openapi;

import java.util.List;

/**
 * OpenAPI/MCP SDK 示例片段。
 *
 * <p>示例片段只包含可复制的接入骨架和注意事项，不包含真实 API Key、Secret、Agent Token、
 * 用户素材或可重放签名。所有语言示例必须保持同一签名字段顺序和同一 MCP 端点。</p>
 */
public record OpenApiSdkExampleSnippet(
        String language,
        String scenario,
        String fileName,
        String entryCommand,
        String code,
        List<String> notes
) {
}
