package cn.gaifan.douyinOperations.module.messaging.repository;

import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MsgPlatformConfigRepository extends JpaRepository<MsgPlatformConfig, Long>, JpaSpecificationExecutor<MsgPlatformConfig> {

    // P2-003: 移除 AndDeleted 后缀，@SQLRestriction 自动过滤 deleted=0
    Optional<MsgPlatformConfig> findById(Long id);

    List<MsgPlatformConfig> findByPlatformAndStatus(String platform, Integer status);

    Optional<MsgPlatformConfig> findByPlatformAndCallbackToken(String platform, String callbackToken);
}
