package cn.gaifan.douyinOperations.module.live.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 直播话术版本搜索 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LiveScriptVersionSearchVO extends BasicQueryDto {

    private Long scriptId;
    private Long sessionId;
    private String versionStatus;
    private Double effectivenessScoreMin;
    private Double effectivenessScoreMax;
    private Integer isRecommended;
    private Long ownerId;
    private String scriptType;
}
