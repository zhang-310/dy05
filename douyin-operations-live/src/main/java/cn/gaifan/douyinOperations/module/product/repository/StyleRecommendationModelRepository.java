package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.StyleRecommendationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 风格推荐模型 Repository
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Repository
public interface StyleRecommendationModelRepository extends JpaRepository<StyleRecommendationModel, Long>,
        JpaSpecificationExecutor<StyleRecommendationModel> {

    /**
     * 查找用户的激活模型
     */
    Optional<StyleRecommendationModel> findByUserIdAndIsActiveTrueAndDeleted(Long userId, Integer deleted);

    /**
     * 查找指定类型和版本的模型
     */
    Optional<StyleRecommendationModel> findByUserIdAndModelTypeAndModelVersionAndDeleted(
            Long userId, String modelType, String modelVersion, Integer deleted);
}
