package cn.gaifan.douyinOperations.module.product.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 产品话术批量 AI 生成请求
 */
@Data
public class BatchGenerateRequestVO {

    @NotEmpty(message = "产品 ID 列表不能为空")
    @Size(max = 100, message = "单次最多 100 个产品")
    private List<Long> productIds;

    @NotNull(message = "话术类型不能为空")
    private String scriptType;

    @NotEmpty(message = "至少选择一个风格")
    @Size(max = 10, message = "最多 10 个风格")
    private List<String> styles;

    private Long personaId;
    private Integer duration;

    /** 是否使用话术知识库参考（RAG） */
    private Boolean useKbRef;

    /** 应用场景：short_video(短视频)/guopin(过品)/cangbo(仓播)/danpin(单品)/yubo(娱播) */
    private String scene;
}
