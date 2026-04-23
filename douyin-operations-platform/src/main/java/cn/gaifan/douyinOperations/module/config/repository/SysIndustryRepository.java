package cn.gaifan.douyinOperations.module.config.repository;

import cn.gaifan.douyinOperations.module.config.entity.SysIndustry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysIndustryRepository extends JpaRepository<SysIndustry, Long>, JpaSpecificationExecutor<SysIndustry> {

    Optional<SysIndustry> findByIdAndDeleted(Long id, Integer deleted);

    Optional<SysIndustry> findByIndustryCodeAndDeleted(String industryCode, Integer deleted);

    List<SysIndustry> findByParentIdAndDeletedOrderBySortOrderAsc(Long parentId, Integer deleted);

    List<SysIndustry> findByStatusAndDeletedOrderBySortOrderAsc(Integer status, Integer deleted);
}
