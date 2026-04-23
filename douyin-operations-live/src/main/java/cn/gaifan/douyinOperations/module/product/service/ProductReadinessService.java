package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.vo.ProductReadinessVO;

/**
 * 商品上播准备度检测服务
 */
public interface ProductReadinessService {

    /**
     * 检测商品上播准备度
     * @param productId 商品ID
     * @param userId 用户ID
     * @return 准备度检测结果
     */
    ProductReadinessVO checkReadiness(Long productId, Long userId);
}
