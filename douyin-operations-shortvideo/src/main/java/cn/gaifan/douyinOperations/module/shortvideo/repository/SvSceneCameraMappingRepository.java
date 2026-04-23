package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvSceneCameraMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 场景-运镜推荐映射 Repository (Phase 5)
 */
public interface SvSceneCameraMappingRepository extends JpaRepository<SvSceneCameraMapping, Long> {

    List<SvSceneCameraMapping> findAllByOrderByConfidenceDesc();
}
