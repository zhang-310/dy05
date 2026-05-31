package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiGenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiGenerationTaskRepository extends JpaRepository<AiGenerationTask, Long>, JpaSpecificationExecutor<AiGenerationTask> {
    Optional<AiGenerationTask> findByIdAndDeleted(Long id, Integer deleted);

    @Modifying
    @Query("UPDATE AiGenerationTask t SET t.taskStatus = :status, t.outputContent = :output, t.tokensUsed = :tokens WHERE t.id = :id")
    void updateResult(@Param("id") Long id, @Param("status") Integer status,
                      @Param("output") String output, @Param("tokens") Long tokens);
}
