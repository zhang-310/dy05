package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiTaskModelConfigRepository extends JpaRepository<AiTaskModelConfig, Long> {

    Optional<AiTaskModelConfig> findByTaskCodeAndStatusAndDeleted(String taskCode, Integer status, Integer deleted);
}
