package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 直播实时数据值对象
 * 用于返回实时数据给前端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionRealtimeDataVO {

    /**
     * 实时数据 ID
     */
    private Long id;

    /**
     * 直播场次 ID
     */
    private Long liveSessionId;

    /**
     * 累计观看人数
     */
    private Integer watchedCount;

    /**
     * 在线观众数
     */
    private Integer viewerCount;

    /**
     * 点赞总数
     */
    private Integer likeCount;

    /**
     * 评论总数
     */
    private Integer commentCount;

    /**
     * 分享总数
     */
    private Integer shareCount;

    /**
     * 新增关注数
     */
    private Integer followCount;

    /**
     * 礼物金额总额（元）
     */
    private BigDecimal giftAmount;

    /**
     * 产品点击数
     */
    private Integer productClickCount;

    /**
     * 商品购买数
     */
    private Integer productPurchaseCount;

    /**
     * 商品购买总金额（元）
     */
    private BigDecimal productPurchaseAmount;

    /**
     * 当前话术段落序号
     */
    private Integer currentSlotIndex;
}
