package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiveCompetitorScriptSaveVO {

    private Long id;

    private Long ownerId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String competitorName;

    private String platform;

    @NotBlank(message = "话术正文不能为空")
    private String scriptContent;

    private String sourceUrl;

    private String tags;

    private String notes;
}
