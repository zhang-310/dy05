package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkPromptAbTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Prompt A/B 测试 Repository
 */
@Repository
public interface BenchmarkPromptAbTestRepository extends JpaRepository<BenchmarkPromptAbTest, Long>, JpaSpecificationExecutor<BenchmarkPromptAbTest> {

    /**
     * 根据测试编码查询
     */
    Optional<BenchmarkPromptAbTest> findByTestCodeAndDeleted(String testCode, Integer deleted);

    /**
     * 查询指定状态的测试
     */
    List<BenchmarkPromptAbTest> findByOwnerIdAndStatusAndDeleted(Long ownerId, String status, Integer deleted);

    /**
     * 查询正在运行的测试
     */
    List<BenchmarkPromptAbTest> findByStatusAndDeleted(String status, Integer deleted);

    /**
     * 查询包含指定模板的测试
     */
    List<BenchmarkPromptAbTest> findByTemplateAIdOrTemplateBIdAndDeleted(Long templateAId, Long templateBId, Integer deleted);
}
