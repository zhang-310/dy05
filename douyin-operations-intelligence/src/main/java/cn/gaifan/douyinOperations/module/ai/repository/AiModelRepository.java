package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModel, Long>, JpaSpecificationExecutor<AiModel> {
    Optional<AiModel> findByIdAndDeleted(Long id, Integer deleted);
    List<AiModel> findByStatusAndDeleted(Integer status, Integer deleted);

    @Modifying
    @Query("UPDATE AiModel m SET m.quotaUsed = m.quotaUsed + :tokens WHERE m.id = :id")
    void incrementQuotaUsed(@Param("id") Long id, @Param("tokens") Long tokens);
}
