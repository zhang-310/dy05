package cn.gaifan.douyinOperations.module.product.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 商品话术「多类型 × 多风格 × 多场景」组合生成请求（SSE 进度）。
 * <ul>
 *   <li>scriptTypes 为空：默认三种主类型全开（种草/促销/正式）</li>
 *   <li>scenes 为空或 null：仅按「通用」场景生成一条（scene=null）</li>
 *   <li>总任务数 = productIds × scriptTypes × styles × scenes，受服务端上限约束</li>
 * </ul>
 */
@Data
public class ComboGenerateRequestVO {

    @NotEmpty(message = "产品 ID 列表不能为空")
    @Size(max = 100, message = "单次最多 100 个产品")
    private List<Long> productIds;

    /**
     * 话术类型：seed / promotion / formal；为空则三种全开
     */
    @Size(max = 10, message = "类型数量异常")
    private List<String> scriptTypes;

    @NotEmpty(message = "至少选择一个风格")
    @Size(max = 10, message = "最多 10 个风格")
    private List<String> styles;

    /**
     * 应用场景列表；空或 null 表示仅生成「通用」一条（scene 空）。
     * 多选时与类型、风格做笛卡尔积。
     */
    @Size(max = 8, message = "最多 8 个场景组合")
    private List<String> scenes;

    private Long personaId;
    private Integer duration;

    /** 是否使用话术知识库参考（RAG） */
    private Boolean useKbRef;
}
