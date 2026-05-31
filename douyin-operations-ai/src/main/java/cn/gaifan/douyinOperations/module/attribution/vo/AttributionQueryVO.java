package cn.gaifan.douyinOperations.module.attribution.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 归因查询请求参数
 * P1-2: 替代 Map 参数，提供类型安全和校验
 */
@Data
public class AttributionQueryVO {

    @NotNull(message = "场次ID不能为空")
    private Long sessionId;
}
