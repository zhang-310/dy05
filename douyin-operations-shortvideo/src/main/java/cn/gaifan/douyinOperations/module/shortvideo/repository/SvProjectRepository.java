package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * 短视频项目 Repository
 */
public interface SvProjectRepository extends JpaRepository<SvProject, Long>, JpaSpecificationExecutor<SvProject> {

    List<SvProject> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    /** 统计用户的项目总数（战略规划用） */
    long countByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    /** 每日拍摄：按日期、人设筛选 */
    List<SvProject> findByOwnerIdAndProjectTypeAndDeletedOrderByScheduleDateDescCreateTimeDesc(
            Long ownerId, String projectType, Integer deleted);

    /** 每日拍摄：统计某人设某日已有脚本数 */
    long countByOwnerIdAndPersonaIdAndProjectTypeAndScheduleDateAndDeleted(
            Long ownerId, Long personaId, String projectType, Date scheduleDate, Integer deleted);

    /** 按 shotListId 查找项目（用于分镜审核） */
    java.util.Optional<SvProject> findByShotListIdAndDeleted(Long shotListId, Integer deleted);

    /** 内容日历：计划拍摄日期在范围内的项目 */
    @Query("SELECT p FROM SvProject p WHERE p.ownerId IN :ownerIds AND p.deleted = 0 " +
            "AND p.scheduleDate BETWEEN :startDate AND :endDate ORDER BY p.scheduleDate, p.createTime")
    List<SvProject> findByOwnerIdInAndScheduleDateBetweenAndDeleted(
            @Param("ownerIds") List<Long> ownerIds, @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    /** 内容日历：发布时间在范围内的项目 */
    @Query("SELECT p FROM SvProject p WHERE p.ownerId IN :ownerIds AND p.deleted = 0 " +
            "AND p.publishTime IS NOT NULL AND p.publishTime BETWEEN :startTs AND :endTs ORDER BY p.publishTime")
    List<SvProject> findByOwnerIdInAndPublishTimeBetweenAndDeleted(
            @Param("ownerIds") List<Long> ownerIds, @Param("startTs") Timestamp startTs,
            @Param("endTs") Timestamp endTs);

    @Query("SELECT p FROM SvProject p WHERE p.deleted = 0 AND p.scheduleDate BETWEEN :startDate AND :endDate ORDER BY p.scheduleDate")
    List<SvProject> findByScheduleDateBetweenAndDeleted(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT p FROM SvProject p WHERE p.deleted = 0 AND p.publishTime IS NOT NULL AND p.publishTime BETWEEN :startTs AND :endTs ORDER BY p.publishTime")
    List<SvProject> findByPublishTimeBetweenAndDeleted(@Param("startTs") Timestamp startTs, @Param("endTs") Timestamp endTs);
}
