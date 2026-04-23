package cn.gaifan.douyinOperations.module.product.vo;

import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品话术多风格 AI 生成结果
 */
@Data
public class MultiStyleGenerateResultVO {

    private Long productId;
    private String productName;
    private String scriptType;
    private List<StyleResult> results = new ArrayList<>();

    /** A/B 实验 ID（如果使用了实验分配）*/
    private Long abExperimentId;

    /** A/B 实验变体 ID（如果使用了实验分配）*/
    private Long abVariantId;

    @Data
    public static class StyleResult {
        private String style;
        private boolean success;
        private DyProductScript script;
        private String errorMessage;
    }
}
