package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SvCategoryRepository extends JpaRepository<SvCategory, Long> {

    Optional<SvCategory> findByIdAndDeleted(Long id, Integer deleted);

    List<SvCategory> findByOwnerIdAndDeletedOrderBySortOrderAsc(Long ownerId, Integer deleted);
}
