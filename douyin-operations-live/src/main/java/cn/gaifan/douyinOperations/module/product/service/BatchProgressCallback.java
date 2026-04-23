package cn.gaifan.douyinOperations.module.product.service;

/**
 * 批量生成进度回调
 */
@FunctionalInterface
public interface BatchProgressCallback {
    void onProgress(int current, int total, Long productId, String productName, String style, boolean success, String message);
}
