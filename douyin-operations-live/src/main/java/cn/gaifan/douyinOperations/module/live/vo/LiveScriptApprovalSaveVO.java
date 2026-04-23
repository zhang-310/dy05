package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LiveScriptApprovalSaveVO {

    private Long id;

    @NotNull(message = "话术 ID 不能为空")
    @Positive
    private Long scriptId;

    /** 操作: submit / approve / reject / revoke */
    private String action;

    /** 审核意见 */
    private String comments;
}
