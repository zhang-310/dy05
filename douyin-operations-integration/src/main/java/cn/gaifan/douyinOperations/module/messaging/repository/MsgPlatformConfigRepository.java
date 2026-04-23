package cn.gaifan.douyinOperations.module.messaging.repository;

import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MsgPlatformConfigRepository extends JpaRepository<MsgPlatformConfig, Long>, JpaSpecificationExecutor<MsgPlatformConfig> {

    Optional<MsgPlatformConfig> findByIdAndDeleted(Long id, Integer deleted);

    List<MsgPlatformConfig> findByPlatformAndStatusAndDeleted(String platform, Integer status, Integer deleted);

    Optional<MsgPlatformConfig> findByPlatformAndCallbackTokenAndDeleted(String platform, String callbackToken, Integer deleted);
}
