package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * AI 直播复盘报告 VO（来自 ai_live_review 表）
 */
@Data
public class LiveReviewVO {
    private Long id;
    private Long sessionId;
    private Long totalViewers;
    private BigDecimal totalGmv;
    private BigDecimal conversionRate;
    private Long peakViewers;
    private String reportContent;
    private String topScripts;
    private String weakPoints;
    private Integer status;
    private Timestamp createTime;
}
