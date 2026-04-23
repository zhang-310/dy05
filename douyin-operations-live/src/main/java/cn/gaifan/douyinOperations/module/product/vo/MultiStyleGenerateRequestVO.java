package cn.gaifan.douyinOperations.module.product.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 产品话术多风格 AI 生成请求
 */
@Data
public class MultiStyleGenerateRequestVO {

    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    /** 话术类型：seed(种草) / promotion(促销) / formal(正式) */
    @NotNull(message = "话术类型不能为空")
    private String scriptType;

    /** 风格列表，如 professional, friendly, passionate */
    @NotEmpty(message = "至少选择一个风格")
    private List<String> styles;

    /** 人设 ID（可选） */
    private Long personaId;

    /** 时长（秒），用于控制话术长度 */
    private Integer duration;

    /** 是否使用话术知识库参考（RAG），默认 true */
    private Boolean useKbRef;

    /** 应用场景：short_video(短视频)/guopin(过品)/cangbo(仓播)/danpin(单品)/yubo(娱播) */
    private String scene;

    /** 话术知识库参考分类（可多选） */
    private List<String> kbCategories;

    /** 风格融合模式：多风格融合为一条话术，默认 false */
    private Boolean fusionMode;

    /** 风格权重配置（融合模式下生效），key=风格代码，value=权重（0-1），总和应为1.0 */
    private Map<String, Double> styleWeights;

    /** 融合策略：blended(混合)/sequential(顺序)/layered(分层)/alternating(交替)/progressive(渐进) */
    private String fusionStrategy;
}
