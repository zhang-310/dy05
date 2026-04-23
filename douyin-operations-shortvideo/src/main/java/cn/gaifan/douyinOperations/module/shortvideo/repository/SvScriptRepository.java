package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 脚本 Repository
 */
public interface SvScriptRepository extends JpaRepository<SvScript, Long>, JpaSpecificationExecutor<SvScript> {
}
