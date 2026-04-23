package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * P1-2: 话术智能推荐结果 VO
 */
@Data
@Builder
public class ScriptRecommendVO {
    /** 推荐话术 ID（来自 live_script 或 script_library） */
    private Long scriptId;
    /** 来源类型：live_script / script_library / template */
    private String sourceType;
    /** 话术类型 */
    private String scriptType;
    /** 推荐理由 */
    private String reason;
    /** 话术内容预览 */
    private String contentPreview;
    /** 历史效果分（0-100） */
    private BigDecimal effectivenessScore;
    /** 历史使用次数 */
    private Integer useCount;
    /** 推荐分（综合排序用） */
    private double recommendScore;
}
