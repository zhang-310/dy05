package cn.gaifan.douyinOperations.module.copy.repository;

import cn.gaifan.douyinOperations.module.copy.entity.CopyTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CopyTemplateRepository extends JpaRepository<CopyTemplate, Long>, JpaSpecificationExecutor<CopyTemplate> {

    Optional<CopyTemplate> findByIdAndDeleted(Long id, Integer deleted);

    Page<CopyTemplate> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    @Modifying
    @Query("UPDATE CopyTemplate t SET t.status = :status WHERE t.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
