package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 直播话术版本保存/更新 VO
 */
@Data
public class LiveScriptVersionSaveVO {

    @NotNull(message = "话术ID不能为空")
    private Long scriptId;

    private Integer versionNo;
    private String versionLabel;

    @NotBlank(message = "话术内容不能为空")
    private String scriptContent;

    private String scriptType;
    private String remark;
    private String versionStatus = "draft";
    private Double effectivenessScore;
    private Long basedOnVersionId;
    private String recommendReason;
    private Integer isRecommended;
}
