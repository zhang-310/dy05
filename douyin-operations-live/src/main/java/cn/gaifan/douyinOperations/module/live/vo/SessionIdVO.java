package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class SessionIdVO {

    @NotNull(message = "场次 ID 不能为空")
    @Positive(message = "场次 ID 必须为正数")
    private Long sessionId;

    /** 指定使用的 AI 模型 ID（可选） */
    @Positive(message = "模型 ID 必须为正数")
    private Long modelId;
}
