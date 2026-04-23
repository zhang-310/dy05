package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveSessionScriptSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 直播话术段落 Repository
 * 提供话术段落的数据访问能力，支持查询、更新、导航等操作
 */
@Repository
public interface LiveSessionScriptSlotRepository
        extends JpaRepository<LiveSessionScriptSlot, Long>, JpaSpecificationExecutor<LiveSessionScriptSlot> {

    /**
     * 按直播场次 ID 和排序顺序查询所有话术段落
     *
     * @param liveSessionId 直播场次 ID
     * @param pageable      分页信息
     * @return 话术段落分页结果
     */
    Page<LiveSessionScriptSlot> findByLiveSessionIdOrderBySlotIndex(Long liveSessionId, Pageable pageable);

    /**
     * 按直播场次 ID 查询所有话术段落（不分页）
     *
     * @param liveSessionId 直播场次 ID
     * @return 话术段落列表
     */
    List<LiveSessionScriptSlot> findByLiveSessionIdOrderBySlotIndex(Long liveSessionId);

    /**
     * 查询当前话术段落（isCurrent = true）
     *
     * @param liveSessionId 直播场次 ID
     * @return 当前话术段落，如果不存在则返回 Optional.empty()
     */
    @Query("SELECT s FROM LiveSessionScriptSlot s WHERE s.liveSessionId = :liveSessionId AND s.isCurrent = true")
    Optional<LiveSessionScriptSlot> findCurrentSlot(@Param("liveSessionId") Long liveSessionId);

    /**
     * 按直播场次和段落序号查询
     *
     * @param liveSessionId 直播场次 ID
     * @param slotIndex     段落序号
     * @return 话术段落，如果不存在则返回 Optional.empty()
     */
    Optional<LiveSessionScriptSlot> findByLiveSessionIdAndSlotIndex(Long liveSessionId, Integer slotIndex);

    /**
     * 查询已完成的话术段落列表
     *
     * @param liveSessionId 直播场次 ID
     * @return 已完成话术段落列表
     */
    @Query("SELECT s FROM LiveSessionScriptSlot s WHERE s.liveSessionId = :liveSessionId AND s.isCompleted = true ORDER BY s.slotIndex")
    List<LiveSessionScriptSlot> findCompletedSlots(@Param("liveSessionId") Long liveSessionId);

    /**
     * 查询未完成的话术段落列表
     *
     * @param liveSessionId 直播场次 ID
     * @return 未完成话术段落列表
     */
    @Query("SELECT s FROM LiveSessionScriptSlot s WHERE s.liveSessionId = :liveSessionId AND s.isCompleted = false ORDER BY s.slotIndex")
    List<LiveSessionScriptSlot> findUncompletedSlots(@Param("liveSessionId") Long liveSessionId);

    /**
     * 统计直播的话术段落总数
     *
     * @param liveSessionId 直播场次 ID
     * @return 话术段落总数
     */
    long countByLiveSessionId(Long liveSessionId);
}
