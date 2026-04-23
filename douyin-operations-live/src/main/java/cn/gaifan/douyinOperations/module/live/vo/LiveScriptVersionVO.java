package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播话术版本返回 VO
 */
@Data
public class LiveScriptVersionVO {

    private Long id;
    private Long scriptId;
    private Long sessionId;
    private Integer versionNo;
    private String versionLabel;
    private String scriptContent;
    private String scriptType;
    private String remark;
    private String versionStatus;
    private Double effectivenessScore;
    private Integer likedCount;
    private Integer usageCount;
    private Timestamp lastUsedTime;
    private Long ownerId;
    private Integer isRecommended;
    private String recommendReason;
    private Double recommendScore;
    private Long basedOnVersionId;
    private String changeSummary;
    private Timestamp createTime;
    private Timestamp updateTime;
}
