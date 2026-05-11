package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 抖音视频保存 VO
 */
@Data
public class DouyinVideoSaveVO {

    private Long id;

    @NotNull(message = "账号 ID 不能为空")
    private Long accountId;

    @NotBlank(message = "视频 ID 不能为空")
    @Size(max = 64, message = "视频 ID 不能超过 64 字符")
    private String videoId;

    @NotBlank(message = "视频标题不能为空")
    @Size(max = 256, message = "视频标题不能超过 256 字符")
    private String title;

    @Size(max = 2000, message = "视频描述不能超过 2000 字符")
    private String description;

    private Long viewCount;

    private Long likeCount;

    private Long shareCount;

    private Long commentCount;

    private Long downloadCount;

    @Size(max = 32, message = "视频类型不能超过 32 字符")
    private String videoType;
}
