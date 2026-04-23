package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 个人中心修改密码请求（需校验旧密码）
 */
@Data
public class ChangePasswordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前密码 */
    @NotBlank(message = "当前密码不能为空")
    @Size(max = 128)
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "新密码长度 6～128")
    private String newPassword;
}
