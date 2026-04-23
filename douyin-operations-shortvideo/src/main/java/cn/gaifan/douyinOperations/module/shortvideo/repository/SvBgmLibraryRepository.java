package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvBgmLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * BGM 素材库 Repository（Phase 4.2）
 */
public interface SvBgmLibraryRepository extends JpaRepository<SvBgmLibrary, Long> {

    List<SvBgmLibrary> findByStyleAndDeleted(String style, Integer deleted);
}
