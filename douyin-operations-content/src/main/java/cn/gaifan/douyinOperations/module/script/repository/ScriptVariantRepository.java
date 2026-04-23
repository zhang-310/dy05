package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ScriptVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScriptVariantRepository extends JpaRepository<ScriptVariant, Long> {
    List<ScriptVariant> findByGenerationId(Long generationId);
}
