package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiModelRoutingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface AiModelRoutingLogRepository extends JpaRepository<AiModelRoutingLog, Long> {

    /** 统计指定内容类型和模型的成功率 */
    @Query("SELECT COUNT(r) FROM AiModelRoutingLog r WHERE r.contentType = :contentType AND r.selectedModel = :model AND r.success = true AND r.createTime >= :since AND r.deleted = 0")
    long countSuccessByContentTypeAndModel(@Param("contentType") String contentType, @Param("model") String model, @Param("since") Timestamp since);

    @Query("SELECT COUNT(r) FROM AiModelRoutingLog r WHERE r.contentType = :contentType AND r.selectedModel = :model AND r.createTime >= :since AND r.deleted = 0")
    long countTotalByContentTypeAndModel(@Param("contentType") String contentType, @Param("model") String model, @Param("since") Timestamp since);

    /** 连续失败次数（最近 N 条） */
    @Query("SELECT r FROM AiModelRoutingLog r WHERE r.selectedModel = :model AND r.deleted = 0 ORDER BY r.createTime DESC")
    List<AiModelRoutingLog> findRecentByModel(@Param("model") String model);

    /** 指定模型的平均质量评分 */
    @Query("SELECT AVG(r.qualityScore) FROM AiModelRoutingLog r WHERE r.contentType = :contentType AND r.selectedModel = :model AND r.success = true AND r.createTime >= :since AND r.deleted = 0")
    Double getAvgQualityScore(@Param("contentType") String contentType, @Param("model") String model, @Param("since") Timestamp since);
}
