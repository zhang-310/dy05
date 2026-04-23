package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 模板 Repository
 */
@Repository
public interface BenchmarkPromptTemplateRepository extends JpaRepository<BenchmarkPromptTemplate, Long>, JpaSpecificationExecutor<BenchmarkPromptTemplate> {

    /**
     * 根据模板编码查询
     */
    Optional<BenchmarkPromptTemplate> findByTemplateCodeAndDeleted(String templateCode, Integer deleted);

    /**
     * 查询用户的所有激活模板
     */
    List<BenchmarkPromptTemplate> findByOwnerIdAndIsActiveAndDeleted(Long ownerId, Boolean isActive, Integer deleted);

    /**
     * 查询指定场景类型的模板
     */
    List<BenchmarkPromptTemplate> findByOwnerIdAndSceneTypeAndIsActiveAndDeleted(Long ownerId, String sceneType, Boolean isActive, Integer deleted);

    /**
     * 查询指定行业的模板
     */
    List<BenchmarkPromptTemplate> findByOwnerIdAndIndustryAndIsActiveAndDeleted(Long ownerId, String industry, Boolean isActive, Integer deleted);
}
