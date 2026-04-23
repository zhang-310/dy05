package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 从场次话术创建自定义模板的请求 VO
 * 用于 /api/v1/live/script-template/save-from-session
 */
@Data
public class SaveFromSessionVO {

    @NotNull(message = "场次 ID 不能为空")
    @Positive(message = "场次 ID 必须为正数")
    private Long sessionId;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称不能超过 128 个字符")
    private String templateName;

    @Size(max = 500, message = "描述不能超过 500 个字符")
    private String description;
}
