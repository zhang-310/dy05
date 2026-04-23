package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveSessionRealtimeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 直播实时数据 Repository
 * 提供直播实时数据的持久化访问能力
 */
@Repository
public interface LiveSessionRealtimeDataRepository extends JpaRepository<LiveSessionRealtimeData, Long> {

    /**
     * 按直播场次 ID 查询实时数据
     *
     * @param liveSessionId 直播场次 ID
     * @return 实时数据，如果不存在则返回 Optional.empty()
     */
    Optional<LiveSessionRealtimeData> findByLiveSessionId(Long liveSessionId);

    /**
     * 批量更新实时数据
     *
     * @param liveSessionId        直播场次 ID
     * @param watchedCount         观看人数
     * @param viewerCount          在线人数
     * @param likeCount            点赞数
     * @param commentCount         评论数
     * @param shareCount           分享数
     * @param followCount          关注数
     * @param giftAmount           礼物金额
     * @param productClickCount    产品点击数
     * @param productPurchaseCount 商品购买数
     * @param productPurchaseAmount 商品购买金额
     * @param currentSlotIndex     当前话术段落序号
     * @return 更新行数
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE live_session_realtime_data SET
            watched_count = :watchedCount,
            viewer_count = :viewerCount,
            like_count = :likeCount,
            comment_count = :commentCount,
            share_count = :shareCount,
            follow_count = :followCount,
            gift_amount = :giftAmount,
            product_click_count = :productClickCount,
            product_purchase_count = :productPurchaseCount,
            product_purchase_amount = :productPurchaseAmount,
            current_slot_index = :currentSlotIndex,
            updated_at = CURRENT_TIMESTAMP
        WHERE live_session_id = :liveSessionId AND deleted = 0
        """, nativeQuery = true)
    int updateRealtimeData(
            @Param("liveSessionId") Long liveSessionId,
            @Param("watchedCount") Integer watchedCount,
            @Param("viewerCount") Integer viewerCount,
            @Param("likeCount") Integer likeCount,
            @Param("commentCount") Integer commentCount,
            @Param("shareCount") Integer shareCount,
            @Param("followCount") Integer followCount,
            @Param("giftAmount") BigDecimal giftAmount,
            @Param("productClickCount") Integer productClickCount,
            @Param("productPurchaseCount") Integer productPurchaseCount,
            @Param("productPurchaseAmount") BigDecimal productPurchaseAmount,
            @Param("currentSlotIndex") Integer currentSlotIndex
    );
}
