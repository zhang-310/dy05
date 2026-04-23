package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkPromptUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt 使用日志 Repository
 */
@Repository
public interface BenchmarkPromptUsageLogRepository extends JpaRepository<BenchmarkPromptUsageLog, Long>, JpaSpecificationExecutor<BenchmarkPromptUsageLog> {

    /**
     * 查询指定模板的使用日志
     */
    List<BenchmarkPromptUsageLog> findByTemplateIdAndDeletedOrderByCreateTimeDesc(Long templateId, Integer deleted);

    /**
     * 查询指定视频的使用日志
     */
    List<BenchmarkPromptUsageLog> findByVideoIdAndDeleted(Long videoId, Integer deleted);

    /**
     * 查询指定 A/B 测试组的日志
     */
    List<BenchmarkPromptUsageLog> findByAbTestGroupAndDeleted(String abTestGroup, Integer deleted);

    /**
     * 统计模板的平均评分
     */
    @Query("SELECT AVG(l.userRating) FROM BenchmarkPromptUsageLog l WHERE l.templateId = :templateId AND l.userRating IS NOT NULL AND l.deleted = 0")
    Double calculateAvgRating(@Param("templateId") Long templateId);

    /**
     * 统计模板的使用次数
     */
    @Query("SELECT COUNT(l) FROM BenchmarkPromptUsageLog l WHERE l.templateId = :templateId AND l.deleted = 0")
    Long countUsageByTemplateId(@Param("templateId") Long templateId);

    /**
     * 查询时间范围内的使用日志
     */
    List<BenchmarkPromptUsageLog> findByOwnerIdAndCreateTimeBetweenAndDeleted(Long ownerId, LocalDateTime startTime, LocalDateTime endTime, Integer deleted);
}
