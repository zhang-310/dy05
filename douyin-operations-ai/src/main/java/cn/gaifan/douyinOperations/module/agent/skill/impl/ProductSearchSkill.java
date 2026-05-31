package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.intelligence.bridge.LiveProductRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 商品搜索技能 - 从 live_product 表查询真实商品数据
 */
@Component
public class ProductSearchSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchSkill.class);

    @Resource(name = "intelligenceLiveProductRepository")
    private LiveProductRepository liveProductRepository;

    @Override
    public String getName() {
        return "product_search";
    }

    @Override
    public String getDescription() {
        return "搜索商品信息，支持按名称、类目、价格范围查询";
    }

    @Override
    public boolean matches(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("商品") || lower.contains("产品") || lower.contains("货品")
                || lower.contains("价格") || lower.contains("库存");
    }

    @Override
    public String execute(SkillContext ctx) {
        try {
            // 从 params 中获取 keyword，fallback 到 rawInput
            String keyword = (String) ctx.params().getOrDefault("keyword", ctx.rawInput());
            log.info("[ProductSearch] 执行搜索: keyword={}, userId={}", keyword, ctx.userId());

            // 查询最多 10 条
            List<LiveProduct> products = liveProductRepository
                    .searchProducts(keyword != null ? keyword : "", PageRequest.of(0, 10))
                    .getContent();

            if (products == null || products.isEmpty()) {
                return "未找到匹配的商品。请尝试调整搜索关键词，或者先在直播场次中添加商品。\n" +
                       "💡 提示：商品来源为直播场次关联产品，可通过「直播场次 - 商品管理」添加。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(products.size()).append(" 个商品：\n\n");

            for (int i = 0; i < products.size(); i++) {
                LiveProduct p = products.get(i);
                sb.append(i + 1).append(". ").append(p.getProductName()).append("\n");
                sb.append("   销售额: ¥").append(p.getRevenue() != null ? p.getRevenue() : "0.00").append("\n");
                sb.append("   销量: ").append(p.getSaleQuantity() != null ? p.getSaleQuantity() : 0).append(" 件\n");
                if (p.getProductType() != null) {
                    sb.append("   类型: ").append(p.getProductType()).append("\n");
                }
                if (p.getPosition() != null) {
                    sb.append("   展示位: ").append(p.getPosition()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("💡 共 ").append(products.size()).append(" 条结果，可通过「直播场次 - 商品管理」维护商品数据。\n");
            return sb.toString();

        } catch (Exception e) {
            log.error("[ProductSearch] 执行失败", e);
            return "商品搜索失败: " + e.getMessage();
        }
    }
}