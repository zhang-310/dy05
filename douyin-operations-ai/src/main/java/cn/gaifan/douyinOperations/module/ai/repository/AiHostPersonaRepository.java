package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiHostPersonaRepository extends JpaRepository<AiHostPersona, Long> {

    Optional<AiHostPersona> findByHostCodeAndDeleted(String hostCode, int deleted);

    List<AiHostPersona> findByStatusAndDeletedOrderBySortOrderAsc(int status, int deleted);
}
