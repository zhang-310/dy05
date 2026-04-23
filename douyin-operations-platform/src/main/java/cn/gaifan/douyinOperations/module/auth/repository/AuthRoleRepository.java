package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 角色表 Repository
 */
public interface AuthRoleRepository extends JpaRepository<AuthRole, Long>, JpaSpecificationExecutor<AuthRole> {

    Optional<AuthRole> findByRoleCodeAndDeleted(String roleCode, Integer deleted);

    List<AuthRole> findByDeletedOrderBySortOrderAsc(Integer deleted);

    Page<AuthRole> findByDeleted(Integer deleted, Pageable pageable);
}
