package cn.gaifan.douyinOperations.module.product.service;

/**
 * 组合生成进度（含话术类型、风格、场景，便于前端展示）
 */
@FunctionalInterface
public interface ComboProgressCallback {

    void onProgress(int current, int total, Long productId, String productName,
                    String scriptType, String style, String scene, boolean success, String message);
}
