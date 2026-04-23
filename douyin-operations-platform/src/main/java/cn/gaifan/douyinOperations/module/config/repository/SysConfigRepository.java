package cn.gaifan.douyinOperations.module.config.repository;

import cn.gaifan.douyinOperations.module.config.entity.SysConfig;
import cn.gaifan.douyinOperations.module.config.dto.ConfigDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface SysConfigRepository extends JpaRepository<SysConfig, Long>, JpaSpecificationExecutor<SysConfig> {

    Optional<SysConfig> findByConfigKeyAndDeleted(String configKey, Integer deleted);

    Page<ConfigDTO> findAllDTOBy(Specification<SysConfig> spec, Pageable pageable);
}

