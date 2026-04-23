package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ScriptGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScriptGenerationRepository extends JpaRepository<ScriptGeneration, Long>,
        JpaSpecificationExecutor<ScriptGeneration> {
}
