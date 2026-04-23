package cn.gaifan.douyinOperations.module.shortvideo.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 短视频项目保存 VO
 */
@Data
public class SvProjectSaveVO {

    private Long id;
    private Long accountId;

    @NotBlank(message = "项目标题不能为空")
    private String title;

    @NotBlank(message = "项目类型不能为空")
    private String projectType;  // viral_clone/daily/soft_ad

    private Long personaId;      // 人设 ID（daily 类型）
    private String scheduleDate; // 计划拍摄日期 yyyy-MM-dd（daily 类型）
    private String shootStatus;  // not_started/ready/shooting/shot_done（daily 类型）

    private String status;       // draft/processing/completed/failed
    private Long scriptId;
    private Long shotListId;
    private String finalVideoUrl;
    private String thumbnailUrl;
    private String characterReferenceUrl;
    private String sceneReferenceUrl;
    private Integer duration;
    private String publishTitle;
    private String publishPlatforms;
    private String publishTime;
    private String reviewStatus;
    private Long reviewerId;
    private String reviewComment;

    /** 关联商品 ID 列表 */
    private java.util.List<Long> relatedProductIds;
}
