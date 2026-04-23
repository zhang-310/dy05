package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ScriptCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ScriptCheckRepository extends JpaRepository<ScriptCheck, Long>, JpaSpecificationExecutor<ScriptCheck> {

    Optional<ScriptCheck> findByIdAndDeleted(Long id, Integer deleted);
}
