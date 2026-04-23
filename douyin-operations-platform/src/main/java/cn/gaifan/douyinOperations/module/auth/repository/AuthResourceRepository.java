package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 系统资源表 Repository
 */
public interface AuthResourceRepository extends JpaRepository<AuthResource, Long>, JpaSpecificationExecutor<AuthResource> {

    List<AuthResource> findByResourceTypeAndDeletedOrderBySortOrderAsc(String resourceType, Integer deleted);

    List<AuthResource> findByDeletedOrderBySortOrderAsc(Integer deleted);

    Page<AuthResource> findByDeleted(Integer deleted, Pageable pageable);
}
