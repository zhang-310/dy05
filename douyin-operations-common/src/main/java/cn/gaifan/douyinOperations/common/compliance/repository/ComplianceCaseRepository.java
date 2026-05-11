package cn.gaifan.douyinOperations.common.compliance.repository;

import cn.gaifan.douyinOperations.common.compliance.entity.ComplianceCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 违规案例库 Repository
 */
@Repository
public interface ComplianceCaseRepository extends JpaRepository<ComplianceCase, Long>, JpaSpecificationExecutor<ComplianceCase> {

    /**
     * 根据规则 ID 查询案例
     */
    @Query("SELECT c FROM ComplianceCase c WHERE c.ruleId = :ruleId AND c.status = 1 AND c.deleted = 0 ORDER BY c.caseDate DESC")
    List<ComplianceCase> findByRuleIdAndEnabled(@Param("ruleId") Long ruleId);

    /**
     * 查询所有启用的案例
     */
    @Query("SELECT c FROM ComplianceCase c WHERE c.status = 1 AND c.deleted = 0 ORDER BY c.caseDate DESC")
    List<ComplianceCase> findAllEnabled();
}
