package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * P1-2: 多场景话术智能推荐请求 VO
 */
@Data
public class ScriptRecommendRequestVO {
    /** 场次 ID */
    private Long sessionId;
    /** 当前商品 ID（可选） */
    private Long productId;
    /** 当前槽位索引（0-based）*/
    private Integer currentSlot;
    /** 当前在线人数（辅助推荐策略） */
    private Long viewerCount;
    /** 已开播时长（秒） */
    private Long timeElapsed;
    /** 推荐数量，默认 5 */
    private Integer topN;
}
