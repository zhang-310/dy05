package cn.gaifan.douyinOperations.module.config.repository;

import cn.gaifan.douyinOperations.module.config.entity.SysConfigGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysConfigGroupRepository extends JpaRepository<SysConfigGroup, Long>, JpaSpecificationExecutor<SysConfigGroup> {

    Optional<SysConfigGroup> findByIdAndDeleted(Long id, Integer deleted);

    Optional<SysConfigGroup> findByGroupCodeAndDeleted(String groupCode, Integer deleted);

    List<SysConfigGroup> findByParentIdAndDeletedOrderBySortOrderAsc(Long parentId, Integer deleted);

    List<SysConfigGroup> findByStatusAndDeletedOrderBySortOrderAsc(Integer status, Integer deleted);
}
