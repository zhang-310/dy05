package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询话术审核历史请求 VO
 * P1-9: 替换 Map 参数，添加校验
 */
@Data
public class ApprovalHistoryQueryVO {

    @NotNull(message = "scriptId 不能为空")
    private Long scriptId;
}
