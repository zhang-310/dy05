package cn.gaifan.douyinOperations.module.copy.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class CopyApprovalSaveVO {
    private Long id;
    @NotNull(message = "文案 ID 不能为空")
    private Long copyId;
    private Long userId;
    private Integer approvalStatus;
    private String comments;
}
