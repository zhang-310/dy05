package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LiveCompetitiveInsightSaveVO {

    private Long id;

    private Long sessionId;

    @NotBlank(message = "竞品标识不能为空")
    private String competitorLabel;

    private BigDecimal productPrice;
    private BigDecimal marketSharePercent;
    private BigDecimal gmvEstimate;
    private String winLossNotes;
}
