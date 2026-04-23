package cn.gaifan.douyinOperations.module.live.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 直播话术深度合规增强器
 * <p>
 * 补充基础违禁词检测未覆盖的三类高危合规风险：
 * <ol>
 *   <li>品类级禁售校验 —— 判断话术内容是否触及平台禁售/限售品类关键词</li>
 *   <li>价格一致性检测 —— 话术中出现的价格数字与商品系统价格交叉校验</li>
 *   <li>售后承诺校验 —— 检测话术中是否包含平台无法兑现的售后/退换货承诺</li>
 * </ol>
 * 对应抖音电商《商品发布规范》《直播内容规范》中的强制性条款。
 */
public interface LiveScriptComplianceEnhancer {

    /**
     * 品类禁售与限售校验结果
     */
    record CategoryCheckResult(
            /** 是否触发禁售 */
            boolean forbidden,
            /** 是否触发限售（需资质） */
            boolean restricted,
            /** 命中的禁售关键词列表 */
            List<String> forbiddenKeywords,
            /** 命中的限售类目描述列表 */
            List<String> restrictedCategories
    ) {}

    /**
     * 价格一致性检测结果
     */
    record PriceCheckResult(
            /** 是否通过（未发现不一致） */
            boolean passed,
            /** 话术中识别到的价格片段 */
            List<String> detectedPrices,
            /** 系统商品价格（元） */
            BigDecimal systemPrice,
            /** 检测到的不一致提示 */
            String hint
    ) {}

    /**
     * 售后承诺校验结果
     */
    record AftersaleCheckResult(
            /** 是否发现高风险售后承诺 */
            boolean hasRiskyCommitment,
            /** 命中的承诺片段 */
            List<String> commitmentMatches,
            /** 风险说明 */
            String hint
    ) {}

    /**
     * 品类禁售与限售校验
     *
     * @param scriptContent 话术正文
     * @return 校验结果
     */
    CategoryCheckResult checkCategoryRestriction(String scriptContent);

    /**
     * 价格一致性检测
     *
     * @param scriptContent 话术正文
     * @param systemPrice   商品在系统中登记的价格（null 表示未关联商品，跳过检测）
     * @param productName   商品名称（用于日志）
     * @return 校验结果
     */
    PriceCheckResult checkPriceConsistency(String scriptContent, BigDecimal systemPrice, String productName);

    /**
     * 售后承诺高风险检测
     *
     * @param scriptContent 话术正文
     * @return 校验结果
     */
    AftersaleCheckResult checkAftersaleCommitment(String scriptContent);
}
