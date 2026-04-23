package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShotList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 分镜列表 Repository
 */
public interface SvShotListRepository extends JpaRepository<SvShotList, Long> {

    List<SvShotList> findByScriptIdAndDeletedOrderByCreateTimeDesc(Long scriptId, Integer deleted);

    Page<SvShotList> findByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, Integer deleted, Pageable pageable);
}
