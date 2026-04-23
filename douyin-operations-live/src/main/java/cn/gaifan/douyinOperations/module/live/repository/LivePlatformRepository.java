package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LivePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface LivePlatformRepository extends JpaRepository<LivePlatform, Long>, JpaSpecificationExecutor<LivePlatform> {

    Optional<LivePlatform> findByPlatformCodeAndDeleted(String platformCode, Integer deleted);

    List<LivePlatform> findByActiveAndDeleted(Integer active, Integer deleted);
}
