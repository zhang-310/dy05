package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 产品话术保存 VO
 */
@Data
public class ProductScriptSaveVO {

    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @NotNull(message = "话术类型不能为空")
    private String scriptType;

    @NotBlank(message = "话术内容不能为空")
    private String scriptContent;

    private Long personaId;

    private String style;

    private Integer duration;

    private Integer tokenUsage;

    private Boolean isActive;

    /** 来源：ai=AI生成 manual=人工编写 import=导入 */
    private String source;

    /** 应用场景：short_video(短视频)/guopin(过品)/cangbo(仓播)/danpin(单品)/yubo(娱播) */
    private String scene;
}
