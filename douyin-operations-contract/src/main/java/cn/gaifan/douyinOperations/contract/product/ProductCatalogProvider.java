package cn.gaifan.douyinOperations.contract.product;

/**
 * 产品目录服务 SPI
 *
 * 返回当前平台的完整产品列表和功能定义。
 * 实现类在 platform 模块中。
 */
public interface ProductCatalogProvider {

    /** 所有可售产品 */
    java.util.List<ProductSummary> listProducts();

    /** 根据编码查找产品 */
    ProductSummary findProduct(String productCode);

    /** 根据编码查找功能 */
    FeatureSummary findFeature(String featureCode);

    record ProductSummary(String code, String name, String stage, boolean enabled,
                          java.util.List<FeatureSummary> features) {}

    record FeatureSummary(String code, String name, String quotaUnit, long monthlyLimit) {}
}
