package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AuthOrganizationRepository extends JpaRepository<AuthOrganization, Long>, JpaSpecificationExecutor<AuthOrganization> {

    Optional<AuthOrganization> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    Optional<AuthOrganization> findByOrgCodeAndDeleted(String orgCode, Integer deleted);

    boolean existsByOrgCodeAndDeleted(String orgCode, Integer deleted);
}
