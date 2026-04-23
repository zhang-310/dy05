package cn.gaifan.douyinOperations.module.slangdict.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SdEntrySaveVO {
    private Long id;
    @NotBlank(message = "梗/暗语不能为空")
    private String phrase;
    private String meaning;
    private String category;
    private String usageScene;
    private String example;
    private String source;
    private Integer status;
}
