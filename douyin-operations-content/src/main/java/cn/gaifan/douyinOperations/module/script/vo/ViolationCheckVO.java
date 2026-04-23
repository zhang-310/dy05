package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ViolationCheckVO {
    @NotBlank(message = "检测文本不能为空")
    private String text;
    /** 检测范围：all=全部 live=直播 video=短视频，默认 all */
    private String scope = "all";
}
