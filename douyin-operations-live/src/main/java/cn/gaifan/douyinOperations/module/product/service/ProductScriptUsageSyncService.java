package cn.gaifan.douyinOperations.module.product.service;

/**
 * 直播排品绑定商品库话术时，写入 dy_product_script_usage（效果闭环 / P3）
 */
public interface ProductScriptUsageSyncService {

    /**
     * 排品保存时若绑定了 product_script_id，记录一次「引用/应用」
     *
     * @param liveProductId 可为 null（兼容旧调用）；非 null 时与 productScriptId 组合幂等
     */
    void recordLiveProductScriptBinding(Long liveProductId, Long sessionId, Long productScriptId, Long userId);
}
