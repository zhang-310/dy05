package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 忘记密码（验证码+新密码重置）
 */
@Data
public class ForgotPasswordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号或邮箱（与发验证码时一致） */
    @NotBlank(message = "手机号或邮箱不能为空")
    @Size(max = 128)
    private String target;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    @Size(max = 16)
    private String code;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度 6～128")
    private String newPassword;
}
