package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批话术请求 VO
 * P1-9: 替换 Map 参数，添加校验
 */
@Data
public class ReviewApprovalVO {

    @NotNull(message = "approvalId 不能为空")
    private Long approvalId;

    @NotBlank(message = "action 不能为空")
    private String action;  // "approve" 或 "reject"

    private String comments;
}
