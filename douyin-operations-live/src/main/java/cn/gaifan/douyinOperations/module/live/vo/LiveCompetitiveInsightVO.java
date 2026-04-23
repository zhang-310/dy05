package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class LiveCompetitiveInsightVO {

    private Long id;
    private Long ownerId;
    private Long sessionId;
    private String competitorLabel;
    private BigDecimal productPrice;
    private BigDecimal marketSharePercent;
    private BigDecimal gmvEstimate;
    private String winLossNotes;
    private Timestamp createTime;
    private Timestamp updateTime;
}
