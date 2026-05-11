package cn.gaifan.douyinOperations.common.compliance.repository;

import cn.gaifan.douyinOperations.common.compliance.entity.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 违规规则 Repository
 */
@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, Long>, JpaSpecificationExecutor<ComplianceRule> {

    /**
     * 根据规则编码查询
     */
    ComplianceRule findByRuleCode(String ruleCode);

    /**
     * 根据分类查询启用的规则
     */
    @Query("SELECT r FROM ComplianceRule r WHERE r.category = :category AND r.status = 1 AND r.deleted = 0")
    List<ComplianceRule> findByCategoryAndEnabled(@Param("category") String category);

    /**
     * 根据分类和子分类查询启用的规则
     */
    @Query("SELECT r FROM ComplianceRule r WHERE r.category = :category AND r.subCategory = :subCategory AND r.status = 1 AND r.deleted = 0")
    List<ComplianceRule> findByCategoryAndSubCategoryAndEnabled(
            @Param("category") String category,
            @Param("subCategory") String subCategory
    );

    /**
     * 根据严重程度查询启用的规则
     */
    @Query("SELECT r FROM ComplianceRule r WHERE r.severity = :severity AND r.status = 1 AND r.deleted = 0")
    List<ComplianceRule> findBySeverityAndEnabled(@Param("severity") String severity);

    /**
     * 查询所有启用的规则
     */
    @Query("SELECT r FROM ComplianceRule r WHERE r.status = 1 AND r.deleted = 0 ORDER BY r.severity, r.category")
    List<ComplianceRule> findAllEnabled();
}
