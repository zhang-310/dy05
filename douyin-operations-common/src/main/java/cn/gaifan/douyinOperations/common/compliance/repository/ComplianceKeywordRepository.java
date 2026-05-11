package cn.gaifan.douyinOperations.common.compliance.repository;

import cn.gaifan.douyinOperations.common.compliance.entity.ComplianceKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 敏感词库 Repository
 */
@Repository
public interface ComplianceKeywordRepository extends JpaRepository<ComplianceKeyword, Long>, JpaSpecificationExecutor<ComplianceKeyword> {

    /**
     * 根据分类查询启用的敏感词
     */
    @Query("SELECT k FROM ComplianceKeyword k WHERE k.category = :category AND k.status = 1 AND k.deleted = 0")
    List<ComplianceKeyword> findByCategoryAndEnabled(@Param("category") String category);

    /**
     * 根据严重程度查询启用的敏感词
     */
    @Query("SELECT k FROM ComplianceKeyword k WHERE k.severity = :severity AND k.status = 1 AND k.deleted = 0")
    List<ComplianceKeyword> findBySeverityAndEnabled(@Param("severity") String severity);

    /**
     * 查询所有启用的敏感词
     */
    @Query("SELECT k FROM ComplianceKeyword k WHERE k.status = 1 AND k.deleted = 0 ORDER BY k.severity, k.category")
    List<ComplianceKeyword> findAllEnabled();

    /**
     * 根据关键词查询
     */
    ComplianceKeyword findByKeyword(String keyword);
}
