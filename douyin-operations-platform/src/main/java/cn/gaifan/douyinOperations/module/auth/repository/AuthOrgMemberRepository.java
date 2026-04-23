package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthOrgMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuthOrgMemberRepository extends JpaRepository<AuthOrgMember, Long> {

    List<AuthOrgMember> findByOrgIdAndDeletedAndStatus(Long orgId, Integer deleted, Integer status);

    List<AuthOrgMember> findByOrgIdAndDeleted(Long orgId, Integer deleted);

    Optional<AuthOrgMember> findByOrgIdAndUserIdAndDeleted(Long orgId, Long userId, Integer deleted);

    List<AuthOrgMember> findByUserIdAndDeletedAndStatus(Long userId, Integer deleted, Integer status);

    /**
     * 查询机构 owner 旗下所有已加入成员的 user_id（含 owner 自己）
     */
    @Query("SELECT m.userId FROM AuthOrgMember m WHERE m.orgId IN " +
           "(SELECT o.id FROM AuthOrganization o WHERE o.ownerId = :ownerId AND o.deleted = 0) " +
           "AND m.status = 1 AND m.deleted = 0")
    List<Long> findMemberUserIdsByOrgOwnerId(@Param("ownerId") Long ownerId);
}
