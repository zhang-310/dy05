package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AiKbDocumentRepository extends JpaRepository<AiKbDocument, Long>, JpaSpecificationExecutor<AiKbDocument> {
    List<AiKbDocument> findByKbIdAndDeletedOrderByCreateTimeDesc(Long kbId, Integer deleted);

    /** 时效性检测：未检测或超过 90 天未检测的文档 */
    @Query("SELECT d FROM AiKbDocument d WHERE d.deleted = 0 AND (d.lastExpiryCheck IS NULL OR d.lastExpiryCheck < :cutoff) ORDER BY d.createTime DESC")
    List<AiKbDocument> findForFreshnessCheck(@Param("cutoff") Timestamp cutoff, Pageable pageable);

    @Query("SELECT d FROM AiKbDocument d WHERE d.deleted = 0 AND d.kbId = :kbId AND (d.lastExpiryCheck IS NULL OR d.lastExpiryCheck < :cutoff) ORDER BY d.createTime DESC")
    List<AiKbDocument> findForFreshnessCheckByKb(@Param("kbId") Long kbId, @Param("cutoff") Timestamp cutoff, Pageable pageable);
    long countByKbIdAndDeleted(Long kbId, Integer deleted);

    /** 按 ID 列表批量查询（用于 boost 更新、检索加权） */
    List<AiKbDocument> findByIdIn(List<Long> ids);

    /** 按知识库 + 标题查询是否存在（用于导入去重） */
    boolean existsByKbIdAndTitleAndDeleted(Long kbId, String title, Integer deleted);

    /** 按知识库 + 内容指纹查询是否存在（文档级完全一致去重） */
    boolean existsByKbIdAndContentFingerprintAndDeleted(Long kbId, String contentFingerprint, Integer deleted);

    /** 按知识库 + 内容指纹取已存在文档（用于返回“已存在”的文档） */
    java.util.Optional<AiKbDocument> findFirstByKbIdAndContentFingerprintAndDeletedOrderByIdDesc(Long kbId, String contentFingerprint, Integer deleted);

    /** 取知识库下所有文档的 simhash（用于 SimHash 海明距离检测，仅非空） */
    @Query("SELECT d.simhash FROM AiKbDocument d WHERE d.kbId = :kbId AND d.deleted = 0 AND d.simhash IS NOT NULL")
    List<Long> findSimhashesByKbId(@Param("kbId") Long kbId);

    /** 按来源类型查询（用于知识引用率统计） */
    List<AiKbDocument> findBySourceTypeAndDeleted(String sourceType, Integer deleted);

    /** 统计指定来源的文档数 */
    long countBySourceTypeAndDeleted(String sourceType, Integer deleted);

    /** P2 质量启发扫描：按 id 升序分页 */
    org.springframework.data.domain.Page<AiKbDocument> findByDeletedOrderByIdAsc(int deleted, org.springframework.data.domain.Pageable pageable);

    /** 按知识库+来源类型查询高引用文档（跨域共享用） */
    @Query("SELECT d FROM AiKbDocument d WHERE d.kbId = :kbId AND d.sourceType = :sourceType AND d.deleted = 0 AND d.citationCount >= :minCitation ORDER BY d.citationCount DESC")
    List<AiKbDocument> findTopCitedByKbAndSourceType(@Param("kbId") Long kbId, @Param("sourceType") String sourceType, @Param("minCitation") Long minCitation, Pageable pageable);

    /** 按来源类型查询高引用文档（全局，用于报告） */
    @Query("SELECT d FROM AiKbDocument d WHERE d.sourceType = :sourceType AND d.deleted = 0 ORDER BY d.citationCount DESC NULLS LAST")
    List<AiKbDocument> findTopCitedBySourceType(@Param("sourceType") String sourceType, Pageable pageable);

    /** 按知识库+来源类型+逻辑删除 查询（huashu 维护用） */
    List<AiKbDocument> findByKbIdAndSourceTypeAndDeleted(Long kbId, String sourceType, Integer deleted);

    /** 冷门文档：最近N天未被检索 */
    @Query("SELECT d FROM AiKbDocument d WHERE d.deleted = 0 AND (d.lastRetrievalAt IS NULL OR d.lastRetrievalAt < :cutoff) ORDER BY d.createTime ASC")
    List<AiKbDocument> findColdDocs(@Param("cutoff") Timestamp cutoff, Pageable pageable);

    /** 统计冷门文档数量 */
    @Query("SELECT COUNT(d) FROM AiKbDocument d WHERE d.deleted = 0 AND (d.lastRetrievalAt IS NULL OR d.lastRetrievalAt < :cutoff)")
    long countColdDocs(@Param("cutoff") Timestamp cutoff);

    /** 时效性过期：expiry_status=2 的文档入队更新 */
    @Query("SELECT d FROM AiKbDocument d WHERE d.deleted = 0 AND d.expiryStatus = 2 ORDER BY d.lastExpiryCheck ASC NULLS FIRST")
    List<AiKbDocument> findExpiredForUpdate(Pageable pageable);

    /** 批量增加检索计数 */
    @Modifying
    @Query("UPDATE AiKbDocument d SET d.retrievalCount = COALESCE(d.retrievalCount, 0) + 1 WHERE d.id IN :ids")
    int incrementRetrievalCount(@Param("ids") List<Long> ids);

    /** 统计用户知识库的总引用数和总检索数（质量评分用），返回 Object[]{citationSum, retrievalSum} */
    @Query("SELECT COALESCE(SUM(d.citationCount), 0), COALESCE(SUM(d.retrievalCount), 0) FROM AiKbDocument d " +
           "JOIN AiKnowledgeBase k ON d.kbId = k.id WHERE k.userId = :userId AND d.deleted = 0 AND k.deleted = 0")
    Object[] sumCitationAndRetrievalByUserId(@Param("userId") Long userId);

    /** 批量增加引用计数 */
    @Modifying
    @Query("UPDATE AiKbDocument d SET d.citationCount = COALESCE(d.citationCount, 0) + 1 WHERE d.id IN :ids")
    int incrementCitationCount(@Param("ids") List<Long> ids);

    /** huashu 维护：sourceType 为 null 或 sourceType = 指定值的文档（用于回填 sourceType） */
    @Query("SELECT d FROM AiKbDocument d WHERE d.kbId = :kbId AND d.deleted = 0 AND (d.sourceType IS NULL OR d.sourceType = :sourceType)")
    List<AiKbDocument> findByKbIdAndSourceTypeNullOr(@Param("kbId") Long kbId, @Param("sourceType") String sourceType, org.springframework.data.domain.Pageable pageable);

    /** P0 双写补偿：status=0 且超时未完成，sync_retry_count<3 */
    @Query("SELECT d FROM AiKbDocument d WHERE d.deleted = 0 AND d.status = 0 AND (d.syncRetryCount IS NULL OR d.syncRetryCount < 3) AND d.createTime < :cutoff ORDER BY d.createTime ASC")
    List<AiKbDocument> findStuckForCompensation(@Param("cutoff") Timestamp cutoff, Pageable pageable);
}
