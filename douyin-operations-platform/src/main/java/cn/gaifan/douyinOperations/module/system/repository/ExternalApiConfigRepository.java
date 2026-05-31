package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalApiConfigRepository extends JpaRepository<ExternalApiConfig, Long>,
        JpaSpecificationExecutor<ExternalApiConfig> {

    Optional<ExternalApiConfig> findByProviderCodeAndDeleted(String providerCode, Integer deleted);

    List<ExternalApiConfig> findByCategoryAndIsEnabledAndDeleted(String category, Boolean isEnabled, Integer deleted);

    List<ExternalApiConfig> findByIsEnabledAndDeleted(Boolean isEnabled, Integer deleted);

    @Modifying
    @Query("UPDATE ExternalApiConfig c SET c.healthStatus = :status, c.avgLatencyMs = :latencyMs, "
            + "c.successRatePct = :successRate, c.lastHealthCheck = :lastHealthCheck, c.updateTime = :lastHealthCheck "
            + "WHERE c.providerCode = :providerCode AND c.deleted = 0")
    int updateHealthFields(@Param("providerCode") String providerCode,
                           @Param("status") String status,
                           @Param("latencyMs") Integer latencyMs,
                           @Param("successRate") Float successRate,
                           @Param("lastHealthCheck") Timestamp lastHealthCheck);
}
