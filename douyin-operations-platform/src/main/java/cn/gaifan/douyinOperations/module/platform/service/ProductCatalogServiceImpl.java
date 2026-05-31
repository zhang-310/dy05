package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.product.ProductCatalogProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 产品目录服务 — DB 驱动 (V184+)
 *
 * 数据来源：DB (sys_product/sys_feature) + 内存 fallback
 */
@Service
public class ProductCatalogServiceImpl implements ProductCatalogProvider {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private final Map<String, ProductSummary> products = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            var rows = jdbcTemplate.queryForList(
                    """
                            SELECT p.product_code as code, p.product_name as name, 'launched' as stage, p.enabled,
                                   f.feature_code as fcode, f.feature_name as fname, f.quota_unit, f.monthly_limit
                            FROM gf_product p
                            LEFT JOIN gf_feature f ON p.product_code = f.product_code
                            WHERE p.enabled = true AND (f.enabled = true OR f.feature_code IS NULL)
                            """);
            if (rows.isEmpty()) {
                rows = jdbcTemplate.queryForList(
                        "SELECT p.code, p.name, p.stage, p.enabled, " +
                        "f.code as fcode, f.name as fname, f.quota_unit, f.monthly_limit " +
                        "FROM sys_product p LEFT JOIN sys_feature f ON p.code = f.product_code " +
                        "WHERE p.enabled = true AND (f.enabled = true OR f.code IS NULL)");
            }
            Map<String, ProductSummary> map = new java.util.LinkedHashMap<>();
            for (var row : rows) {
                String code = (String) row.get("code");
                map.computeIfAbsent(code, k -> new ProductSummary(
                        k, (String) row.get("name"), (String) row.get("stage"),
                        (Boolean) row.get("enabled"), new ArrayList<>()));
                String fcode = (String) row.get("fcode");
                if (fcode != null) {
                    map.get(code).features().add(new FeatureSummary(
                            fcode, (String) row.get("fname"),
                            (String) row.get("quota_unit"),
                            ((Number) row.get("monthly_limit")).longValue()));
                }
            }
            List<ProductSummary> dbProducts = new ArrayList<>(map.values());
            if (!dbProducts.isEmpty()) {
                dbProducts.forEach(p -> products.put(p.code(), p));
                return;
            }
        } catch (Exception e) {
            // Fallback to in-memory
        }
        buildDefaultCatalog().forEach(p -> products.put(p.code(), p));
    }

    @Override
    public List<ProductSummary> listProducts() {
        return new ArrayList<>(products.values());
    }

    @Override
    public ProductSummary findProduct(String productCode) {
        return products.get(productCode);
    }

    @Override
    public FeatureSummary findFeature(String featureCode) {
        return products.values().stream()
                .flatMap(p -> p.features().stream())
                .filter(f -> f.code().equals(featureCode))
                .findFirst()
                .orElse(null);
    }

    private static List<ProductSummary> buildDefaultCatalog() {
        return List.of(
                new ProductSummary("douyin-ops", "抖音运营", "launched", true, List.of(
                        new FeatureSummary("douyin-ops.account-mgmt", "账号管理", "account", 100),
                        new FeatureSummary("douyin-ops.video-analysis", "视频分析", "analysis", 1000),
                        new FeatureSummary("douyin-ops.content-planning", "内容策划", "plan", 500),
                        new FeatureSummary("douyin-ops.live-script", "直播话术", "script", 200)
                )),
                new ProductSummary("video-insight", "短视频拆解分析", "launched", true, List.of(
                        new FeatureSummary("video-insight.breakdown", "视频拆解", "analysis", 500),
                        new FeatureSummary("video-insight.viral-analysis", "爆款分析", "analysis", 200)
                )),
                new ProductSummary("knowledge-base", "AI 知识库", "launched", true, List.of(
                        new FeatureSummary("knowledge-base.rag", "RAG 查询", "query", 5000),
                        new FeatureSummary("knowledge-base.document", "文档管理", "doc", 200)
                )),
                new ProductSummary("digital-human", "AI 数字人", "preview", true, List.of(
                        new FeatureSummary("digital-human.generate", "数字人生成", "video", 50)
                )),
                new ProductSummary("drama-ai", "短剧 AI 制作", "preview", true, List.of(
                        new FeatureSummary("drama-ai.script", "剧本生成", "script", 100),
                        new FeatureSummary("drama-ai.storyboard", "分镜设计", "storyboard", 100)
                )),
                new ProductSummary("photo-avatar-video", "照片转视频", "preview", true, List.of(
                        new FeatureSummary("photo-avatar-video.generate", "AI视频生成", "video", 50)
                )),
                new ProductSummary("shortvideo-maker", "短视频成片创作", "preview", true, List.of(
                        new FeatureSummary("shortvideo-maker.script", "脚本创作", "script", 200),
                        new FeatureSummary("shortvideo-maker.render", "渲染合成", "video", 50)
                ))
        );
    }
}
