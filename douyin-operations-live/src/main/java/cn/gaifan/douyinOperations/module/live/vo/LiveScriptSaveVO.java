package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 直播话术保存/更新 VO
 */
@Data
public class LiveScriptSaveVO {

    private Long id;

    private Long sessionId;

    private String scriptContent;

    private String scriptType;
    private String style;
    private String requirement;
    private Long productId;
    private Integer durationLimitSec;
    private Integer sequenceNo;
    private Long executionTime;
    private Integer executed = 0;
}
