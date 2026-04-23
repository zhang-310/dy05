package cn.gaifan.douyinOperations.module.shortvideo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 拍摄任务保存
 */
@Data
public class SvShootingTaskSaveVO {

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 256)
    private String title;

    @NotBlank(message = "计划拍摄日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "shootDate 须为 yyyy-MM-dd")
    private String shootDate;

    private Long anchorUserId;
    private Long personaId;
    private Long photographerId;
    private Long scriptId;
    private Long projectId;
    private String description;
    private String scriptContent;
    private String shootingBrief;
    private Integer priority;
    /** 0-5 */
    private Integer status;
    private String materialUrls;
    private String reviewNotes;
    private Long reviewedBy;
    private String referenceVideoUrl;
    private Long referenceVideoTaskId;
    private String referenceGeneratedAt;
}
