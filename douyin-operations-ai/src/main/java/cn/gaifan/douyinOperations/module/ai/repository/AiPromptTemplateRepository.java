package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, Long>, JpaSpecificationExecutor<AiPromptTemplate> {
    Optional<AiPromptTemplate> findByIdAndDeleted(Long id, Integer deleted);
    List<AiPromptTemplate> findByUserIdAndStatusAndDeleted(Long userId, Integer status, Integer deleted);

    /** 按 templateCode + variantName + deleted 查询 */
    List<AiPromptTemplate> findByTemplateCodeAndVariantNameAndDeleted(String templateCode, String variantName, Integer deleted);

    /** 按 templateCode + variantName + ownerId + deleted 查询（精确匹配） */
    Optional<AiPromptTemplate> findFirstByTemplateCodeAndVariantNameAndOwnerIdAndDeleted(String templateCode, String variantName, Long ownerId, Integer deleted);

    /** 按 templateCode + isActive + deleted 查询活跃模板 */
    List<AiPromptTemplate> findByTemplateCodeAndIsActiveAndDeletedOrderByOwnerIdDesc(String templateCode, Integer isActive, Integer deleted);

    /** 按 templateCode + variantName + isActive + deleted 查询 */
    List<AiPromptTemplate> findByTemplateCodeAndVariantNameAndIsActiveAndDeleted(String templateCode, String variantName, Integer isActive, Integer deleted);
}
