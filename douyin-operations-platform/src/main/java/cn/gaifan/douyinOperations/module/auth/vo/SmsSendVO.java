package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 发送验证码请求（短信/邮箱）
 */
@Data
public class SmsSendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号或邮箱 */
    @NotBlank(message = "手机号或邮箱不能为空")
    @Size(max = 128)
    private String target;

    /** 类型：login / forgot_password */
    @NotBlank(message = "类型不能为空")
    @Size(max = 32)
    private String type;
}
