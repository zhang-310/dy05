package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 分镜详情 Repository
 */
public interface SvShotRepository extends JpaRepository<SvShot, Long> {

    List<SvShot> findByShotListIdAndDeletedOrderByShotNumberAsc(Long shotListId, Integer deleted);
}
