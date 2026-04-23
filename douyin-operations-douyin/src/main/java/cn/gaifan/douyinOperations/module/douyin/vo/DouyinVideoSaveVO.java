package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 抖音视频保存 VO
 */
@Data
public class DouyinVideoSaveVO {

    private Long id;

    @NotNull(message = "账号 ID 不能为空")
    private Long accountId;

    @NotBlank(message = "视频 ID 不能为空")
    private String videoId;

    @NotBlank(message = "视频标题不能为空")
    private String title;

    private String description;

    private Long viewCount;

    private Long likeCount;

    private Long shareCount;

    private Long commentCount;

    private Long downloadCount;

    private String videoType;
}
