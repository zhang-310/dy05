package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 对标视频Repository
 */
@Repository
public interface BenchmarkVideoRepository extends JpaRepository<BenchmarkVideo, Long>, JpaSpecificationExecutor<BenchmarkVideo> {

    /**
     * 根据账号ID查找所有视频
     */
    List<BenchmarkVideo> findByBenchmarkAccountId(Long benchmarkAccountId);

    /**
     * 根据账号ID和分析状态查找视频
     */
    List<BenchmarkVideo> findByBenchmarkAccountIdAndAnalysisStatus(Long benchmarkAccountId, String analysisStatus);

    /**
     * 根据视频ID查找
     */
    Optional<BenchmarkVideo> findByVideoId(String videoId);

    /**
     * 根据视频ID和账号ID查找
     */
    Optional<BenchmarkVideo> findByVideoIdAndBenchmarkAccountId(String videoId, Long benchmarkAccountId);

    /**
     * 统计账号的视频数量
     */
    int countByBenchmarkAccountId(Long benchmarkAccountId);

    long countByDeleted(Integer deleted);

    long countByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    long countByAnalysisStatusAndDeleted(String analysisStatus, Integer deleted);

    long countByOwnerIdAndAnalysisStatusAndDeleted(Long ownerId, String analysisStatus, Integer deleted);

    @Query("SELECT MAX(v.createTime) FROM BenchmarkVideo v WHERE v.deleted = 0")
    java.time.LocalDateTime findLastCreateTime();

    @Query("SELECT MAX(v.createTime) FROM BenchmarkVideo v WHERE v.ownerId = :ownerId AND v.deleted = 0")
    java.time.LocalDateTime findLastCreateTimeByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 根据账号ID和是否符合条件查找视频
     */
    List<BenchmarkVideo> findByBenchmarkAccountIdAndIsQualified(Long benchmarkAccountId, Boolean isQualified);

    /**
     * 批量查询视频（优化：使用IN查询）
     */
    @Query("SELECT v FROM BenchmarkVideo v WHERE v.id IN :ids AND v.deleted = 0")
    List<BenchmarkVideo> findByIdIn(@Param("ids") List<Long> ids);

    /**
     * 批量查询待分析视频（优化：限制数量）
     */
    @Query("SELECT v FROM BenchmarkVideo v WHERE v.benchmarkAccountId = :accountId " +
           "AND v.analysisStatus = 'pending' AND v.isQualified = true AND v.deleted = 0 " +
           "ORDER BY v.likeCount DESC")
    List<BenchmarkVideo> findPendingVideosForAnalysis(@Param("accountId") Long accountId);

    /**
     * 查询高质量视频（点赞数排名前N）
     */
    @Query(value = "SELECT * FROM benchmark_video WHERE benchmark_account_id = :accountId " +
           "AND deleted = 0 ORDER BY like_count DESC LIMIT :limit", nativeQuery = true)
    List<BenchmarkVideo> findTopVideosByLikes(@Param("accountId") Long accountId, @Param("limit") int limit);

    /**
     * 统计各状态视频数量
     */
    @Query("SELECT v.analysisStatus, COUNT(v) FROM BenchmarkVideo v " +
           "WHERE v.benchmarkAccountId = :accountId AND v.deleted = 0 " +
           "GROUP BY v.analysisStatus")
    List<Object[]> countByAnalysisStatus(@Param("accountId") Long accountId);
}
