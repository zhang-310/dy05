package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.dto.AuthUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * AuthUser Repository with N+1 query optimization
 */
public interface AuthUserRepository extends JpaRepository<AuthUser, Long>, JpaSpecificationExecutor<AuthUser> {

    Optional<AuthUser> findByUsernameAndDeleted(String username, Integer deleted);

    Optional<AuthUser> findByMobileAndDeleted(String mobile, Integer deleted);

    Optional<AuthUser> findByEmailAndDeleted(String email, Integer deleted);

    Page<AuthUser> findByDeleted(Integer deleted, Pageable pageable);

    boolean existsByUsernameAndDeleted(String username, Integer deleted);

    @Override
    Optional<AuthUser> findById(Long id);

    @Override
    Page<AuthUser> findAll(Specification<AuthUser> spec, Pageable pageable);

    Page<AuthUserDTO> findAllDTOBy(Specification<AuthUser> spec, Pageable pageable);

    long countByDeleted(Integer deleted);

    long countByStatusAndDeleted(Integer status, Integer deleted);

    long countByCreateTimeAfterAndDeleted(Timestamp createTime, Integer deleted);
}

