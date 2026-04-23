package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 对标账号Repository
 */
@Repository
public interface BenchmarkAccountRepository extends JpaRepository<BenchmarkAccount, Long>, JpaSpecificationExecutor<BenchmarkAccount> {

    /**
     * 根据ownerId和secUid查找账号
     */
    Optional<BenchmarkAccount> findByOwnerIdAndSecUid(Long ownerId, String secUid);

    /**
     * 根据secUid和ownerId查找账号（用于检查是否已存在）
     */
    BenchmarkAccount findBySecUidAndOwnerId(String secUid, Long ownerId);

    /**
     * 根据ownerId和平台查找所有账号
     */
    List<BenchmarkAccount> findByOwnerIdAndPlatform(Long ownerId, String platform);

    /**
     * 根据ownerId和激活状态查找账号
     */
    List<BenchmarkAccount> findByOwnerIdAndIsActive(Long ownerId, Boolean isActive);
}
