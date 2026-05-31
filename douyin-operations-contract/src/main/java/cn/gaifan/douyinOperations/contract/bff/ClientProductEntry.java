package cn.gaifan.douyinOperations.contract.bff;

import java.util.List;

/**
 * 客户端产品入口摘要。
 *
 * <p>该模型给 Web/App/小程序首页使用，只暴露用户需要看到的产品入口、功能数和可用端。
 * 详细授权、套餐和额度仍由平台授权/计费接口负责。</p>
 */
public record ClientProductEntry(
        // 产品编码，所有商业入口都必须围绕 productCode 建模。
        String productCode,
        // 产品名称。
        String name,
        // 产品阶段，例如首发产品、优先产品、规划产品。
        String stage,
        // 是否启用。
        boolean enabled,
        // 是否可独立售卖。
        boolean independentlySellable,
        // 当前端可展示的功能数量。
        int featureCount,
        // 当前端支持的渠道。
        List<String> channels,
        // 引导用户进入该产品时的默认功能点。
        String primaryFeatureCode
) {
}
