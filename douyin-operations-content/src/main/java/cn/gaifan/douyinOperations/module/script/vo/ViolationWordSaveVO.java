package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ViolationWordSaveVO {
    private Long id;
    @NotBlank(message = "违规词不能为空")
    private String word;
    @NotNull(message = "严重程度不能为空")
    private Integer level;
    private String reason;
    private String replacement;
    /** 适用范围：all / live_only / video_only，默认 all */
    private String scope;
    private Integer status;
}
