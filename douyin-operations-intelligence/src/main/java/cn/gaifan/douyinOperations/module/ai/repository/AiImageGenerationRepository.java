package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiImageGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiImageGenerationRepository extends JpaRepository<AiImageGeneration, Long> {
    Page<AiImageGeneration> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted, Pageable pageable);
}
