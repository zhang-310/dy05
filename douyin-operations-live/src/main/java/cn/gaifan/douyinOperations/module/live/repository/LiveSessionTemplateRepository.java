package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveSessionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LiveSessionTemplateRepository extends JpaRepository<LiveSessionTemplate, Long>,
        JpaSpecificationExecutor<LiveSessionTemplate> {

    Optional<LiveSessionTemplate> findByIdAndDeleted(Long id, Integer deleted);

    Optional<LiveSessionTemplate> findByOwnerIdAndCodeAndDeleted(Long ownerId, String code, Integer deleted);
}
