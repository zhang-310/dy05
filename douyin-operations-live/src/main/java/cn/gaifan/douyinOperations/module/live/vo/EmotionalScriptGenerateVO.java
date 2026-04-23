package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 情绪价值话术生成请求
 */
@Data
public class EmotionalScriptGenerateVO {

    @NotNull(message = "场次 ID 不能为空")
    private Long sessionId;

    /** 话术类型：quote/proverb/emotional_healing/female_perspective */
    @NotBlank(message = "话术类型不能为空")
    private String category;

    /** 子类别：如 female_perspective 下的 family_topic/relationship_topic，或具体话题如 mother_in_law */
    private String subCategory;
}
