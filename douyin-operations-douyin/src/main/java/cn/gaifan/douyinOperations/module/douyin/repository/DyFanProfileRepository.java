package cn.gaifan.douyinOperations.module.douyin.repository;

import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DyFanProfileRepository extends JpaRepository<DyFanProfile, Long> {

    /**
     * 查找账号的粉丝画像
     */
    List<DyFanProfile> findByAccountIdAndDeletedOrderBySyncTimeDesc(Long accountId, Integer deleted);

    /**
     * 查找账号最新的粉丝画像
     */
    Optional<DyFanProfile> findFirstByAccountIdAndDeletedOrderBySyncTimeDesc(Long accountId, Integer deleted);
}
