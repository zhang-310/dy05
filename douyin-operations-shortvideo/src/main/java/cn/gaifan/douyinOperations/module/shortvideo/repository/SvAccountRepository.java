package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 短视频账号主表 Repository
 */
@Repository
public interface SvAccountRepository extends JpaRepository<SvAccount, Long>, JpaSpecificationExecutor<SvAccount> {

    /**
     * 根据 owner_id 和 sec_uid 查找账号
     */
    Optional<SvAccount> findByOwnerIdAndSecUidAndDeleted(Long ownerId, String secUid, Integer deleted);

    /**
     * 根据 ID 和 deleted 查找
     */
    Optional<SvAccount> findByIdAndDeleted(Long id, Integer deleted);
}
