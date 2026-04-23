package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class ScriptChatVO {

    @NotNull(message = "话术 ID 不能为空")
    @Positive(message = "话术 ID 必须为正数")
    private Long scriptId;

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息不能超过 2000 个字符")
    private String message;

    /** 指定使用的 AI 模型 ID（可选） */
    @Positive(message = "模型 ID 必须为正数")
    private Long modelId;
}
