package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.service.LiveScriptComplianceEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 直播话术深度合规增强器实现
 * <p>
 * 覆盖三类抖音电商高危合规风险，数据来源：
 * 1. 抖音《商品发布规范》禁止发布商品类目（40+ 类）
 * 2. 广告法第9条绝对化用语、第17条/18条保健品医疗承诺
 * 3. 消费者权益保护法第8条价格标示规定
 * </p>
 */
@Service
public class LiveScriptComplianceEnhancerImpl implements LiveScriptComplianceEnhancer {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptComplianceEnhancerImpl.class);

    // ─── 品类禁售关键词（抖音《商品发布规范》禁止类目核心词） ─────────────────────────

    /** 绝对禁售类目关键词：枪支弹药、管制刀具、毒品前体、处方药类 */
    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
            // 武器类
            "枪支", "弹药", "手枪", "步枪", "炸药", "爆炸物", "管制刀具", "匕首",
            // 毒品/药品前体
            "麻黄素", "伪麻黄碱", "制毒", "合成毒品",
            // 处方药直播
            "处方药", "抗生素",
            // 活体动物（直播间禁售）
            "活体动物", "活鱼发货", "活虾发货", "活螃蟹发货",
            // 违禁食品/添加剂
            "三聚氰胺", "苏丹红", "塑化剂",
            // 色情/赌博引导
            "赌博", "色情", "裸聊", "援交",
            // 高仿/侵权
            "高仿包", "A货", "精仿", "1:1复刻",
            // 传销/非法金融
            "传销", "无限极", "直销拉人头", "虚拟货币投资"
    );

    /** 限售类目（需要平台资质，直播话术中若出现需提示） */
    private static final Map<String, String> RESTRICTED_CATEGORY_PATTERNS = Map.ofEntries(
            Map.entry("保健品功效承诺", "治疗|治好|根治|康复|消除.*病|治愈|治疗效果"),
            Map.entry("医疗器械疗效承诺", "医疗仪器.*治疗|治疗仪.*效果"),
            Map.entry("医用产品直播", "医用|手术|临床"),
            Map.entry("酒类时段违规", "烈酒|白酒|高度酒")
    );

    // ─── 价格识别正则 ─────────────────────────────────────────────────────────────

    /**
     * 匹配话术中的价格片段：
     * - 数字直接跟元/块/块钱 如 "99元" "59块" "199块钱"
     * - ¥/￥符号跟数字
     * - 中文"一百" "两百" 等不做识别（避免误判）
     */
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?:[¥￥]\\s*|[原售]价\\s*|仅需\\s*|只要\\s*|直播价\\s*|优惠价\\s*|到手价\\s*|秒杀价\\s*)"
                    + "(\\d{1,6}(?:\\.\\d{1,2})?)"
                    + "(?:\\s*元|\\s*块钱?)?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 「全网最低价」「史上最低」等绝对化价格承诺
     */
    private static final List<String> ABSOLUTE_PRICE_CLAIMS = List.of(
            "全网最低价", "全网最低", "史上最低", "全国最低", "历史最低价",
            "最便宜", "最低价格", "价格最低"
    );

    // ─── 售后承诺关键词（消保法/广告法高风险承诺） ────────────────────────────────

    /** 高风险售后承诺模式（需与实际政策核验） */
    private static final List<String[]> AFTERSALE_RISK_PATTERNS = List.of(
            new String[]{"假一赔十", "虚假承诺：假一赔十属于需商家审核核实的承诺，话术须与商品橱窗政策一致"},
            new String[]{"假一赔百", "虚假承诺：假一赔百属于极高风险承诺，若无法兑现将触发消保法处罚"},
            new String[]{"假一赔万", "虚假承诺：假一赔万属于极高风险承诺"},
            new String[]{"无条件退货退款", "高风险承诺：请确认已在商品橱窗设置同等无理由退货政策"},
            new String[]{"永久质保", "高风险承诺：永久质保属于超出合理范围的承诺"},
            new String[]{"终身保修", "高风险承诺：终身保修需有服务协议支撑"},
            new String[]{"保用一辈子", "高风险承诺：\"保用一辈子\"属于无法核实的售后承诺"},
            new String[]{"无理由退", "请确认商品设置了七天无理由退货，话术与政策须一致"},
            new String[]{"效果不好全额退款", "高风险承诺：效果承诺退款需有明确的效果标准"},
            new String[]{"不满意全退", "请确认与橱窗政策一致，\"不满意全退\"若无法执行将被投诉"},
            new String[]{"买到假货赔偿", "高风险承诺：赔偿类承诺须与实际能力匹配"}
    );

    // ─── 品类禁售校验 ─────────────────────────────────────────────────────────────

    @Override
    public CategoryCheckResult checkCategoryRestriction(String scriptContent) {
        if (scriptContent == null || scriptContent.isBlank()) {
            return new CategoryCheckResult(false, false, List.of(), List.of());
        }

        List<String> forbiddenHits = new ArrayList<>();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (scriptContent.contains(keyword)) {
                forbiddenHits.add(keyword);
            }
        }

        List<String> restrictedHits = new ArrayList<>();
        for (Map.Entry<String, String> entry : RESTRICTED_CATEGORY_PATTERNS.entrySet()) {
            try {
                Pattern p = Pattern.compile(entry.getValue());
                if (p.matcher(scriptContent).find()) {
                    restrictedHits.add(entry.getKey());
                }
            } catch (Exception e) {
                log.debug("品类限售正则解析失败 category={}: {}", entry.getKey(), e.getMessage());
            }
        }

        return new CategoryCheckResult(
                !forbiddenHits.isEmpty(),
                !restrictedHits.isEmpty(),
                forbiddenHits,
                restrictedHits
        );
    }

    // ─── 价格一致性检测 ────────────────────────────────────────────────────────────

    @Override
    public PriceCheckResult checkPriceConsistency(String scriptContent, BigDecimal systemPrice, String productName) {
        if (scriptContent == null || scriptContent.isBlank()) {
            return new PriceCheckResult(true, List.of(), systemPrice, null);
        }

        // 先检查绝对化价格表述（不需要对比系统价格）
        for (String claim : ABSOLUTE_PRICE_CLAIMS) {
            if (scriptContent.contains(claim)) {
                return new PriceCheckResult(false, List.of(claim), systemPrice,
                        "话术包含绝对化价格承诺「" + claim + "」，违反广告法第9条，建议改为具体折扣表述");
            }
        }

        // 若无系统价格，跳过数字对比
        if (systemPrice == null || systemPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new PriceCheckResult(true, List.of(), null, null);
        }

        List<String> detectedPrices = new ArrayList<>();
        Matcher matcher = PRICE_PATTERN.matcher(scriptContent);
        while (matcher.find()) {
            detectedPrices.add(matcher.group().trim());
        }

        if (detectedPrices.isEmpty()) {
            return new PriceCheckResult(true, List.of(), systemPrice, null);
        }

        // 检查话术中最低报价是否低于系统价格的合理范围（允许 ±20% 误差，话术可能说"约XX元"）
        BigDecimal tolerance = systemPrice.multiply(BigDecimal.valueOf(0.20));
        BigDecimal lowerBound = systemPrice.subtract(tolerance).setScale(0, RoundingMode.FLOOR);

        String inconsistentHint = null;
        List<String> inconsistentPrices = new ArrayList<>();
        for (String priceStr : detectedPrices) {
            try {
                // 提取纯数字部分
                String numStr = priceStr.replaceAll("[^0-9.]", "");
                if (numStr.isEmpty()) continue;
                BigDecimal detected = new BigDecimal(numStr);
                // 如果话术中报价远低于系统价格（超过50%折扣），可能是异常
                if (detected.compareTo(lowerBound) < 0) {
                    inconsistentPrices.add(priceStr + "(系统价:" + systemPrice.toPlainString() + "元)");
                }
            } catch (NumberFormatException ignored) {
                // 无法解析的价格片段跳过
            }
        }

        if (!inconsistentPrices.isEmpty()) {
            String name = productName != null ? productName : "当前商品";
            inconsistentHint = "话术报价" + inconsistentPrices + "与系统登记价格" + systemPrice.toPlainString()
                    + "元差异超过20%，请确认「" + name + "」价格是否已更新，避免承诺无法兑现";
        }

        return new PriceCheckResult(
                inconsistentHint == null,
                detectedPrices,
                systemPrice,
                inconsistentHint
        );
    }

    // ─── 售后承诺校验 ─────────────────────────────────────────────────────────────

    @Override
    public AftersaleCheckResult checkAftersaleCommitment(String scriptContent) {
        if (scriptContent == null || scriptContent.isBlank()) {
            return new AftersaleCheckResult(false, List.of(), null);
        }

        List<String> hits = new ArrayList<>();
        List<String> hints = new ArrayList<>();
        for (String[] pattern : AFTERSALE_RISK_PATTERNS) {
            String keyword = pattern[0];
            String hint = pattern[1];
            if (scriptContent.contains(keyword)) {
                hits.add(keyword);
                hints.add(hint);
            }
        }

        if (hits.isEmpty()) {
            return new AftersaleCheckResult(false, List.of(), null);
        }

        String combinedHint = String.join("；", hints.subList(0, Math.min(hints.size(), 3)));
        return new AftersaleCheckResult(true, hits, combinedHint);
    }
}
