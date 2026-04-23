package cn.gaifan.douyinOperations.module.copy.repository;

import cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface CopyLibraryRepository extends JpaRepository<CopyLibrary, Long>, JpaSpecificationExecutor<CopyLibrary> {

    Optional<CopyLibrary> findByIdAndDeleted(Long id, Integer deleted);

    @Query("SELECT c.id FROM CopyLibrary c WHERE c.deleted = 0 AND c.title LIKE :keyword")
    List<Long> findIdsByTitleContaining(@Param("keyword") String keyword);

    Page<CopyLibrary> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    @Modifying
    @Query("UPDATE CopyLibrary c SET c.useCount = c.useCount + 1 WHERE c.id = :id")
    void incrementUseCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE CopyLibrary c SET c.status = :status WHERE c.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    // === Dashboard 统计方法 ===

    /** 统计总文案库数（未删除） */
    long countByDeleted(Integer deleted);

    /** 统计某用户的文案库数 */
    long countByUserIdAndDeleted(Long userId, Integer deleted);

    /** 统计某用户在某日期后创建的文案库数 */
    long countByUserIdAndCreateTimeAfterAndDeleted(
            @Param("userId") Long userId,
            @Param("createTime") Timestamp createTime,
            @Param("deleted") Integer deleted);

    /** 统计今日创建的文案库数（全局） */
    long countByCreateTimeAfterAndDeleted(Timestamp createTime, Integer deleted);

    /** 统计已审批通过的文案库数 */
    @Query("SELECT COUNT(c) FROM CopyLibrary c WHERE c.deleted = 0 AND c.status = 1")
    long countApprovedCopies();

    /** 统计某用户已审批通过的文案库数 */
    @Query("SELECT COUNT(c) FROM CopyLibrary c WHERE c.userId = :userId AND c.deleted = 0 AND c.status = 1")
    long countApprovedCopiesByUserId(@Param("userId") Long userId);

    /** 统计特定状态的文案库数 */
    long countByStatusAndDeleted(Integer status, Integer deleted);

    /** 统计某用户特定状态的文案库数 */
    long countByUserIdAndStatusAndDeleted(Long userId, Integer status, Integer deleted);

    /** 统计今日审批通过的文案库数 */
    @Query("SELECT COUNT(c) FROM CopyLibrary c WHERE c.deleted = 0 AND c.status = 1 AND c.createTime >= :startTime")
    long countApprovedCopiesSince(@Param("startTime") Timestamp startTime);

    /** 统计某用户今日审批通过的文案库数 */
    @Query("SELECT COUNT(c) FROM CopyLibrary c WHERE c.userId = :userId AND c.deleted = 0 AND c.status = 1 AND c.createTime >= :startTime")
    long countApprovedCopiesByUserIdSince(@Param("userId") Long userId, @Param("startTime") Timestamp startTime);

    /** 去重检测：按 userId + category + content 精确统计（天工API导入幂等） */
    @Query("SELECT COUNT(c) FROM CopyLibrary c WHERE c.userId = :userId AND c.category = :category AND c.content = :content AND c.deleted = 0")
    long countByUserIdAndCategoryAndContentAndDeleted(@Param("userId") Long userId, @Param("category") String category, @Param("content") String content);
}
