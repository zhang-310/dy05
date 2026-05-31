package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiModelPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiModelPricingRepository extends JpaRepository<AiModelPricing, Long> {

    Optional<AiModelPricing> findByModelNameAndDeleted(String modelName, int deleted);
}
