package cn.gaifan.douyinOperations.module.shortvideo.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SvScriptSaveVO {

    private Long id;

    @NotBlank(message = "脚本标题不能为空")
    private String title;

    @NotBlank(message = "脚本内容不能为空")
    private String content;

    @NotBlank(message = "脚本类型不能为空")
    private String scriptType;  // viral_clone/daily/soft_ad

    private String generationType;
    private Long referenceViralId;
    private String theme;
    private String style;
    private Integer duration;
    private Integer wordCount;
    private String tags;
    private String aiPrompt;
    private String aiModel;

    /** 关联人设 ID */
    private Long personaId;
}
