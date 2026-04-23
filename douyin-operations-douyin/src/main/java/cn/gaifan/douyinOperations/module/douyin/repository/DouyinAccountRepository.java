package cn.gaifan.douyinOperations.module.douyin.repository;

import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 抖音账号表 Repository
 */
public interface DouyinAccountRepository extends JpaRepository<DouyinAccount, Long>, JpaSpecificationExecutor<DouyinAccount> {

    Page<DouyinAccount> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    Optional<DouyinAccount> findByIdAndDeleted(Long id, Integer deleted);

    Page<DouyinAccount> findByStatusAndDeleted(Integer status, Integer deleted, Pageable pageable);

    boolean existsByAccountIdAndDeleted(String accountId, Integer deleted);

    @Query("SELECT a.id FROM DouyinAccount a WHERE a.userId IN :userIds AND a.deleted = 0")
    List<Long> findIdsByUserIdIn(@Param("userIds") List<Long> userIds);
}
