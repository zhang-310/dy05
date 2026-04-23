package cn.gaifan.douyinOperations.module.shortvideo.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 二创模板保存 VO（Phase 5）
 */
@Data
public class RemakeTemplateSaveVO {

    private Long id;
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    @NotBlank(message = "二创类型不能为空")
    private String remakeType;
    private Long sourceViralId;
    private Object structureTemplate;
    private String emotionCurve;
    private String bgmStyle;
    private String durationRange;
    private String adaptationGuide;
    private List<Object> variableSlots;
}
