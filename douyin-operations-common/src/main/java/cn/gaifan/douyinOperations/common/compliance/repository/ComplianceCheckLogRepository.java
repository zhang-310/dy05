package cn.gaifan.douyinOperations.common.compliance.repository;

import cn.gaifan.douyinOperations.common.compliance.entity.ComplianceCheckLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * 违规检测记录 Repository
 */
@Repository
public interface ComplianceCheckLogRepository extends JpaRepository<ComplianceCheckLog, Long>, JpaSpecificationExecutor<ComplianceCheckLog> {

    /**
     * 根据用户 ID 查询检测记录
     */
    List<ComplianceCheckLog> findByUserIdOrderByCheckTimeDesc(Long userId);

    /**
     * 根据内容类型和内容 ID 查询检测记录
     */
    List<ComplianceCheckLog> findByContentTypeAndContentIdOrderByCheckTimeDesc(String contentType, Long contentId);

    /**
     * 根据检测结果查询记录
     */
    @Query("SELECT c FROM ComplianceCheckLog c WHERE c.checkResult = :result AND c.checkTime >= :startTime ORDER BY c.checkTime DESC")
    List<ComplianceCheckLog> findByCheckResultAndTimeRange(
            @Param("result") String result,
            @Param("startTime") Timestamp startTime
    );

    /**
     * 统计用户的违规次数
     */
    @Query("SELECT COUNT(c) FROM ComplianceCheckLog c WHERE c.userId = :userId AND c.checkResult = 'reject' AND c.checkTime >= :startTime")
    Long countViolationsByUser(
            @Param("userId") Long userId,
            @Param("startTime") Timestamp startTime
    );

    /**
     * 统计内容类型的违规次数
     */
    @Query("SELECT COUNT(c) FROM ComplianceCheckLog c WHERE c.contentType = :contentType AND c.checkResult = 'reject' AND c.checkTime >= :startTime")
    Long countViolationsByContentType(
            @Param("contentType") String contentType,
            @Param("startTime") Timestamp startTime
    );
}
