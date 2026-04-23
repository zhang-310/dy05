package cn.gaifan.douyinOperations.module.ai.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiPromptTemplateSearchVO extends BasicQueryDto {

    /** 模板编码 */
    private String templateCode;

    /** 变体名称 */
    private String variantName;

    /** 是否激活 */
    private Boolean isActive;

    /** 所有者 ID */
    private Long ownerId;
}
