package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ScriptSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
    @NotBlank(message = "话术标题不能为空")
    private String title;
    private String content;
    private String category;
    /** 来源：manual/live/ai */
    private String source;
    /** 来源ID（如直播场次ID） */
    private Long sourceId;
    private String tags;
    private Integer status;
}
