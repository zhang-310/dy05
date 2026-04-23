package cn.gaifan.douyinOperations.module.auth.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 封禁/解封用户请求体
 */
@Data
public class AuthUserBanVO {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "ban 标志不能为空")
    private Boolean ban;

    private String reason;
}
