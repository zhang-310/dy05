package cn.gaifan.douyinOperations.module.ai.tool.impl;

import cn.gaifan.douyinOperations.module.ai.tool.LlmRegisteredTool;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import cn.gaifan.douyinOperations.module.product.vo.ProductSearchVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品搜索工具：LLM 按需查询商品库，支持关键词/分类/价格区间过滤
 */
@Component
public class ProductSearchLlmTool implements LlmRegisteredTool {

    public static final String TOOL_NAME = "product_search";

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private ProductService productService;

    public ProductSearchLlmTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "搜索系统中的商品信息，可按关键词、分类、价格区间筛选。用于获取商品名称、价格、库存、卖点等详情，支撑直播话术生成与排品建议。";
    }

    @Override
    public String parametersJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "keyword": { "type": "string", "description": "商品关键词（名称/品牌/功效）" },
                    "category": { "type": "string", "description": "商品分类，如 护肤/彩妆/面膜" },
                    "min_price": { "type": "number", "description": "最低价格（元）" },
                    "max_price": { "type": "number", "description": "最高价格（元）" },
                    "limit": { "type": "integer", "description": "返回数量，默认 10，最大 30" }
                  }
                }
                """;
    }

    @Override
    public String outputJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "products": { "type": "array" },
                    "total": { "type": "integer" },
                    "message": { "type": "string" }
                  }
                }
                """;
    }

    @Override
    public boolean resultCacheable() {
        return true;
    }

    @Override
    public int resultCacheTtlSeconds() {
        return 300;
    }

    @Override
    public String execute(String argumentsJson, LlmToolContext ctx) throws Exception {
        if (productService == null) {
            return objectMapper.writeValueAsString(Map.of("error", "商品服务不可用"));
        }
        if (ctx == null || ctx.userId() == null) {
            return objectMapper.writeValueAsString(Map.of("error", "缺少用户上下文"));
        }

        JsonNode args = objectMapper.readTree(argumentsJson != null && !argumentsJson.isBlank() ? argumentsJson : "{}");
        String keyword = args.path("keyword").asText(null);
        String category = args.path("category").asText(null);
        int limit = Math.min(30, Math.max(1, args.path("limit").asInt(10)));

        ProductSearchVO searchVO = new ProductSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(limit);
        if (keyword != null && !keyword.isBlank()) {
            searchVO.setKeyword(keyword);
        }
        if (category != null && !category.isBlank()) {
            searchVO.setProductCategory(category);
        }
        searchVO.setUserId(ctx.userId());

        var result = productService.search(searchVO);
        if (result == null || result.getList() == null || result.getList().isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("products", List.of(), "total", 0, "message", "未找到匹配商品"));
        }

        List<Map<String, Object>> products = result.getList().stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "name", p.getProductName() != null ? p.getProductName() : "",
                "price", p.getPrice() != null ? p.getPrice() : 0,
                "category", p.getProductCategory() != null ? p.getProductCategory() : "",
                "inventory", p.getInventory() != null ? p.getInventory() : 0,
                "sellingPoints", p.getAiSellingPoints() != null ? truncate(p.getAiSellingPoints(), 300) : ""
        )).collect(Collectors.toList());

        return objectMapper.writeValueAsString(Map.of("products", products, "total", result.getTotal()));
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
