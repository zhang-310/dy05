package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalApiConfigRepository extends JpaRepository<ExternalApiConfig, Long>,
        JpaSpecificationExecutor<ExternalApiConfig> {

    Optional<ExternalApiConfig> findByProviderCodeAndDeleted(String providerCode, Integer deleted);

    List<ExternalApiConfig> findByCategoryAndIsEnabledAndDeleted(String category, Boolean isEnabled, Integer deleted);

    List<ExternalApiConfig> findByIsEnabledAndDeleted(Boolean isEnabled, Integer deleted);
}
