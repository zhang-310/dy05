package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/**
 * 素材库 Repository
 */
public interface SvMaterialRepository extends JpaRepository<SvMaterial, Long>, JpaSpecificationExecutor<SvMaterial> {

    List<SvMaterial> findByProjectIdInAndMaterialType(Collection<Long> projectIds, String materialType);

    List<SvMaterial> findByProjectIdAndMaterialType(Long projectId, String materialType);
}
