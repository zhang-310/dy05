package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 直播实时数据保存参数
 * 用于保存或更新实时数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeDataSaveVO {

    /**
     * 直播场次 ID
     */
    @NotNull(message = "直播场次 ID 不能为空")
    private Long liveSessionId;

    /**
     * 观看人数
     */
    private Integer watchedCount;

    /**
     * 在线人数
     */
    private Integer viewerCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 分享数
     */
    private Integer shareCount;

    /**
     * 关注数
     */
    private Integer followCount;

    /**
     * 礼物金额（元）
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
     * 商品购买金额（元）
     */
    private BigDecimal productPurchaseAmount;
}
