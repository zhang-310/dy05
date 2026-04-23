package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UserViolationWordSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
    @NotBlank(message = "违规词不能为空")
    private String word;
    private Integer level;
    private String reason;
    private String replacement;
    /** 适用范围：all / live_only / video_only，默认 all */
    private String scope;
    private Integer status;
}
