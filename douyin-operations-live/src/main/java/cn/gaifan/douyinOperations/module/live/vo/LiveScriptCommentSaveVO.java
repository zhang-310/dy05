package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 话术行内评论保存 VO
 */
@Data
public class LiveScriptCommentSaveVO {

    @NotNull(message = "话术 ID 不能为空")
    @Positive(message = "话术 ID 必须为正数")
    private Long scriptId;

    @NotNull(message = "场次 ID 不能为空")
    @Positive(message = "场次 ID 必须为正数")
    private Long sessionId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 5000, message = "评论内容不能超过 5000 个字符")
    private String content;

    /** 父评论 ID（回复时传入，顶级评论不传） */
    @Positive(message = "父评论 ID 必须为正数")
    private Long parentId;
}
