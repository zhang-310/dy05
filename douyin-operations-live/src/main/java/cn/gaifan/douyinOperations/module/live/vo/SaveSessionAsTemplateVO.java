package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 从场次话术保存为模板的请求 VO
 */
@Data
public class SaveSessionAsTemplateVO {

    @NotNull(message = "场次 ID 不能为空")
    @Positive(message = "场次 ID 必须为正数")
    private Long sessionId;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称不能超过 128 个字符")
    private String templateName;

    /** 要包含的话术类型列表（如 opening, product, transition, closing），为空则包含全部 */
    private List<String> scriptTypes;
}
