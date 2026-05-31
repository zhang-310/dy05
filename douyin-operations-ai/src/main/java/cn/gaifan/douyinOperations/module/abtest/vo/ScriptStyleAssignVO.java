package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 话术风格 A/B 分配结果：随机分配到的变体及风格编码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptStyleAssignVO {
    private Long experimentId;
    private Long variantId;
    private String styleCode;
    private String variantType; // A / B
}
