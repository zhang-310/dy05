package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthRoleResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 角色-资源关联表 Repository
 */
public interface AuthRoleResourceRepository extends JpaRepository<AuthRoleResource, Long> {

    List<AuthRoleResource> findByRoleId(Long roleId);

    @Modifying
    @Query("DELETE FROM AuthRoleResource r WHERE r.roleId = :roleId")
    void deleteByRoleId(Long roleId);
}
