package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class LiveSessionDataSaveVO {
    @NotNull(message = "场次 ID 不能为空")
    private Long sessionId;
    private Integer totalViewers;
    private Integer peakViewers;
    private Long totalLikes;
    private Integer totalComments;
    private Integer totalShares;
    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private Integer avgStayTime;
    private Integer newFollowers;
}
