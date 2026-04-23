package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 产品 AI 话术生成请求 VO
 */
@Data
public class ProductScriptGenerateVO {

    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    /** 人设 ID（可选） */
    private Long personaId;

    /** 话术类型：seed(种草) / promotion(促销) / formal(正式) */
    @NotNull(message = "话术类型不能为空")
    private String scriptType;

    /** 话术风格：enthusiastic(热情) / professional(专业) / casual(随意) / warm(温暖) */
    private String style;

    /** 额外提示词 */
    private String extraPrompt;

    /** 时长（秒），用于控制话术长度 */
    private Integer duration;

    /** 是否使用话术知识库参考（RAG），默认 true */
    private Boolean useKbRef;

    /** 应用场景：short_video(短视频)/guopin(过品)/cangbo(仓播)/danpin(单品)/yubo(娱播) */
    private String scene;

    /** 话术知识库参考分类（可多选） */
    private List<String> kbCategories;
}
