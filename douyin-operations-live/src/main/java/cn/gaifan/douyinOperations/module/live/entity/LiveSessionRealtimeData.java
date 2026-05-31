package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 直播实时数据实体
 * 存储直播场次的实时统计数据，包括观众、点赞、评论和商品转化数据
 */
@Entity
@Table(name = "live_session_realtime_data", indexes = {
    @Index(name = "idx_live_session_realtime_data_session_id", columnList = "live_session_id")
})
@SQLRestriction("deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionRealtimeData {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 直播场次 ID
     */
    @Column(name = "live_session_id", nullable = false)
    private Long liveSessionId;

    /**
     * 累计观看人数
     */
    @Column(name = "watched_count")
    private Integer watchedCount;

    /**
     * 在线观众数
     */
    @Column(name = "viewer_count")
    private Integer viewerCount;

    /**
     * 点赞总数
     */
    @Column(name = "like_count")
    private Integer likeCount;

    /**
     * 评论总数
     */
    @Column(name = "comment_count")
    private Integer commentCount;

    /**
     * 分享总数
     */
    @Column(name = "share_count")
    private Integer shareCount;

    /**
     * 新增关注数
     */
    @Column(name = "follow_count")
    private Integer followCount;

    /**
     * 礼物金额总额（元）
     */
    @Column(name = "gift_amount", precision = 10, scale = 2)
    private BigDecimal giftAmount;

    /**
     * 产品点击数
     */
    @Column(name = "product_click_count")
    private Integer productClickCount;

    /**
     * 商品购买数
     */
    @Column(name = "product_purchase_count")
    private Integer productPurchaseCount;

    /**
     * 商品购买总金额（元）
     */
    @Column(name = "product_purchase_amount", precision = 10, scale = 2)
    private BigDecimal productPurchaseAmount;

    /**
     * 当前话术段落序号
     */
    @Column(name = "current_slot_index")
    private Integer currentSlotIndex;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    /**
     * 插入前自动设置创建和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        deleted = 0;
        if (watchedCount == null) {
            watchedCount = 0;
        }
        if (viewerCount == null) {
            viewerCount = 0;
        }
        if (likeCount == null) {
            likeCount = 0;
        }
        if (commentCount == null) {
            commentCount = 0;
        }
        if (shareCount == null) {
            shareCount = 0;
        }
        if (followCount == null) {
            followCount = 0;
        }
        if (giftAmount == null) {
            giftAmount = BigDecimal.ZERO;
        }
        if (productClickCount == null) {
            productClickCount = 0;
        }
        if (productPurchaseCount == null) {
            productPurchaseCount = 0;
        }
        if (productPurchaseAmount == null) {
            productPurchaseAmount = BigDecimal.ZERO;
        }
    }

    /**
     * 更新前自动更新修改时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
