package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ScriptVersionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScriptVersionHistoryRepository extends JpaRepository<ScriptVersionHistory, Long> {

    List<ScriptVersionHistory> findByScriptIdOrderByCreateTimeDesc(Long scriptId);

    List<ScriptVersionHistory> findByProductIdAndScriptTypeAndStyleOrderByCreateTimeDesc(
            Long productId, String scriptType, String style);
}
