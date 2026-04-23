package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 直播话术版本 Repository
 */
public interface LiveScriptVersionRepository extends JpaRepository<LiveScriptVersion, Long>, JpaSpecificationExecutor<LiveScriptVersion> {

    /**
     * 根据话术ID和版本号查询特定版本
     */
    Optional<LiveScriptVersion> findByScriptIdAndVersionNoAndDeleted(Long scriptId, Integer versionNo, Integer deleted);

    /**
     * 获取某话术的最新版本
     */
    @Query("SELECT sv FROM LiveScriptVersion sv WHERE sv.scriptId = :scriptId AND sv.deleted = 0 ORDER BY sv.versionNo DESC LIMIT 1")
    Optional<LiveScriptVersion> findLatestVersionByScriptId(@Param("scriptId") Long scriptId);

    /**
     * 获取某话术的所有版本（按版本号倒序）
     */
    List<LiveScriptVersion> findByScriptIdAndDeletedOrderByVersionNoDesc(Long scriptId, Integer deleted);

    /**
     * 分页查询某话术的版本
     */
    Page<LiveScriptVersion> findByScriptIdAndDeletedOrderByVersionNoDesc(Long scriptId, Integer deleted, Pageable pageable);

    /**
     * 根据直播场次ID分页查询所有版本
     */
    Page<LiveScriptVersion> findBySessionIdAndDeletedOrderByCreateTimeDesc(Long sessionId, Integer deleted, Pageable pageable);

    /**
     * 查询推荐版本（is_recommended=1）
     */
    List<LiveScriptVersion> findByScriptIdAndIsRecommendedAndDeleted(Long scriptId, Integer isRecommended, Integer deleted);

    /**
     * 获取该用户所有活跃版本（用于权限验证）
     */
    List<LiveScriptVersion> findByOwnerIdAndVersionStatusAndDeleted(Long ownerId, String versionStatus, Integer deleted);

    /**
     * 查询效果评分较好的版本（用于推荐）
     */
    @Query("SELECT sv FROM LiveScriptVersion sv WHERE sv.scriptId = :scriptId AND sv.deleted = 0 AND sv.effectivenessScore >= :threshold ORDER BY sv.effectivenessScore DESC")
    List<LiveScriptVersion> findByScriptIdAndEffectivenessScoreGreaterThanEqual(@Param("scriptId") Long scriptId, @Param("threshold") BigDecimal threshold);

    /**
     * 查询推荐分数较高的版本
     */
    @Query("SELECT sv FROM LiveScriptVersion sv WHERE sv.scriptId = :scriptId AND sv.deleted = 0 ORDER BY sv.recommendScore DESC NULLS LAST")
    List<LiveScriptVersion> findByScriptIdOrderByRecommendScoreDesc(@Param("scriptId") Long scriptId);

    /**
     * 统计某话术有多少个版本
     */
    long countByScriptIdAndDeleted(Long scriptId, Integer deleted);

    /**
     * 查询某用户在某场次下的所有版本
     */
    @Query("SELECT sv FROM LiveScriptVersion sv WHERE sv.sessionId = :sessionId AND sv.ownerId = :ownerId AND sv.deleted = 0 ORDER BY sv.createTime DESC")
    List<LiveScriptVersion> findBySessionIdAndOwnerId(@Param("sessionId") Long sessionId, @Param("ownerId") Long ownerId);

    /**
     * 查询某用户的所有版本（用于知识质量评分和进化报告）
     */
    List<LiveScriptVersion> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    /**
     * 统计某用户版本数量
     */
    long countByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    /**
     * 批量查询多个话术的最新版本
     */
    @Query("SELECT sv FROM LiveScriptVersion sv WHERE sv.scriptId IN :scriptIds AND sv.deleted = 0 AND sv.versionNo = (SELECT MAX(v.versionNo) FROM LiveScriptVersion v WHERE v.scriptId = sv.scriptId AND v.deleted = 0)")
    List<LiveScriptVersion> findLatestVersionsByScriptIds(@Param("scriptIds") List<Long> scriptIds);

    /**
     * 逻辑删除某话术的所有版本
     */
    @Query("UPDATE LiveScriptVersion sv SET sv.deleted = 1 WHERE sv.scriptId = :scriptId")
    long deleteByScriptId(@Param("scriptId") Long scriptId);

    /**
     * 查询被使用过的版本（usageCount > 0）
     */
    @Query("SELECT sv FROM LiveScriptVersion sv WHERE sv.scriptId = :scriptId AND sv.deleted = 0 AND sv.usageCount > 0 ORDER BY sv.usageCount DESC")
    List<LiveScriptVersion> findUsedVersionsByScriptId(@Param("scriptId") Long scriptId);
}
