package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 风格预览结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StylePreviewVO {
    /** 风格代码 */
    private String style;

    /** 风格名称 */
    private String styleName;

    /** 预览内容（15-20秒片段） */
    private String content;
}
