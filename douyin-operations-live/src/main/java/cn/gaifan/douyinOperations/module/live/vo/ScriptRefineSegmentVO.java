package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class ScriptRefineSegmentVO {

    @NotNull(message = "话术 ID 不能为空")
    @Positive(message = "话术 ID 必须为正数")
    private Long scriptId;

    @NotBlank(message = "待修改片段不能为空")
    @Size(max = 5000, message = "片段不能超过 5000 个字符")
    private String segmentText;

    @NotBlank(message = "修改要求不能为空")
    @Size(max = 500, message = "修改要求不能超过 500 个字符")
    private String instruction;

    /** 指定使用的 AI 模型 ID（可选） */
    @Positive(message = "模型 ID 必须为正数")
    private Long modelId;
}
