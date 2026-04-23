package cn.gaifan.douyinOperations.module.product.service;

import java.util.Map;

/**
 * 从商品链接提取信息：抓取页面 meta、调用 AI 提炼卖点
 */
public interface ProductLinkExtractService {

    /**
     * 从商品链接提取：标题、图片、描述，并调用 AI 提炼卖点
     *
     * @param productLink 商品链接（抖音/淘宝等）
     * @return 提取结果：productName, imageUrl, description, aiSellingPoints
     */
    Map<String, String> extractFromLink(String productLink);

    /**
     * 异步从商品链接提取关键信息并保存到商品字段（不覆盖用户已填内容）
     *
     * @param productId 商品 ID
     */
    void extractAndSaveAsync(Long productId);
}
