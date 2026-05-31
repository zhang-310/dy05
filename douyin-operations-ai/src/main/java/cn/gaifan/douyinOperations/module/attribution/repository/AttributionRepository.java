package cn.gaifan.douyinOperations.module.attribution.repository;

import cn.gaifan.douyinOperations.module.attribution.entity.Attribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AttributionRepository extends JpaRepository<Attribution, Long>, JpaSpecificationExecutor<Attribution> {

    List<Attribution> findBySessionIdAndDeleted(Long sessionId, Integer deleted);

    List<Attribution> findBySessionIdAndAttributionTypeAndDeleted(Long sessionId, String attributionType, Integer deleted);

    Optional<Attribution> findByIdAndDeleted(Long id, Integer deleted);

    List<Attribution> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    long deleteBySessionIdAndDeleted(Long sessionId, Integer deleted);
}
