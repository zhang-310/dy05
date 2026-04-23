package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ProductFeatureCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商品特征缓存 Repository
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Repository
public interface ProductFeatureCacheRepository extends JpaRepository<ProductFeatureCache, Long>,
        JpaSpecificationExecutor<ProductFeatureCache> {

    /**
     * 根据商品ID查找特征缓存
     */
    Optional<ProductFeatureCache> findByProductIdAndDeleted(Long productId, Integer deleted);

    /**
     * 查找用户的所有特征缓存
     */
    List<ProductFeatureCache> findByUserIdAndDeleted(Long userId, Integer deleted);

    /**
     * 查找指定特征版本的缓存
     */
    List<ProductFeatureCache> findByUserIdAndFeatureVersionAndDeleted(Long userId, String featureVersion, Integer deleted);
}
