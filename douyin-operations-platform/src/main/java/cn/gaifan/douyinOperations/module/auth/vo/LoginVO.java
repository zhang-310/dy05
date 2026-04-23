package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 登录请求 VO（支持账号密码、手机验证码、邮箱验证码）
 * loginType=password 或不传：username+password 必填；loginType=sms：target+code（手机号+验证码）；loginType=email：target+code（邮箱+验证码）
 */
@Data
public class LoginVO {

    /** 登录方式：password / sms / email，默认 password */
    @Size(max = 16)
    private String loginType;

    /** 账号密码方式：用户名 */
    @Size(max = 64)
    private String username;

    /** 账号密码方式：密码 */
    @Size(max = 128)
    private String password;

    /** 验证码方式：手机号或邮箱 */
    @Size(max = 128)
    private String target;

    /** 验证码方式：验证码 */
    @Size(max = 16)
    private String code;

    /** 图片验证码：首次密码失败后必填；captchaId 从 GET /auth/captcha 获取 */
    @Size(max = 64)
    private String captchaId;
    /** 图片验证码：用户输入的验证码 */
    @Size(max = 16)
    private String captchaCode;
}
