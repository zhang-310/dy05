package cn.gaifan.douyinOperations.module.copy.repository;

import cn.gaifan.douyinOperations.module.copy.entity.CopyApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CopyApprovalRepository extends JpaRepository<CopyApproval, Long>, JpaSpecificationExecutor<CopyApproval> {

    Optional<CopyApproval> findByIdAndDeleted(Long id, Integer deleted);

    Page<CopyApproval> findByCopyIdAndDeleted(Long copyId, Integer deleted, Pageable pageable);

    Optional<CopyApproval> findTopByCopyIdAndDeletedOrderByCreateTimeDesc(Long copyId, Integer deleted);
}
