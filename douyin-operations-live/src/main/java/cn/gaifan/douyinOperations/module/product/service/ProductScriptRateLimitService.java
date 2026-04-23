package cn.gaifan.douyinOperations.module.product.service;

/**
 * 产品话术生成频率限制：每用户每分钟最多 5 次
 */
public interface ProductScriptRateLimitService {

    /**
     * 尝试获取许可，超限时抛出 BusinessException(PRODUCT_SCRIPT_RATE_LIMITED)
     */
    void tryAcquire(Long userId);
}
