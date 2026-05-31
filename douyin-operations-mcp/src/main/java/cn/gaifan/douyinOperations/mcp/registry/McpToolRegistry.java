package cn.gaifan.douyinOperations.mcp.registry;

import cn.gaifan.douyinOperations.contract.mcp.McpGatewayOverview;
import cn.gaifan.douyinOperations.contract.mcp.McpToolDescriptor;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class McpToolRegistry {
    public List<McpToolDescriptor> listTools() {
        return List.of(
                tool("video.search_hot", "Search hot videos", "video.mcp.invoke", "tenant-minute:60"),
                tool("video.analyze", "Analyze one short video", "video.analyze.standard", "tenant-minute:30"),
                tool("video.report_export", "Export video insight report", "video.mcp.invoke", "tenant-minute:30"),
                tool("video.batch_report_export", "Export video insight batch report", "video.mcp.invoke", "tenant-minute:20"),
                tool("creator.analyze", "Analyze creator patterns", "video.mcp.invoke", "tenant-minute:10"),
                tool("video.find_similar", "Find similar videos", "video.find_similar", "tenant-minute:30"),
                tool("video.extract_script_pattern", "Extract script pattern", "video.analyze.standard", "tenant-minute:30"),
                tool("topic.recommend", "Recommend creator topics", "video.mcp.invoke", "tenant-minute:30"),
                tool("comments.analyze", "Analyze comments and user pain points", "video.analyze.deep", "tenant-minute:15"),
                tool("brief.generate_imitation", "Generate compliant imitation brief", "video.analyze.standard", "tenant-minute:30"),
                tool("shortvideo.project_create", "Create short-video maker project brief",
                        ProductCode.SHORTVIDEO_MAKER, "shortvideo.script.generate", "tenant-minute:20"),
                tool("drama.storyboard_generate", "Generate drama storyboard brief",
                        ProductCode.DRAMA_AI, "drama.storyboard.generate", "tenant-minute:10"),
                tool("digital-human.script_generate", "Generate digital human script",
                        ProductCode.DIGITAL_HUMAN, "digital-human.script.generate", "tenant-minute:20"),
                tool("knowledge.document_evidence", "List tenant knowledge document source evidence",
                        ProductCode.KNOWLEDGE_BASE, "knowledge.document.evidence", "tenant-minute:60"),
                tool("knowledge.source_evidence", "List tenant knowledge source governance evidence",
                        ProductCode.KNOWLEDGE_BASE, "knowledge.source.evidence", "tenant-minute:60"),
                tool("knowledge.rag_evidence_pack", "Create tenant knowledge RAG evidence pack",
                        ProductCode.KNOWLEDGE_BASE, "knowledge.rag.evidence_pack", "tenant-minute:30"),
                tool("knowledge.rag_search", "Search tenant knowledge base with RAG citations",
                        ProductCode.KNOWLEDGE_BASE, "knowledge.search.rag", "tenant-minute:60")
        );
    }

    public Optional<McpToolDescriptor> findTool(String toolCode) {
        return listTools().stream()
                .filter(tool -> tool.toolCode().equals(toolCode))
                .findFirst();
    }

    public McpGatewayOverview overview() {
        List<McpToolDescriptor> tools = listTools();
        return new McpGatewayOverview(
                tools,
                tools.size(),
                (int) tools.stream().filter(McpToolDescriptor::auditRequired).count(),
                tools.size(),
                "MCP",
                "authorization + ai-gateway + usage-cost + audit"
        );
    }

    private McpToolDescriptor tool(String code, String name, String featureCode, String rateLimitHint) {
        return new McpToolDescriptor(code, name, ProductCode.VIDEO_INSIGHT, featureCode, rateLimitHint, true);
    }

    private McpToolDescriptor tool(String code, String name, String productCode, String featureCode, String rateLimitHint) {
        return new McpToolDescriptor(code, name, productCode, featureCode, rateLimitHint, true);
    }
}
