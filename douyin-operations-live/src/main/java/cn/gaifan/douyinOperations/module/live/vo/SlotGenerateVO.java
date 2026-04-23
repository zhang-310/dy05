package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class SlotGenerateVO {

    @NotNull(message = "话术 ID 不能为空")
    @Positive(message = "话术 ID 必须为正数")
    private Long scriptId;

    /** 需求描述（可选） */
    @Size(max = 2000, message = "需求描述不能超过 2000 个字符")
    private String requirement;

    /** 时长上限（秒，可选） */
    @Positive(message = "时长上限必须为正数")
    private Integer durationLimitSec;

    /** 指定使用的 AI 模型 ID（可选） */
    @Positive(message = "模型 ID 必须为正数")
    private Long modelId;
}
